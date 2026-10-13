package com.rmgames.core.data

/**
 * Daily best per (day, game). Best-of-2: keeps max when higherWins, min otherwise.
 * The daily best uploads once for the shared ranking. Phase 0: in-memory.
 */
interface ScoreRepository {
    /** Submits a run score, returns the current daily best. */
    fun submit(dayIndex: Long, gameId: String, value: Long, higherWins: Boolean): Long
    fun best(dayIndex: Long, gameId: String): Long?
}

class InMemoryScoreRepository : ScoreRepository {
    private val bests = mutableMapOf<Pair<Long, String>, Long>()

    override fun submit(dayIndex: Long, gameId: String, value: Long, higherWins: Boolean): Long {
        val key = dayIndex to gameId
        val current = bests[key]
        val next = when {
            current == null -> value
            higherWins -> maxOf(current, value)
            else -> minOf(current, value)
        }
        bests[key] = next
        return next
    }

    override fun best(dayIndex: Long, gameId: String): Long? = bests[dayIndex to gameId]
}
