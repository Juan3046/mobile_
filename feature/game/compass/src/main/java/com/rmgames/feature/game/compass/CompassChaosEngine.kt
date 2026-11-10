package com.rmgames.feature.game.compass

import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Phases of one run (docs/games/04-compass-chaos.md). */
enum class CompassPhase {
    /** The fixed 20 s round is running; a direction is called. */
    RUNNING,
    /** Terminal: time is up, the score is frozen. */
    DONE,
    /** Missing sensor: the run is void — no score, no result (docs/04 fallback). */
    BLOCKED,
}

/** Everything [CompassChaosScreen] renders for one frame. */
data class CompassChaosState(
    val phase: CompassPhase = CompassPhase.RUNNING,
    val elapsedMs: Long = 0L,
    /** Correct directions in the round — higher wins. */
    val score: Int = 0,
    /** Called direction in degrees (0/90/180/270); `null` only before the first call. */
    val targetDegrees: Int? = null,
    val windowTotalMs: Long = CompassChaosEngine.WINDOW_EARLY_MS,
    val windowRemainingMs: Long = CompassChaosEngine.WINDOW_EARLY_MS,
    /** Smoothed device heading (0..360) for the needle. */
    val heading: Float = 0f,
    /** Turn size of the current call (90 or 180; 0 for the first call). */
    val turnDegrees: Int = 0,
) {
    companion object {
        /** A round that has not started yet (no direction called). */
        val INITIAL = CompassChaosState()
    }
}

/**
 * Pure game rules for Compass Chaos (docs/games/04-compass-chaos.md) — no Android,
 * no Compose: the host loop calls [tick] with the latest heading and [state] is
 * what the screen renders.
 *
 * **Round**: fixed [ROUND_MS] with no fail state — the clock always runs out and
 * the score freezes. **Calls**: a random cardinal is called; a window of
 * [WINDOW_EARLY_MS]/[WINDOW_MID_MS]/[WINDOW_HELL_MS] (by phase: 0–7 s / 7–14 s /
 * 14–20 s) must contain an alignment of [TOLERANCE_DEGREES] (strictly inside) to
 * score; a missed window simply calls the next direction. **Turns**: phase 1 only
 * turns ±90°, from phase 2 the opposite (180°) direction joins the mix (uniform
 * over the three non-current cardinals) — "rapid unpredictable changes" only
 * tighten the window further.
 *
 * The heading arrives already smoothed by the Android adapter's `HeadingFilter`
 * (docs/1a), so this class only normalizes it.
 */
class CompassChaosEngine(
    initialState: CompassChaosState = CompassChaosState.INITIAL,
    private val random: Random = Random.Default,
) {
    private val initial = initialState
    private val _state = MutableStateFlow(firstCall(initialState))
    val state: StateFlow<CompassChaosState> = _state.asStateFlow()

    /**
     * Advances the round by [rawDtMs] and evaluates the current call against
     * [heading] (degrees, any range — normalized). Aligned = score and the next
     * direction is called immediately; an expired window misses and calls too.
     */
    fun tick(rawDtMs: Long, heading: Float) {
        if (rawDtMs <= 0L) return
        val current = _state.value
        if (current.phase != CompassPhase.RUNNING) return

        val elapsed = current.elapsedMs + rawDtMs
        val normalized = normalize(heading)

        if (elapsed >= ROUND_MS) {
            // The round always lasts the full 20 s; the score is frozen here.
            _state.value = current.copy(
                phase = CompassPhase.DONE,
                elapsedMs = ROUND_MS,
                heading = normalized,
            )
            return
        }

        val target = current.targetDegrees
        if (target == null) {
            // Defensive: the constructor already calls a direction.
            _state.value = firstCall(current.copy(elapsedMs = elapsed, heading = normalized))
            return
        }

        val aligned = shortestDistance(normalized, target.toFloat()) < TOLERANCE_DEGREES
        val remaining = current.windowRemainingMs - rawDtMs

        when {
            // Alignment wins ties: a heading that arrives exactly as the window
            // closes still counts (at most one tick of grace).
            aligned -> callNext(
                current.copy(elapsedMs = elapsed, heading = normalized, score = current.score + 1),
                elapsed,
            )

            remaining > 0L -> _state.value = current.copy(
                elapsedMs = elapsed,
                heading = normalized,
                windowRemainingMs = remaining,
            )

            else -> callNext(
                current.copy(elapsedMs = elapsed, heading = normalized, windowRemainingMs = 0L),
                elapsed,
            )
        }
    }

    /**
     * Missing sensor: the run is void. The score is zeroed and the game reports
     * nothing and never finishes — a blocked run must never record a result
     * (docs/02: never record fake scores).
     */
    fun block() {
        _state.value = _state.value.copy(phase = CompassPhase.BLOCKED, score = 0, windowRemainingMs = 0L)
    }

    /** Restores the run to its initial state; a retry starts with a fresh call. */
    fun reset() {
        _state.value = firstCall(initial)
    }

    /** Ensures a direction is called: random first call unless one was supplied. */
    private fun firstCall(from: CompassChaosState): CompassChaosState {
        if (from.targetDegrees != null) return from
        val window = windowFor(from.elapsedMs)
        return from.copy(
            targetDegrees = random.nextInt(4) * 90,
            windowTotalMs = window,
            windowRemainingMs = window,
            turnDegrees = 0,
        )
    }

    /** Scores (or misses) [base] and calls the next direction for [elapsedMs]. */
    private fun callNext(base: CompassChaosState, elapsedMs: Long) {
        val window = windowFor(elapsedMs)
        val (target, turn) = nextTarget(base.targetDegrees ?: NORTH, elapsedMs)
        _state.value = base.copy(
            targetDegrees = target,
            windowTotalMs = window,
            windowRemainingMs = window,
            turnDegrees = turn,
        )
    }

    /** Next cardinal: ±90° only before 7 s, then ±90°/180° uniformly (docs/04). */
    private fun nextTarget(currentDegrees: Int, elapsedMs: Long): Pair<Int, Int> {
        val options = if (elapsedMs < PHASE_2_AT_MS) intArrayOf(-1, 1) else intArrayOf(-1, 1, 2)
        val offset = options[random.nextInt(options.size)]
        val wrapped = ((currentDegrees / 90 + offset) % 4 + 4) % 4
        return (wrapped * 90) to (abs(offset) * 90)
    }

    companion object {
        const val ROUND_MS = 20_000L
        const val PHASE_2_AT_MS = 7_000L
        const val PHASE_3_AT_MS = 14_000L
        const val WINDOW_EARLY_MS = 3_000L
        const val WINDOW_MID_MS = 2_000L
        const val WINDOW_HELL_MS = 1_200L

        /** Half a cardinal quadrant: strictly inside counts as aligned (docs/04). */
        const val TOLERANCE_DEGREES = 45f

        const val NORTH = 0
        const val EAST = 90
        const val SOUTH = 180
        const val WEST = 270

        private fun windowFor(elapsedMs: Long): Long = when {
            elapsedMs < PHASE_2_AT_MS -> WINDOW_EARLY_MS
            elapsedMs < PHASE_3_AT_MS -> WINDOW_MID_MS
            else -> WINDOW_HELL_MS
        }

        private fun normalize(degrees: Float): Float {
            val mod = degrees % 360f
            return if (mod < 0f) mod + 360f else mod
        }

        /** Shortest angular distance in degrees (0..180), wrap-around aware. */
        internal fun shortestDistance(a: Float, b: Float): Float {
            var delta = abs(a - b) % 360f
            if (delta > 180f) delta = 360f - delta
            return delta
        }
    }
}
