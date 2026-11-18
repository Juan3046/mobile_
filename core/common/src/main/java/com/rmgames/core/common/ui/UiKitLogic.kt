package com.rmgames.core.common.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import com.rmgames.core.common.theme.RmDimens
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.time.Duration

// Pure logic behind the UI kit — kept out of composables so it is JVM-testable.

/** Where the content sits and how much of the hard ledge shows below it. */
data class LedgeGeometry(val contentOffset: Dp, val ledgeHeight: Dp)

/** Resting: full ledge. Pressed: content sinks [RmDimens.pressShift]; footprint stays [RmDimens.ledge]. */
fun ledgeGeometry(pressed: Boolean, dimens: RmDimens): LedgeGeometry =
    if (pressed) LedgeGeometry(dimens.pressShift, dimens.ledgePressed)
    else LedgeGeometry(dimens.ledge * 0, dimens.ledge)

/** `HH:MM:SS`, rounding partial seconds up (never shows 00:00:00 while time remains). */
fun formatCountdown(remaining: Duration): String {
    val total = ceil(remaining.inWholeMilliseconds.coerceAtLeast(0) / 1000.0).toLong()
    return "%02d:%02d:%02d".format(Locale.US, total / 3600, (total % 3600) / 60, total % 60)
}

/** Points with thousands separators (`4,975`); [signed] adds `+` to positives. */
fun formatPoints(points: Long, signed: Boolean = false): String {
    val body = "%,d".format(Locale.US, points)
    return if (signed && points > 0) "+$body" else body
}

/** Accessible attempts text that always accompanies the pips (never colour alone). */
fun attemptsLabel(remaining: Int, total: Int): String =
    if (remaining <= 0) "No attempts left" else "${remaining.coerceAtMost(total)} of $total attempts left"

/** true = available pip, filled from the start. Out-of-range values are clamped. */
fun pipStates(remaining: Int, total: Int): List<Boolean> {
    val r = remaining.coerceIn(0, total)
    return List(total) { it < r }
}

/** Value shown by the ScoreCounter at animation [fraction] (0 → 0, 1 → [target]). */
fun countUpValue(target: Long, fraction: Float): Long =
    (target * fraction.coerceIn(0f, 1f).toDouble()).roundToLong()

enum class SeasonDay { Done, Today, Upcoming }

/** The 7 days of a season relative to [todayIndex] (0-based). */
fun seasonDays(todayIndex: Int, length: Int = 7): List<SeasonDay> {
    require(todayIndex in 0 until length) { "todayIndex $todayIndex outside 0..${length - 1}" }
    return List(length) {
        when {
            it < todayIndex -> SeasonDay.Done
            it == todayIndex -> SeasonDay.Today
            else -> SeasonDay.Upcoming
        }
    }
}

/**
 * One confetti piece in a unit box. [colorSlot] 0 = tertiary (celebration),
 * 1 = primary, 2 = secondary.
 */
data class ConfettiPiece(
    val x: Float, val y: Float, val rotation: Float, val size: Float,
    val colorSlot: Int, val isCircle: Boolean, val drift: Float,
)

/** Deterministic burst: same [seed] → same pieces (stable across recompositions). */
fun confettiPieces(count: Int, seed: Int): List<ConfettiPiece> {
    val r = Random(seed)
    return List(count) {
        ConfettiPiece(
            x = r.nextFloat(), y = r.nextFloat(), rotation = r.nextFloat() * 360f,
            size = 0.6f + r.nextFloat() * 0.6f, colorSlot = it % 3,
            isCircle = r.nextInt(4) == 0, drift = r.nextFloat() * 2f - 1f,
        )
    }
}

/**
 * Click feedback hook. `:core:audio` will provide an implementation that plays the UI
 * click and honours the Settings toggles (plan 09 §2B); until then it is silent.
 */
interface UiFeedback {
    val hapticsEnabled: Boolean get() = true
    fun click() {}
}

object NoUiFeedback : UiFeedback

val LocalUiFeedback = staticCompositionLocalOf<UiFeedback> { NoUiFeedback }

/** Order matters: haptic first (instant), then the sound, then the action (may navigate). */
fun handleRmClick(enabled: Boolean, onClick: () -> Unit, feedback: UiFeedback, haptic: () -> Unit) {
    if (!enabled) return
    haptic()
    feedback.click()
    onClick()
}
