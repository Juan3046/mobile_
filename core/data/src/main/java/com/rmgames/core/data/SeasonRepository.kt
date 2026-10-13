package com.rmgames.core.data

import com.rmgames.core.common.SeasonPoints

/**
 * Season = sum of daily position points over 7-day windows.
 * Missed days score 0 by absence. Phase 0: in-memory.
 */
interface SeasonRepository {
    fun dayPoints(standings: Map<String, Long>, higherWins: Boolean): Map<String, Int>
    fun addDay(points: Map<String, Int>)
    fun total(): Map<String, Int>
}

class InMemorySeasonRepository : SeasonRepository {

    private val totals = mutableMapOf<String, Int>()

    override fun dayPoints(standings: Map<String, Long>, higherWins: Boolean): Map<String, Int> {
        val positions = SeasonPoints.positions(standings, higherWins)
        val playerCount = standings.size
        return positions.mapValues { (_, position) ->
            SeasonPoints.pointsFor(position, playerCount)
        }
    }

    override fun addDay(points: Map<String, Int>) {
        points.forEach { (player, pts) ->
            totals[player] = (totals[player] ?: 0) + pts
        }
    }

    override fun total(): Map<String, Int> = totals.toMap()
}
