package com.rmgames.core.common

/**
 * Position-based season scoring.
 * Position p of N players earns (N - p + 1) * 25. See docs/01-product-overview.md.
 */
object SeasonPoints {

    const val STEP = 25

    fun pointsFor(position: Int, playerCount: Int): Int {
        require(position >= 1) { "position must be >= 1" }
        require(playerCount >= 1) { "playerCount must be >= 1" }
        require(position <= playerCount) { "position must be <= playerCount" }
        return (playerCount - position + 1) * STEP
    }

    /**
     * Standard competition ranking: position = 1 + number of players strictly better.
     * Ties share the position; the next position is skipped.
     */
    fun positions(scores: Map<String, Long>, higherWins: Boolean): Map<String, Int> {
        return scores.mapValues { (_, value) ->
            1 + scores.values.count { other ->
                if (higherWins) other > value else other < value
            }
        }
    }
}
