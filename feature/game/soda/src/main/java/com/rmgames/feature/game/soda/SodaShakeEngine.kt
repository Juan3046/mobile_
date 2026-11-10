package com.rmgames.feature.game.soda

import com.rmgames.core.sensors.Vector3
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Phases of one run (docs/games/05-soda-shake.md). */
enum class SodaPhase {
    /** Stopwatch running; the player is shaking the phone. */
    RUNNING,
    /** The can popped; short celebration before the run is reported. */
    POPPING,
    /** Terminal: score frozen, the host reports the win. */
    DONE,
    /** Missing sensor: the run is void — no score, no result (docs/05 fallback). */
    BLOCKED,
}

/** Everything [SodaShakeScreen] renders for one frame. */
data class SodaShakeState(
    val phase: SodaPhase = SodaPhase.RUNNING,
    val elapsedMs: Long = 0L,
    val shakes: Int = 0,
    /** Gauge level 0..1 — 5 % per valid shake. */
    val pressure: Float = 0f,
    /** Time to pop in centiseconds; live stopwatch while RUNNING, frozen at the pop. */
    val scoreCentis: Long = 0L,
    /** Pop animation progress 0..1. */
    val popProgress: Float = 0f,
) {
    companion object {
        /** A run that has not started yet. */
        val INITIAL = SodaShakeState()
    }
}

/**
 * Pure game rules for Soda Shake (docs/games/05-soda-shake.md) — no Android, no
 * Compose: the host loop calls [tick] with the latest accelerometer sample and
 * [state] is what the screen renders.
 *
 * **Detection** is peak-based with three guards, so table taps and motor buzz
 * never count (tune by playtest):
 * 1. the magnitude must cross [SHAKE_THRESHOLD] (~0.5 g above gravity),
 * 2. it must settle back below [RELEASE_THRESHOLD] before the next peak counts
 *    (hysteresis — a held or sustained reading counts once),
 * 3. counted peaks are at least [DEAD_TIME_MS] apart **and alternate direction**
 *    (the sign of the dominant axis), so repeated one-way jolts are ignored.
 *
 * **Scoring** is a stopwatch: the clock runs from the start and freezes at the
 * 20th shake (time-to-pop in centiseconds, `higherWins = false`). A run that
 * never pops never finishes, so an idle run records nothing.
 */
class SodaShakeEngine(
    initialState: SodaShakeState = SodaShakeState.INITIAL,
    private val threshold: Float = SHAKE_THRESHOLD,
    private val releaseThreshold: Float = RELEASE_THRESHOLD,
    private val deadTimeMs: Long = DEAD_TIME_MS,
) {
    private val initial = initialState
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<SodaShakeState> = _state.asStateFlow()

    /** True once a peak has been seen and false again after it settles (hysteresis). */
    private var armed = false
    private var lastShakeAtMs: Long? = null
    private var lastDirection = 0
    private var popStartedAtMs = 0L

    /**
     * Advances the run by [rawDtMs] (no spatial simulation, so a stalled frame
     * is harmless) and detects a shake peak in [sample] (m/s², gravity included).
     */
    fun tick(rawDtMs: Long, sample: Vector3) {
        if (rawDtMs <= 0L) return
        val current = _state.value
        when (current.phase) {
            SodaPhase.BLOCKED, SodaPhase.DONE -> return

            SodaPhase.POPPING -> {
                val elapsed = current.elapsedMs + rawDtMs
                val progress = ((elapsed - popStartedAtMs) / POP_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                _state.value = current.copy(
                    elapsedMs = elapsed,
                    popProgress = progress,
                    phase = if (progress >= 1f) SodaPhase.DONE else SodaPhase.POPPING,
                )
                return
            }

            SodaPhase.RUNNING -> Unit // fall through to detection below
        }

        val elapsed = current.elapsedMs + rawDtMs
        var shakes = current.shakes
        var phase = SodaPhase.RUNNING

        val magnitude = sample.magnitude
        if (!armed && magnitude >= threshold) {
            armed = true // a crossing counts at most once, accepted or not
            val direction = directionOf(sample)
            if (isCountable(elapsed, direction)) {
                shakes += 1
                lastShakeAtMs = elapsed
                lastDirection = direction
                if (shakes >= SHAKES_TO_POP) {
                    phase = SodaPhase.POPPING
                    popStartedAtMs = elapsed
                }
            }
        }
        if (magnitude <= releaseThreshold) armed = false

        _state.value = current.copy(
            phase = phase,
            elapsedMs = elapsed,
            shakes = shakes,
            pressure = (shakes * PRESSURE_PER_SHAKE).coerceAtMost(1f),
            // One formula for the whole run: the POPPING branch never recomputes it.
            scoreCentis = elapsed / CENTIS_PER_SECOND,
            popProgress = 0f,
        )
    }

    /** Debug-only shake (emulator playtesting): counts without any sensor reading. */
    fun debugShake() {
        val current = _state.value
        if (current.phase != SodaPhase.RUNNING) return
        registerShake(current.elapsedMs, current)
    }

    /**
     * Missing sensor: the run is void. The score is zeroed and the game reports
     * nothing and never finishes — a blocked run must never record a result
     * (docs/02: never record fake scores).
     */
    fun block() {
        _state.value = _state.value.copy(phase = SodaPhase.BLOCKED, scoreCentis = 0L, popProgress = 0f)
    }

    /** Restores the run to its initial state; a retry starts clean. */
    fun reset() {
        _state.value = initial
        armed = false
        lastShakeAtMs = null
        lastDirection = 0
        popStartedAtMs = 0L
    }

    /** Dead time + direction alternation: the cheat guard of docs/05. */
    private fun isCountable(elapsedMs: Long, direction: Int): Boolean {
        val spacedOut = lastShakeAtMs?.let { elapsedMs - it >= deadTimeMs } ?: true
        return spacedOut && direction != 0 && direction != lastDirection
    }

    private fun registerShake(elapsedMs: Long, current: SodaShakeState) {
        val shakes = current.shakes + 1
        _state.value = current.copy(
            phase = if (shakes >= SHAKES_TO_POP) SodaPhase.POPPING else SodaPhase.RUNNING,
            shakes = shakes,
            pressure = (shakes * PRESSURE_PER_SHAKE).coerceAtMost(1f),
            scoreCentis = elapsedMs / CENTIS_PER_SECOND,
            popProgress = 0f,
        ).also {
            if (shakes >= SHAKES_TO_POP) popStartedAtMs = elapsedMs
        }
    }

    companion object {
        /** ~0.5 g above gravity — the comfortable level of the frozen ShakeDetector. */
        const val SHAKE_THRESHOLD = 15f
        /** The reading must settle back near gravity before the next peak counts. */
        const val RELEASE_THRESHOLD = 11f
        /** Minimum spacing between counted shakes (one jolt ≠ two shakes). */
        const val DEAD_TIME_MS = 120L
        const val SHAKES_TO_POP = 20
        const val PRESSURE_PER_SHAKE = 0.05f
        const val POP_DURATION_MS = 400L
        private const val CENTIS_PER_SECOND = 10L

        /**
         * Sign of the dominant axis at a peak: during a shake that axis carries
         * more than gravity (a peak above [SHAKE_THRESHOLD] proves it), and its
         * sign flips on every back-and-forth — the alternation guard rides on it.
         */
        internal fun directionOf(sample: Vector3): Int {
            val ax = abs(sample.x)
            val ay = abs(sample.y)
            val az = abs(sample.z)
            val dominant = when {
                ax >= ay && ax >= az -> sample.x
                ay >= az -> sample.y
                else -> sample.z
            }
            return when {
                dominant > 0f -> 1
                dominant < 0f -> -1
                else -> 0
            }
        }
    }
}
