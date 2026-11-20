package com.rmgames.core.common.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** M3 shape scale: extra-small 6 · small 10 · medium 14 · large 18 · extra-large 24 (full = pill). */
val RmShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Spacing roles and the "hard ledge" geometry of Direction A.
 * Spacing: related < section < major; [gutter] is the screen edge; [targetMin] the touch target.
 * Depth: buttons/cards get a [strokeWidth] dark outline and a solid [ledge] offset that drops to
 * [ledgePressed] while the control moves down by [pressShift].
 */
@Immutable
data class RmDimens(
    val gapRelated: Dp = 8.dp,
    val gapSection: Dp = 24.dp,
    val gapMajor: Dp = 32.dp,
    val gutter: Dp = 20.dp,
    val targetMin: Dp = 48.dp,
    val strokeWidth: Dp = 2.dp,
    val ledge: Dp = 4.dp,
    val ledgePressed: Dp = 1.dp,
    val pressShift: Dp = 3.dp,
)

/** Motion tokens (ms + M3 easings). Count-up drives the result ScoreCounter. */
@Immutable
data class RmMotion(
    val durationFast: Int = 100,
    val durationBase: Int = 250,
    val durationSlow: Int = 450,
    val durationCountUp: Int = 900,
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val emphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
    /** Slight overshoot for score pops and press release. */
    val spring: Easing = CubicBezierEasing(0.34f, 1.5f, 0.5f, 1f),
)
