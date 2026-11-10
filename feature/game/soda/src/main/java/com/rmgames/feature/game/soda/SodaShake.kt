package com.rmgames.feature.game.soda

import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.AccelerometerSensor
import com.rmgames.core.sensors.Vector3
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Game 05 — Soda Shake (docs/games/05-soda-shake.md): shake the phone until the
 * can pops at 20 valid shakes; the fastest time-to-pop wins.
 *
 * [start] drives [SodaShakeEngine] on a fixed 16 ms tick over [GameScope.hostScope],
 * feeds it the accelerometer's latest sample, reports the stopwatch in centiseconds
 * and ends the run with `onFinished(true)` once the pop animation completes. A
 * missing sensor blocks the run instead ([SodaShakeEngine.block]): nothing is
 * reported and nothing is recorded — never a fake score (docs/02-gameplay-rules.md).
 * [stop] cancels the loop; the host also cancels `hostScope`, so sensor listeners
 * cannot leak.
 */
class SodaShakeGame(
    private val accelerometer: AccelerometerSensor,
) : Game {
    override val id = "05-soda-shake"
    override val title = "Soda Shake"
    override val description = "Shake 20 times to pop the can. Fastest wins."
    /** Variable round ending at 20 shakes; typical duration as placeholder. */
    override val durationMs = 10_000L
    override val higherWins = false

    private val engine = SodaShakeEngine()

    /** Frame state for [SodaShakeScreen]; stable across runs (reset in [start]). */
    val state: StateFlow<SodaShakeState> = engine.state

    /** Latest accelerometer sample; rests at zero until the sensor delivers one. */
    private val sample = MutableStateFlow(Vector3(0f, 0f, 0f))
    private var loopJob: Job? = null

    /** Debug-only shake (emulator/desktop playtesting); [SodaShakeScreen] gates it too. */
    fun requestDebugShake() {
        if (!BuildConfig.DEBUG) return
        engine.debugShake()
    }

    override fun start(scope: GameScope) {
        loopJob?.cancel()
        engine.reset()
        sample.value = Vector3(0f, 0f, 0f)
        loopJob = scope.hostScope.launch {
            val collector = launch {
                accelerometer.readings()
                    .catch { engine.block() } // missing/broken sensor: void run, no score
                    .collect { sample.value = it }
            }
            try {
                var lastReported = -1L
                while (isActive) {
                    delay(TICK_MS)
                    engine.tick(TICK_MS, sample.value)
                    val snapshot = engine.state.value
                    // Blocked runs end silently: reporting would leave a score behind.
                    if (snapshot.phase == SodaPhase.BLOCKED) break
                    if (snapshot.scoreCentis != lastReported) {
                        lastReported = snapshot.scoreCentis
                        scope.onScore(snapshot.scoreCentis)
                    }
                    if (snapshot.phase == SodaPhase.DONE) {
                        // Final state is already in `state`; report without suspending after.
                        scope.onFinished(true) // the can popped: a win
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
 * @param accelerometer injected by `:app` (Android adapter in production,
 * `FakeAccelerometerSensor` in tests); its peaks are the shakes of a run.
 */
fun sodaShakeEntry(accelerometer: AccelerometerSensor): GameEntry {
    val game = SodaShakeGame(accelerometer)
    return GameEntry(game) { SodaShakeScreen(game.state, game::requestDebugShake) }
}
