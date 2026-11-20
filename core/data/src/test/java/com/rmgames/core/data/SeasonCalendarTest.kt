package com.rmgames.core.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SeasonCalendarTest {

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test
    fun `season 1 starts on Monday 16 Nov 2026`() {
        assertEquals(SeasonDay(season = 1, dayOfSeason = 0), SeasonCalendar.seasonDay(day(2026, 11, 16)))
    }

    @Test
    fun `wednesday 18 Nov is day 3 of season 1`() {
        assertEquals(SeasonDay(season = 1, dayOfSeason = 2), SeasonCalendar.seasonDay(day(2026, 11, 18)))
    }

    @Test
    fun `the seventh day closes the season and the next Monday opens season 2`() {
        assertEquals(SeasonDay(1, 6), SeasonCalendar.seasonDay(day(2026, 11, 22)))
        assertEquals(SeasonDay(2, 0), SeasonCalendar.seasonDay(day(2026, 11, 23)))
        assertEquals(SeasonDay(53, 0), SeasonCalendar.seasonDay(day(2027, 11, 15)))
    }

    @Test
    fun `days before launch are pre-season 0`() {
        assertEquals(SeasonDay(0, 6), SeasonCalendar.seasonDay(day(2026, 11, 15)))
    }

    @Test
    fun `day index follows the noon boundary of DayClock`() {
        // Monday 16 Nov at 11:59 still belongs to Sunday's day → pre-season.
        val clock = DayClock(java.time.ZoneOffset.UTC)
        val mondayMorning = java.time.Instant.parse("2026-11-16T11:59:00Z").toEpochMilli()
        val mondayNoon = java.time.Instant.parse("2026-11-16T12:00:00Z").toEpochMilli()
        assertEquals(0, SeasonCalendar.seasonDay(clock.dayIndex(mondayMorning)).season)
        assertEquals(1, SeasonCalendar.seasonDay(clock.dayIndex(mondayNoon)).season)
    }
}
