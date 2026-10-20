package com.rmgames.core.common

import kotlinx.coroutines.CoroutineScope

/**
 * Contract every minigame implements (frozen in Phase 0; extended in Phase 1a
 * with [hostScope]). See docs/08-implementation-plan.md.
 */
interface Game {
    val id: String
    val title: String
    val description: String
    /** Standard 30_000–90_000 ms; documented exceptions only. */
    val durationMs: Long
    /** False for lowest-wins games (e.g. 05 soda time-to-pop). */
    val higherWins: Boolean
    fun start(scope: GameScope)
    fun stop()
}

interface GameScope {
    /**
     * Scope owned by the session host. Games launch their loops/sensor collection
     * here (e.g. colecting [com.rmgames.core.sensors.GyroscopeSensor] readings);
     * the host cancels it when the run ends, so games never manage their own
     * scopes and cannot leak listeners.
     */
    val hostScope: CoroutineScope
    /** Raw game score (centiseconds or points). Daily best = max (or min). */
    fun onScore(value: Long)
    fun onFinished(win: Boolean)
}
