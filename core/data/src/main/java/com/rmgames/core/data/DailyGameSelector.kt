package com.rmgames.core.data

import kotlin.random.Random

/**
 * Today's game (docs/01, plan 09 Decision 2): a **seeded shuffle per season**. Each 7-day
 * season shuffles the playable games with the season number as seed, so the daily game
 * looks random but is identical on every device, offline, with no server.
 *
 * - Ids are sorted first, so the registry order on a device never matters.
 * - Inside a season no game repeats until every game has been played (with ≥ 7 games each
 *   plays exactly once); with fewer games the season's order cycles.
 * - The first day of a season never repeats the last day of the previous one.
 * Kotlin's seeded [Random] is the same algorithm on every platform, so the order is stable.
 */
object DailyGameSelector {

    private const val SEED = 0x524D47 // "RMG"

    fun select(dayIndex: Long, gameIds: List<String>): String {
        require(gameIds.isNotEmpty()) { "gameIds must not be empty" }
        val ids = gameIds.distinct().sorted()
        if (ids.size == 1) return ids.single()
        val day = SeasonCalendar.seasonDay(dayIndex)
        val order = seasonOrder(day.season, ids)
        return order[day.dayOfSeason % ids.size]
    }

    /** The season's game order, adjusted so it does not start with the previous season's last game. */
    private fun seasonOrder(season: Int, ids: List<String>): List<String> {
        var order = shuffled(season, ids)
        if (season <= 1) return order
        // Walk forward from season 1 so each adjustment builds on the previous season's real order.
        var previous = shuffled(1, ids)
        for (s in 2..season) {
            order = shuffled(s, ids)
            val lastOfPrevious = previous[(SeasonCalendar.SEASON_LENGTH_DAYS - 1) % ids.size]
            if (order.first() == lastOfPrevious) order = order.drop(1) + order.first()
            previous = order
        }
        return order
    }

    private fun shuffled(season: Int, ids: List<String>) = ids.shuffled(Random(SEED + season))
}
