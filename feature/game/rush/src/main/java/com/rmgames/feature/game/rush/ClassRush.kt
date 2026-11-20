package com.rmgames.feature.game.rush

import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Game 06 — Class Rush (docs/games/06-class-rush.md): tap and swipe to hop a late student
 * across Poruba's streets, tram tracks and an icy pond; the furthest row in 60 s wins.
 *
 * [start] builds a new seeded street and ticks [ClassRushEngine] every 16 ms on
 * [GameScope.hostScope]; [onTouch] hops. The score (rows) is reported as it grows; the run
 * ends with `onFinished(win)` when time runs out (win) or on a hit / fall / idle (not a win).
 * Touch controls are an approved exception to the sensor rule (playtest 2026-11-20: tilt
 * was too hard to control).
 */
class ClassRushGame(
    private val newSeed: () -> Long = System::nanoTime,
    /** Debug log of how each run ended (wired to Logcat by [classRushEntry]). */
    private val log: (String) -> Unit = {},
) : Game {
    override val id = "06-class-rush"
    override val title = "Class Rush"
    override val description = "Tap to hop across streets, tram tracks and an icy pond; swipe to dodge. Get as far as you can in 60 seconds."
    override val durationMs = RUN_MS
    override val higherWins = true

    private val engine = ClassRushEngine(LaneGenerator(0)::lane)
    private var loopJob: Job? = null

    /** Frame state for [ClassRushScreen]; stable across runs (reset in [start]). */
    val state: StateFlow<RushState> = engine.state

    /** The lane drawn for [row] in the current run. */
    fun lane(row: Int): Lane = engine.lane(row)

    /** A tap or swipe from the screen (ignored outside a live run). */
    fun onTouch(hop: Hop) = engine.hop(hop)

    override fun start(scope: GameScope) {
        loopJob?.cancel()
        engine.reset(LaneGenerator(newSeed())::lane)
        loopJob = scope.hostScope.launch {
            var reported = -1
            while (isActive) {
                delay(TICK_MS)
                engine.tick(TICK_MS)
                val s = engine.state.value
                if (s.score != reported) {
                    reported = s.score
                    scope.onScore(s.score.toLong())
                }
                if (s.phase == RushPhase.OVER) {
                    log("end=${s.end} row=${s.row} x=%.2f t=${s.elapsedMs} lane=${engine.lane(s.row)}".format(s.x))
                    scope.onFinished(s.end?.win == true)
                    break
                }
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
 * Entry for the `GameRegistry` (docs/08 — Game Hosting Contract). No sensor and no permission
 * (touch exception); the game draws its own HUD (time left + rows).
 */
fun classRushEntry(): GameEntry {
    val game = ClassRushGame(log = { if (BuildConfig.DEBUG) android.util.Log.d("ClassRush", it) })
    return GameEntry(game, drawsOwnHud = true, thumbnail = { ClassRushThumbnail() }) { ClassRushScreen(game) }
}
