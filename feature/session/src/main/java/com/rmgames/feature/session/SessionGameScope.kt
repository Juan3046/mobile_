package com.rmgames.feature.session

import com.rmgames.core.common.Game
import com.rmgames.core.common.GameScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * The concrete [GameScope] the session hands to one game run (frozen before
 * Phase 1b). See docs/08-implementation-plan.md.
 *
 * Owns the whole run lifecycle so games (and the host screen) never manage scopes:
 * - [startRun] starts the game exactly once.
 * - [onScore] is forwarded only while the run is live; scores after the end are ignored.
 * - The **first** [onFinished] wins: the game is stopped once, [hostScope] is cancelled
 *   (sensor/camera listeners cannot leak), and only then is the verdict reported.
 * - [close] (screen leaving composition) is idempotent and does the same cleanup.
 *
 * Games must not suspend after calling [onFinished] — the hostScope is already
 * cancelled at that point; set final UI state first, then report the result.
 */
class SessionGameScope(
    private val game: Game,
    private val reportScore: (Long) -> Unit,
    private val reportFinished: (Boolean) -> Unit,
) : GameScope {

    /**
     * Per-run scope on the main dispatcher: games collect sensor/camera Flows here
     * and may update Compose state directly. Cancelled when the run ends or the
     * screen leaves — see [GameScope.hostScope].
     */
    override val hostScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var started = false
    private var stopped = false
    private var finished = false

    /** Starts the game. Subsequent calls are no-ops (one run = one start). */
    fun startRun() {
        if (started) return
        started = true
        game.start(this)
    }

    override fun onScore(value: Long) {
        if (finished) return
        reportScore(value)
    }

    override fun onFinished(win: Boolean) {
        if (finished) return
        finished = true
        endRun()
        reportFinished(win)
    }

    /** Ends the run from the host side (screen disposed). Idempotent. */
    fun close() = endRun()

    private fun endRun() {
        if (started && !stopped) {
            stopped = true
            game.stop()
        }
        hostScope.cancel()
    }
}
