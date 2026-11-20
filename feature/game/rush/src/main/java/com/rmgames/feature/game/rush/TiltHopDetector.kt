package com.rmgames.feature.game.rush

import com.rmgames.core.sensors.Vector3
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2

/** Readings averaged into the neutral pose when a run starts (~0.2 s). */
const val CALIBRATION_SAMPLES = 10

/** Holding a tilt repeats the hop this often. */
const val REPEAT_MS = 350L

/** Tilt (degrees from neutral) that hops, and the calm zone that re-arms. */
private const val HOP_DEG = 18f
private const val REARM_DEG = 9f

/** How fast neutral follows the hand while it rests in the calm zone. */
private const val NEUTRAL_FOLLOW = 0.02f

/**
 * Turns accelerometer readings (gravity, m/s²) into hops (docs/games/06 "Controls"). Two
 * angles are measured against the pose the phone had when the run started: leaning back
 * (top edge away = forward hop, towards you = back hop) and rolling (left edge down = left).
 * One tilt is one hop; holding it repeats every [REPEAT_MS]; coming back to neutral re-arms.
 */
class TiltHopDetector {
    private var samples = 0
    private var baseLean = 0f
    private var baseRoll = 0f
    private var armed = true
    private var lastHop: Hop? = null
    private var lastHopAt = 0L

    fun onSample(g: Vector3, nowMs: Long): Hop? {
        val norm = g.magnitude
        if (norm < 1f) return null // free fall / no data: ignore
        val lean = Math.toDegrees(atan2(g.z.toDouble(), g.y.toDouble())).toFloat()
        val roll = Math.toDegrees(asin((g.x / norm).coerceIn(-1f, 1f).toDouble())).toFloat()
        if (samples < CALIBRATION_SAMPLES) {
            samples++
            baseLean += (lean - baseLean) / samples
            baseRoll += (roll - baseRoll) / samples
            return null
        }
        val dLean = lean - baseLean
        val dRoll = roll - baseRoll
        val strongest = maxOf(abs(dLean), abs(dRoll))
        if (strongest < HOP_DEG) {
            if (strongest < REARM_DEG) {
                armed = true
                lastHop = null
                baseLean += (lean - baseLean) * NEUTRAL_FOLLOW
                baseRoll += (roll - baseRoll) * NEUTRAL_FOLLOW
            }
            return null
        }
        val hop = if (abs(dRoll) > abs(dLean)) {
            if (dRoll > 0) Hop.LEFT else Hop.RIGHT
        } else {
            if (dLean > 0) Hop.FORWARD else Hop.BACK
        }
        val fire = armed || (hop == lastHop && nowMs - lastHopAt >= REPEAT_MS)
        if (!fire) return null
        armed = false
        lastHop = hop
        lastHopAt = nowMs
        return hop
    }
}
