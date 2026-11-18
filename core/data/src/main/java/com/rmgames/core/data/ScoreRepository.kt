package com.rmgames.core.data

/**
 * Daily best per (day, game). Best-of-2: keeps max when higherWins, min otherwise.
 * The daily best uploads once for the shared ranking. Phase 0: in-memory.
 */
interface ScoreRepository {
    /** Submits a run score, returns the current daily best. */
    fun submit(dayIndex: Long, gameId: String, value: Long, higherWins: Boolean): Long
    fun best(dayIndex: Long, gameId: String): Long?
    /** Every stored daily best, oldest day first (feeds the Seasons history). */
    fun history(): List<DailyBest>
}

/** The best run of one day's game. */
data class DailyBest(val dayIndex: Long, val gameId: String, val best: Long)

internal fun Map<Pair<Long, String>, Long>.toHistory(): List<DailyBest> =
    entries.map { DailyBest(it.key.first, it.key.second, it.value) }
        .sortedWith(compareBy({ it.dayIndex }, { it.gameId }))

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

    override fun history(): List<DailyBest> = bests.toHistory()
}
