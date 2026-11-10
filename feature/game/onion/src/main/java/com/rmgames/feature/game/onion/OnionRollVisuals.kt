package com.rmgames.feature.game.onion

/**
 * Pure presentation math for Onion Roll (docs/games/02-onion-roll.md): HUD label,
 * fall animation curves and the onion's face proportions. Kept free of Compose so
 * it is unit-testable (OnionRollVisualsTest).
 */

/** "62.4s" — locale-independent, so the HUD reads the same everywhere. */
internal fun elapsedLabel(elapsedMs: Long): String {
    val tenths = elapsedMs / 100
    return "${tenths / 10}.${tenths % 10}s"
}

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
