package com.rmgames.feature.game.rush

import com.rmgames.core.sensors.Vector3
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2

/** Readings averaged into the neutral pose when a run starts (~0.4 s at 50 Hz). */
const val CALIBRATION_SAMPLES = 20

/** Tilt (degrees from neutral) that hops, and the calm zone that re-arms. */
const val HOP_DEG = 15f
const val REARM_DEG = 8f

/** The hop axis must beat the other one by this factor (no diagonal guesses). */
private const val AXIS_DOMINANCE = 1.4f

/** Gravity low-pass per reading: removes hand jolts (~70 ms at 50 Hz). */
private const val SMOOTHING = 0.25f

/** How fast neutral follows the hand while it rests in the calm zone. */
private const val NEUTRAL_FOLLOW = 0.01f

/** Current tilt from neutral in degrees: [lean] + = top edge away, [roll] + = left edge down. */
data class Tilt(val lean: Float = 0f, val roll: Float = 0f)

/**
 * Turns accelerometer readings (m/s², gravity included) into hops (docs/games/06 "Controls").
 *
 * Playtest 2026-11-20 ("very bad"): raw readings include the hand's own movement, holding a
 * tilt repeated hops into traffic, and re-arming needed an exact return to neutral. Now:
 * readings are low-passed to gravity; two angles (lean back/forward, roll left/right) are
 * measured against the pose held at the start; a tilt past [HOP_DEG] on one clear axis is
 * exactly one hop; coming back inside [REARM_DEG] re-arms. Holding never repeats.
 */
class TiltHopDetector {
    private var samples = 0
    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private var baseLean = 0f
    private var baseRoll = 0f
    private var armed = true

    /** Latest tilt from neutral, for the on-screen indicator. */
    var tilt = Tilt()
        private set

    /** True once neutral is known (the first [CALIBRATION_SAMPLES] readings). */
    val calibrated: Boolean get() = samples >= CALIBRATION_SAMPLES

    fun onSample(g: Vector3, @Suppress("UNUSED_PARAMETER") nowMs: Long): Hop? {
        if (g.magnitude < 1f) return null // free fall / no data
        if (samples == 0) { gx = g.x; gy = g.y; gz = g.z } else {
            gx += (g.x - gx) * SMOOTHING
            gy += (g.y - gy) * SMOOTHING
            gz += (g.z - gz) * SMOOTHING
        }
        val norm = kotlin.math.sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(1f)
        val lean = Math.toDegrees(atan2(gz.toDouble(), gy.toDouble())).toFloat()
        val roll = Math.toDegrees(asin((gx / norm).coerceIn(-1f, 1f).toDouble())).toFloat()
        if (samples < CALIBRATION_SAMPLES) {
            samples++
            baseLean += (lean - baseLean) / samples
            baseRoll += (roll - baseRoll) / samples
            return null
        }
        val dLean = lean - baseLean
        val dRoll = roll - baseRoll
        tilt = Tilt(dLean, dRoll)
        val strongest = maxOf(abs(dLean), abs(dRoll))
        if (strongest < REARM_DEG) {
            armed = true
            baseLean += (lean - baseLean) * NEUTRAL_FOLLOW
            baseRoll += (roll - baseRoll) * NEUTRAL_FOLLOW
            return null
        }
        if (!armed || strongest < HOP_DEG) return null
        val hop = when {
            abs(dRoll) >= abs(dLean) * AXIS_DOMINANCE -> if (dRoll > 0) Hop.LEFT else Hop.RIGHT
            abs(dLean) >= abs(dRoll) * AXIS_DOMINANCE -> if (dLean > 0) Hop.FORWARD else Hop.BACK
            else -> return null // diagonal: not clearly one direction
        }
        armed = false
        return hop
    }
}
