package com.rmgames.feature.game.rush

import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.AccelerometerSensor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Game 06 — Class Rush (docs/games/06-class-rush.md): tilt the phone to hop a late student
 * across Poruba's streets, tram tracks and an icy pond; the furthest row in 60 s wins.
 *
 * [start] builds a new seeded street, feeds every accelerometer reading to [TiltHopDetector]
 * and ticks [ClassRushEngine] every 16 ms on [GameScope.hostScope]. The score (rows) is
 * reported as it grows; the run ends with `onFinished(win)` when time runs out (win) or on a
 * hit / fall / idle (not a win). A failing sensor blocks the run: nothing is reported.
 */
class ClassRushGame(
    private val accelerometer: AccelerometerSensor,
    private val newSeed: () -> Long = System::nanoTime,
    /** Debug tuning log of the tilt angles (wired to Logcat by [classRushEntry]). */
    private val log: (String) -> Unit = {},
) : Game {
    override val id = "06-class-rush"
    override val title = "Class Rush"
    override val description = "Tilt to hop across streets, tram tracks and an icy pond. Get as far as you can in 60 seconds."
    override val durationMs = RUN_MS
    override val higherWins = true

    private val engine = ClassRushEngine(LaneGenerator(0)::lane)
    private var loopJob: Job? = null

    /** Frame state for [ClassRushScreen]; stable across runs (reset in [start]). */
    val state: StateFlow<RushState> = engine.state

    private val _tilt = MutableStateFlow<Tilt?>(null)

    /** Current tilt from neutral for the on-screen indicator; null while calibrating. */
    val tilt: StateFlow<Tilt?> = _tilt.asStateFlow()

    /** The lane drawn for [row] in the current run. */
    fun lane(row: Int): Lane = engine.lane(row)

    /** Debug-only arrow keys (emulator playtesting); [ClassRushScreen] gates them too. */
    fun requestDebugHop(hop: Hop) {
        if (BuildConfig.DEBUG) engine.hop(hop)
    }

    override fun start(scope: GameScope) {
        loopJob?.cancel()
        engine.reset(LaneGenerator(newSeed())::lane)
        val detector = TiltHopDetector()
        _tilt.value = null
        var lastLog = -LOG_EVERY_MS
        loopJob = scope.hostScope.launch {
            val collector = launch {
                accelerometer.readings()
                    .catch { engine.block() } // missing/broken sensor: void run, no score
                    .collect { reading ->
                        val now = engine.state.value.elapsedMs
                        val hop = detector.onSample(reading, now)
                        hop?.let(engine::hop)
                        if (detector.calibrated) _tilt.value = detector.tilt
                        if (hop != null || now - lastLog >= LOG_EVERY_MS) {
                            lastLog = now
                            log("t=$now lean=%.1f roll=%.1f hop=%s".format(detector.tilt.lean, detector.tilt.roll, hop))
                        }
                    }
            }
            try {
                var reported = -1
                while (isActive) {
                    delay(TICK_MS)
                    engine.tick(TICK_MS)
                    val s = engine.state.value
                    if (s.phase == RushPhase.BLOCKED) break
                    if (s.score != reported) {
                        reported = s.score
                        scope.onScore(s.score.toLong())
                    }
                    if (s.phase == RushPhase.OVER) {
                        scope.onFinished(s.end?.win == true)
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
        const val LOG_EVERY_MS = 250L
    }
}

/**
 * Entry for the `GameRegistry` (docs/08 — Game Hosting Contract). The game draws its own HUD
 * (time left + rows), so the host's score chip stays hidden.
 */
fun classRushEntry(accelerometer: AccelerometerSensor): GameEntry {
    val game = ClassRushGame(accelerometer, log = { if (BuildConfig.DEBUG) android.util.Log.d("RushTilt", it) })
    return GameEntry(game, drawsOwnHud = true, thumbnail = { ClassRushThumbnail() }) { ClassRushScreen(game) }
}
