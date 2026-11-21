package com.rmgames.core.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * The weekly reset needs no server: everything derives from the clock (Monday 12:00 local,
 * docs/01). Pins the first real rollover, season 1 → 2 on Monday 23 Nov 2026.
 */
class SeasonRolloverTest {

    private val zone = ZoneOffset.ofHours(1) // Poruba (CET)
    private val clock = DayClock(zone)
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).toInstant(zone).toEpochMilli()

    private val beforeNoon = at(2026, 11, 23, 11, 59)
    private val atNoon = at(2026, 11, 23, 12, 0)

    @Test
    fun `Monday 11-59 is still the last day of season 1, 12-00 opens season 2`() {
        assertEquals(SeasonDay(1, 6), SeasonCalendar.seasonDay(clock.dayIndex(beforeNoon)))
        assertEquals(SeasonDay(2, 0), SeasonCalendar.seasonDay(clock.dayIndex(atNoon)))
        assertEquals(atNoon, clock.nextResetMillis(beforeNoon)) // Main refreshes exactly then
    }

    @Test
    fun `season points start again from zero in season 2`() = runTest {
        val season1 = (16..22).map { DailyBest(LocalDate.of(2026, 11, it).toEpochDay(), "g$it", 10) }
        val today = clock.dayIndex(atNoon)
        val played = seasonPlayedDays(today, season1) { true }
        assertTrue("season 1 days must not count in season 2: $played", played.isEmpty())

        val ranking = object : Ranking {
            override val isOnline = true
            override suspend fun submit(entry: DailyEntry) = Unit
            override suspend fun standing(dayIndex: Long, gameId: String, myBest: Long, higherWins: Boolean) = standingOf(0, 5)
            override suspend fun top(dayIndex: Long, gameId: String, higherWins: Boolean, limit: Int) = emptyList<RankRow>()
        }
        assertEquals(SeasonStatus(0, null), RankingSync(ranking) { "hola" }.seasonTotal(played, today))
    }

    @Test
    fun `season 2 has its own daily order covering all seven games once`() {
        val ids = (1..7).map { "0$it-game" }
        val season2 = (0 until 7).map { DailyGameSelector.select(clock.dayIndex(atNoon) + it, ids) }
        assertEquals(ids.toSet(), season2.toSet())
    }
}
