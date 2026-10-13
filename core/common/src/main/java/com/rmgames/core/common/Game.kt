package com.rmgames.core.common

/**
 * Contract every minigame implements (frozen in Phase 0).
 * See docs/08-implementation-plan.md.
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
    /** Raw game score (centiseconds or points). Daily best = max (or min). */
    fun onScore(value: Long)
    fun onFinished(win: Boolean)
}
