package com.rmgames.core.common

/**
 * Immutable catalogue of registered games keyed by [Game.id] (frozen in Phase 1b).
 * See docs/08-implementation-plan.md.
 *
 * Pattern: Registry. `:app` builds it once from every `:feature:game:*` entry, and
 * the NavHost resolves `session/{gameId}` through [find] instead of hardcoding one
 * route per game — so game authors never edit shared navigation code.
 */
class GameRegistry(entries: List<GameEntry>) {

    private val byId: Map<String, GameEntry> = entries.associateBy { it.game.id }

    init {
        require(byId.size == entries.size) {
            val duplicates = entries
                .groupingBy { it.game.id }
                .eachCount()
                .filterValues { it > 1 }
                .keys
            "Duplicate game ids: $duplicates"
        }
    }

    /** Every registered game, in registration order. */
    val all: List<GameEntry> get() = byId.values.toList()

    /** The entry for [gameId], or null when no such game is registered. */
    fun find(gameId: String): GameEntry? = byId[gameId]
}
