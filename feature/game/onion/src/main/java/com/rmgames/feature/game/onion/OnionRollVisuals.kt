package com.rmgames.feature.game.onion

import kotlin.math.PI
import kotlin.math.sin

/**
 * Pure presentation math for Onion Roll (docs/games/02-onion-roll.md): fall
 * animation curves, the onion's face proportions, the loss banner and the danger glow. Kept free of Compose so
 * it is unit-testable (OnionRollVisualsTest).
 */

/** Share of the full drop distance at fall progress [t] (0..1): quadratic, like gravity. */
internal fun fallDropFraction(t: Float): Float {
    val p = t.coerceIn(0f, 1f)
    return p * p
}

/** Tumble angle (degrees) at fall progress [t]: eases in along with the drop. */
internal fun fallTiltDeg(t: Float): Float = FALL_TILT_MAX_DEG * fallDropFraction(t)

/**
 * Face offsets relative to the onion centre, all proportional to [radius].
 * Y grows downward (screen coordinates), so eyes have negative Y.
 */
internal data class FaceLayout(
    val leftEyeX: Float,
    val rightEyeX: Float,
    val leftEyeY: Float,
    val rightEyeY: Float,
    val eyeRadius: Float,
    val smileCx: Float,
    val smileCy: Float,
    val smileRadius: Float,
)

internal fun faceLayout(radius: Float): FaceLayout {
    val eyeX = radius * 0.35f
    val eyeY = -radius * 0.2f
    return FaceLayout(
        leftEyeX = -eyeX,
        rightEyeX = eyeX,
        leftEyeY = eyeY,
        rightEyeY = eyeY,
        eyeRadius = radius * 0.12f,
        smileCx = 0f,
        smileCy = radius * 0.1f,
        smileRadius = radius * 0.4f,
    )
}

private const val FALL_TILT_MAX_DEG = 240f

/** "YOU LOST!" banner opacity [fallElapsedMs] after the fall started: quick fade-in, then solid. */
internal fun lossBannerAlpha(fallElapsedMs: Long): Float =
    (fallElapsedMs / BANNER_FADE_MS).coerceIn(0f, 1f)

/** Banner pop: grows from 60 % to a slight overshoot, then settles at full size. */
internal fun lossBannerScale(fallElapsedMs: Long): Float {
    val t = fallElapsedMs.toFloat()
    return when {
        t < BANNER_POP_MS -> BANNER_START_SCALE + (BANNER_PEAK_SCALE - BANNER_START_SCALE) * (t / BANNER_POP_MS)
        t < BANNER_POP_MS + BANNER_SETTLE_MS ->
            BANNER_PEAK_SCALE - (BANNER_PEAK_SCALE - 1f) * ((t - BANNER_POP_MS) / BANNER_SETTLE_MS)
        else -> 1f
    }
}

private const val BANNER_FADE_MS = 250f
private const val BANNER_POP_MS = 200f
private const val BANNER_SETTLE_MS = 250f
private const val BANNER_START_SCALE = 0.6f
private const val BANNER_PEAK_SCALE = 1.12f

/** How close the onion has been shoved to the end roller: 0 on its home row, 1 at the roller. */
internal fun dangerLevel(onionY: Float): Float =
    ((onionY - ONION_Y) / (END_OF_BELT_Y - ONION_Y)).coerceIn(0f, 1f)

/**
 * Opacity of the red danger glow at the bottom: always faintly on, brighter and
 * faster-pulsing as [danger] (see [dangerLevel]) grows.
 */
internal fun dangerGlowAlpha(elapsedMs: Long, danger: Float): Float {
    val d = danger.coerceIn(0f, 1f)
    val peak = GLOW_BASE_ALPHA + (GLOW_MAX_ALPHA - GLOW_BASE_ALPHA) * d
    val hz = GLOW_BASE_HZ + GLOW_EXTRA_HZ * d
    val wave = sin(2.0 * PI * hz * elapsedMs / 1000.0).toFloat() // -1..1
    return peak * (1f - GLOW_PULSE_DEPTH * (1f - wave) / 2f)
}

private const val GLOW_BASE_ALPHA = 0.22f
private const val GLOW_MAX_ALPHA = 0.7f
private const val GLOW_PULSE_DEPTH = 0.25f // dips to 75 % of the peak
private const val GLOW_BASE_HZ = 0.8
private const val GLOW_EXTRA_HZ = 2.4
