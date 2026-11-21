package com.rmgames.feature.game.soda

import com.rmgames.core.sensors.Vector3
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
 * Compose: the host feeds every accelerometer reading to [onSample], advances the
 * clock with [tick], and [state] is what the screen renders.
 *
 * **Detection** counts *any* shake — up/down, side to side, forwards/backwards or
 * mixed (playtest 2026-11-20: the old "alternate direction on the dominant axis"
 * guard dropped half of the up/down shakes and felt broken):
 * 1. gravity is removed with a low-pass estimate, so only the hand's movement is
 *    measured, whatever the phone's orientation (tilting slowly never counts),
 * 2. that movement must cross [SHAKE_THRESHOLD] (~0.7 g),
 * 3. it must calm down below [RELEASE_THRESHOLD] before the next peak counts
 *    (hysteresis — one jolt counts once), and counted peaks are at least
 *    [DEAD_TIME_MS] apart. The phone's own vibration stays far below the threshold.
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

    /** True once a peak has been seen and false again after it calms down (hysteresis). */
    private var armed = false
    private var lastShakeAtMs: Long? = null
    private var popStartedAtMs = 0L
    /** Low-pass gravity estimate; null until the first reading. */
    private var gravity: Vector3? = null
    /** Since when the movement has stayed at shake level without a pause (null = it paused). */
    private var unsettledSinceMs: Long? = null

    /**
     * Advances the run by [rawDtMs] (no spatial simulation, so a stalled frame
     * is harmless), then reads [sample] if one is given.
     */
    fun tick(rawDtMs: Long, sample: Vector3? = null) {
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

            SodaPhase.RUNNING -> {
                val elapsed = current.elapsedMs + rawDtMs
                _state.value = current.copy(elapsedMs = elapsed, scoreCentis = elapsed / CENTIS_PER_SECOND)
            }
        }
        sample?.let(::onSample)
    }

    /** One accelerometer reading (m/s², gravity included): a movement peak counts a shake. */
    fun onSample(sample: Vector3) {
        val g = gravity ?: sample
        val movement = Vector3(sample.x - g.x, sample.y - g.y, sample.z - g.z).magnitude
        // Follow gravity quickly while calm, barely during a shake (a hard push is not a tilt).
        val k = if (movement < threshold) GRAVITY_FILTER else GRAVITY_FILTER_SHAKING
        gravity = Vector3(g.x + k * (sample.x - g.x), g.y + k * (sample.y - g.y), g.z + k * (sample.z - g.z))
        val current = _state.value
        if (current.phase != SodaPhase.RUNNING) return
        // A real shake swings back through calm many times a second. "Moving" without pause
        // for this long means the reference is wrong (the run began mid-shake): re-seed it.
        if (movement >= threshold) {
            val since = unsettledSinceMs ?: current.elapsedMs.also { unsettledSinceMs = it }
            if (current.elapsedMs - since >= RESEED_AFTER_MS) {
                gravity = sample
                armed = false
                unsettledSinceMs = null
                return
            }
        } else {
            unsettledSinceMs = null
        }
        if (!armed && movement >= threshold) {
            armed = true // a crossing counts at most once, accepted or not
            val spacedOut = lastShakeAtMs?.let { current.elapsedMs - it >= deadTimeMs } ?: true
            if (spacedOut) {
                lastShakeAtMs = current.elapsedMs
                registerShake(current.elapsedMs, current)
            }
        }
        if (movement <= releaseThreshold) armed = false
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
        popStartedAtMs = 0L
        gravity = null
        unsettledSinceMs = null
    }

    private fun registerShake(elapsedMs: Long, current: SodaShakeState) {
        val shakes = current.shakes + 1
        _state.value = current.copy(
            phase = if (shakes >= SHAKES_TO_POP) SodaPhase.POPPING else SodaPhase.RUNNING,
            shakes = shakes,
            pressure = (shakes * PRESSURE_PER_SHAKE).coerceAtMost(1f),
            scoreCentis = elapsedMs / CENTIS_PER_SECOND,
            popProgress = 0f,
        )
        if (shakes >= SHAKES_TO_POP) popStartedAtMs = elapsedMs
    }

    companion object {
        /** Hand movement (gravity removed) that counts as a shake: ~0.7 g. */
        const val SHAKE_THRESHOLD = 7f
        /** The movement must calm down below this before the next peak counts. */
        const val RELEASE_THRESHOLD = 4f
        /** Minimum spacing between counted shakes (one jolt ≠ two shakes). */
        const val DEAD_TIME_MS = 90L
        const val SHAKES_TO_POP = 20
        const val PRESSURE_PER_SHAKE = 0.05f
        const val POP_DURATION_MS = 400L
        private const val CENTIS_PER_SECOND = 10L
        /** Per-reading weight of the gravity low-pass (~0.2 s at SENSOR_DELAY_GAME). */
        private const val GRAVITY_FILTER = 0.1f
        private const val GRAVITY_FILTER_SHAKING = 0.02f
        /** Continuous "movement" longer than this re-seeds the gravity reference. */
        private const val RESEED_AFTER_MS = 400L
    }
}
