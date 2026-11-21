package com.rmgames.core.data

import java.time.LocalDate

/** Where a day falls in the season cycle. [dayOfSeason] is 0-based (0..6). */
data class SeasonDay(val season: Int, val dayOfSeason: Int)

/**
 * 7-day seasons, Monday noon → Monday noon (docs/01-product-overview.md). Season 1 starts
 * on Monday 16 Nov 2026 at 12:00 (decided 2026-11-18). Works on [DayClock] day indexes,
 * so the noon boundary is already applied. Days before the launch are pre-season 0.
 */
object SeasonCalendar {
    const val SEASON_LENGTH_DAYS = 7
    val SEASON_ONE_START_DAY: Long = LocalDate.of(2026, 11, 16).toEpochDay()

    fun seasonDay(dayIndex: Long): SeasonDay {
        val offset = dayIndex - SEASON_ONE_START_DAY
        return SeasonDay(
            season = Math.floorDiv(offset, SEASON_LENGTH_DAYS.toLong()).toInt() + 1,
            dayOfSeason = Math.floorMod(offset, SEASON_LENGTH_DAYS.toLong()).toInt(),
        )
    }
}
