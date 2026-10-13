package com.rmgames.core.data

/** Deterministic daily game: index = dayIndex mod game count. Same on all devices. */
object DailyGameSelector {

    fun select(dayIndex: Long, gameIds: List<String>): String {
        require(gameIds.isNotEmpty()) { "gameIds must not be empty" }
        return gameIds[Math.floorMod(dayIndex, gameIds.size.toLong()).toInt()]
    }
}
