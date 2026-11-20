package com.rmgames.feature.game.onion

import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.GyroscopeSensor
import com.rmgames.core.sensors.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Game 02 — Onion Roll (docs/games/02-onion-roll.md): tilt to steer the onion along
 * an endless supermarket checkout belt and survive as long as possible.
 *
 * Pure survival (no win state): [start] drives [OnionRollEngine] on a fixed 16 ms
 * tick over [GameScope.hostScope], feeds it the gyroscope's [tiltRate], reports
 * the score in whole seconds and ends the run with `onFinished(false)` once the
 * fall animation and "YOU LOST!" banner complete. [stop] cancels the loop; the host also cancels
 * `hostScope`, so sensor listeners cannot leak.
 */
class OnionRollGame internal constructor(
    private val gyroscope: GyroscopeSensor,
    private val engine: OnionRollEngine,
) : Game {
    constructor(gyroscope: GyroscopeSensor) : this(gyroscope, OnionRollEngine())

    override val id = "02-onion-roll"
    override val title = "Onion Roll"
    override val description = "Tilt to steer the onion. Survive the belt."
    /** Endless run (approved exception, docs/02-gameplay-rules.md): only a fall ends it. */
    override val durationMs = Long.MAX_VALUE
    override val higherWins = true

    /** Frame state for [OnionRollScreen]; stable across runs (reset in [start]). */
    val state: StateFlow<OnionRollState> = engine.state

    private val tilt = MutableStateFlow(0f)
    private val debugSteer = MutableStateFlow<Float?>(null)
    private var loopJob: Job? = null
    private val _practice = MutableStateFlow(false)

    /** True while an unscored debug practice run is playing (see [restart]). */
    val practice: StateFlow<Boolean> = _practice.asStateFlow()

    /** Debug-only keyboard steering; [OnionRollScreen] gates it on BuildConfig.DEBUG. */
    fun setDebugSteer(value: Float?) {
        debugSteer.value = value
    }

    /**
     * Debug-only restart (button gated on BuildConfig.DEBUG in [OnionRollScreen],
     * docs/games/02-onion-roll.md — Controls):
     * - while a loop is live (running or the "YOU LOST!" banner) it resets the run in place;
     * - once the finish was reported the host has shut the run down, so it starts an
     *   **unscored practice run** on [practiceScope] (the screen's scope): nothing
     *   reaches the host, and it ends when that scope is cancelled.
     */
    fun restart(practiceScope: CoroutineScope? = null) {
        if (loopJob?.isActive == true) {
            resetInputs()
            return
        }
        if (practiceScope == null) return
        resetInputs()
        _practice.value = true
        loopJob = practiceScope.launch { runLoop(onScore = {}, onFinished = {}) }
    }

    override fun start(scope: GameScope) {
        loopJob?.cancel()
        resetInputs()
        _practice.value = false
        loopJob = scope.hostScope.launch {
            runLoop(onScore = scope::onScore, onFinished = { scope.onFinished(false) })
        }
    }

    private fun resetInputs() {
        engine.reset()
        tilt.value = 0f
        debugSteer.value = null
    }

    /** Fixed 16 ms tick loop; reports through the given callbacks (no-ops for practice). */
    private suspend fun CoroutineScope.runLoop(onScore: (Long) -> Unit, onFinished: () -> Unit) {
        val collector = launch {
            gyroscope.readings().collect { sample -> tilt.value = tiltRate(sample) }
        }
        try {
            var lastReported = -1L
            while (isActive) {
                delay(TICK_MS)
                engine.tick(TICK_MS, tilt.value, debugSteer.value)
                val snapshot = engine.state.value
                if (snapshot.scoreSeconds != lastReported) {
                    lastReported = snapshot.scoreSeconds
                    onScore(snapshot.scoreSeconds)
                }
                if (snapshot.phase == Phase.DONE) {
                    // Final state is already in `state`; report without suspending after.
                    onFinished()
                    break
                }
            }
        } finally {
            collector.cancel()
        }
    }

    override fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    private companion object {
        const val TICK_MS = 16L
    }
}

/**
 * Left/right tilt rate (rad/s, positive = lean left) from a gyroscope sample,
 * for any hold (docs/games/02-onion-roll.md — Controls): an upright phone tilts
 * around z (steering-wheel motion, +z = lean left), a flat phone rolls around y
 * (+y = right edge dips = lean right), and a semi-upright hold mixes both.
 */
internal fun tiltRate(sample: Vector3): Float = sample.z - sample.y

/**
 * Frozen Phase 1b entry signature (docs/08-implementation-plan.md — Game Hosting
 * Contract): `:app` wires this once into the `GameRegistry`, so gameplay work never
 * touches shared navigation.
 *
 * @param gyroscope injected by `:app` (Android adapter in production,
 * `FakeGyroscopeSensor` in tests); its [tiltRate] is the steering input of a run.
 */
fun onionRollEntry(gyroscope: GyroscopeSensor): GameEntry {
    val game = OnionRollGame(gyroscope)
    // Draws its own seconds counter and "YOU LOST!" banner (docs/08 — own-HUD opt-out).
    return GameEntry(game, drawsOwnHud = true, thumbnail = { OnionRollThumbnail() }) { OnionRollScreen(game.state, game::setDebugSteer, game.practice, game::restart) }
}
