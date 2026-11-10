package com.rmgames.feature.game.compass

import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.CompassSensor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Game 04 — Compass Chaos (docs/games/04-compass-chaos.md): face the called
 * cardinal direction inside a shrinking window; most correct directions in the
 * fixed 20 s round wins.
 *
 * [start] drives [CompassChaosEngine] on a fixed 16 ms tick over [GameScope.hostScope],
 * feeds it the magnetometer heading (already smoothed by the Android adapter's
 * `HeadingFilter`), reports the score in points and ends the run with
 * `onFinished(true)` when the round expires — the round always completes, there
 * is no fail state. A missing sensor blocks the run instead
 * ([CompassChaosEngine.block]): nothing is reported and nothing is recorded —
 * never a fake score (docs/02-gameplay-rules.md). [stop] cancels the loop; the
 * host also cancels `hostScope`, so sensor listeners cannot leak.
 */
class CompassChaosGame(
    private val compass: CompassSensor,
) : Game {
    override val id = "04-compass-chaos"
    override val title = "Compass Chaos"
    override val description = "Face the called direction. 20 seconds."
    override val durationMs = 20_000L
    override val higherWins = true

    private val engine = CompassChaosEngine()

    /** Frame state for [CompassChaosScreen]; stable across runs (reset in [start]). */
    val state: StateFlow<CompassChaosState> = engine.state

    /** Latest heading from the sensor; the needle rests at 0° until one arrives. */
    private val heading = MutableStateFlow(0f)
    /** Debug-only rotation direction (−1/0/+1) while a key is held; 0 = stopped. */
    private val debugRotate = MutableStateFlow(0f)
    private var debugOffset = 0f
    private var loopJob: Job? = null

    /** Debug-only rotation (emulator playtesting); [CompassChaosScreen] gates it too. */
    fun setDebugRotate(direction: Float?) {
        if (!BuildConfig.DEBUG) return
        debugRotate.value = direction ?: 0f
    }

    override fun start(scope: GameScope) {
        loopJob?.cancel()
        engine.reset()
        heading.value = 0f
        debugRotate.value = 0f
        debugOffset = 0f
        loopJob = scope.hostScope.launch {
            val collector = launch {
                compass.headings()
                    .catch { engine.block() } // missing/broken sensor: void run, no score
                    .collect { heading.value = it }
            }
            try {
                var lastReported = -1L
                while (isActive) {
                    delay(TICK_MS)
                    if (BuildConfig.DEBUG && debugRotate.value != 0f) {
                        // Direction × 180°/s: a held key spins the needle like a body turn.
                        debugOffset += debugRotate.value * DEBUG_ROTATE_SPEED_DEG_PER_S * TICK_MS / 1_000f
                    }
                    engine.tick(TICK_MS, heading.value + debugOffset)
                    val snapshot = engine.state.value
                    // Blocked runs end silently: reporting would leave a score behind.
                    if (snapshot.phase == CompassPhase.BLOCKED) break
                    val score = snapshot.score.toLong()
                    if (score != lastReported) {
                        lastReported = score
                        scope.onScore(score)
                    }
                    if (snapshot.phase == CompassPhase.DONE) {
                        // Final state is already in `state`; report without suspending after.
                        scope.onFinished(true) // the round completed: no fail state
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
        const val DEBUG_ROTATE_SPEED_DEG_PER_S = 180f
    }
}

/**
 * Frozen Phase 1b entry signature (docs/08-implementation-plan.md — Game Hosting
 * Contract): `:app` wires this once into the `GameRegistry`, so gameplay work never
 * touches shared navigation.
 *
 * @param compass injected by `:app` (Android adapter in production, `FakeCompassSensor`
 * in tests); its headings are the needle of a run.
 */
fun compassChaosEntry(compass: CompassSensor): GameEntry {
    val game = CompassChaosGame(compass)
    return GameEntry(game) { CompassChaosScreen(game.state, game::setDebugRotate) }
}
