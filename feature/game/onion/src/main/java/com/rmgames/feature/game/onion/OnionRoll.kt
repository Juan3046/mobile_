package com.rmgames.feature.game.onion

import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.GyroscopeSensor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Game 02 — Onion Roll (docs/games/02-onion-roll.md): tilt to steer the onion along
 * a supermarket checkout belt and survive until the belt runs out.
 *
 * Pure survival (no win state): [start] drives [OnionRollEngine] on a fixed 16 ms
 * tick over [GameScope.hostScope], feeds it the gyroscope's z-axis rate, reports
 * the score in centiseconds and ends the run with `onFinished(false)` once the
 * fall animation completes. [stop] cancels the loop; the host also cancels
 * `hostScope`, so sensor listeners cannot leak.
 */
class OnionRollGame(
    private val gyroscope: GyroscopeSensor,
) : Game {
    override val id = "02-onion-roll"
    override val title = "Onion Roll"
    override val description = "Tilt to steer the onion. Survive the belt."
    override val durationMs = 90_000L
    override val higherWins = true

    private val engine = OnionRollEngine()

    /** Frame state for [OnionRollScreen]; stable across runs (reset in [start]). */
    val state: StateFlow<OnionRollState> = engine.state

    private val omegaZ = MutableStateFlow(0f)
    private val debugSteer = MutableStateFlow<Float?>(null)
    private var loopJob: Job? = null

    /** Debug-only keyboard steering; [OnionRollScreen] gates it on BuildConfig.DEBUG. */
    fun setDebugSteer(value: Float?) {
        debugSteer.value = value
    }

    override fun start(scope: GameScope) {
        loopJob?.cancel()
        engine.reset()
        omegaZ.value = 0f
        debugSteer.value = null
        loopJob = scope.hostScope.launch {
            val collector = launch {
                gyroscope.readings().collect { sample -> omegaZ.value = sample.z }
            }
            try {
                var lastReported = -1L
                while (isActive) {
                    delay(TICK_MS)
                    engine.tick(TICK_MS, omegaZ.value, debugSteer.value)
                    val snapshot = engine.state.value
                    if (snapshot.scoreCentis != lastReported) {
                        lastReported = snapshot.scoreCentis
                        scope.onScore(snapshot.scoreCentis)
                    }
                    if (snapshot.phase == Phase.DONE) {
                        // Final state is already in `state`; report without suspending after.
                        scope.onFinished(false)
                        break
                    }
                }
            } finally {
                collector.cancel()
            }
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
 * Frozen Phase 1b entry signature (docs/08-implementation-plan.md — Game Hosting
 * Contract): `:app` wires this once into the `GameRegistry`, so gameplay work never
 * touches shared navigation.
 *
 * @param gyroscope injected by `:app` (Android adapter in production,
 * `FakeGyroscopeSensor` in tests); its z-axis rate is the steering input of a run.
 */
fun onionRollEntry(gyroscope: GyroscopeSensor): GameEntry {
    val game = OnionRollGame(gyroscope)
    return GameEntry(game) { OnionRollScreen(game.state, game::setDebugSteer) }
}
