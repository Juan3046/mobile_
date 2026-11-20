package com.rmgames.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Decision 2 (docs/09): seeded shuffle per 7-day season, same on every device. */
class DailyGameSelectorTest {

    private val games = listOf("g1", "g2", "g3", "g4", "g5")
    private val seasonStart = SeasonCalendar.SEASON_ONE_START_DAY
    private fun season(n: Int) = (0 until 7).map { DailyGameSelector.select(seasonStart + (n - 1) * 7L + it, games) }

    @Test
    fun `selection is deterministic for the same day`() {
        assertEquals(DailyGameSelector.select(42, games), DailyGameSelector.select(42, games))
    }

    @Test
    fun `input order does not matter so every device agrees`() {
        (0 until 30).forEach { d ->
            assertEquals(DailyGameSelector.select(seasonStart + d, games), DailyGameSelector.select(seasonStart + d, games.reversed()))
        }
    }

    @Test
    fun `no game repeats in a season until all have been played`() {
        (1..10).forEach { n ->
            val days = season(n)
            assertEquals("season $n: $days", games.size, days.take(games.size).toSet().size)
        }
    }

    @Test
    fun `with seven games a season plays each exactly once`() {
        val seven = (1..7).map { "g$it" }
        (1..10).forEach { n ->
            val days = (0 until 7).map { DailyGameSelector.select(seasonStart + (n - 1) * 7L + it, seven) }
            assertEquals(seven.toSet(), days.toSet())
        }
    }

    @Test
    fun `the order changes between seasons`() {
        val orders = (1..6).map { season(it) }.toSet()
        assertTrue("expected different orders, got $orders", orders.size > 1)
    }

    @Test
    fun `never the same game two days in a row, also across the season boundary`() {
        val days = (0 until 7 * 20).map { DailyGameSelector.select(seasonStart + it, games) }
        days.zipWithNext().forEachIndexed { i, (a, b) -> assertNotEquals("day $i → ${i + 1}", a, b) }
    }

    @Test
    fun `works before the first season and with a single game`() {
        assertEquals("only", DailyGameSelector.select(seasonStart - 3, listOf("only")))
        assertTrue(DailyGameSelector.select(seasonStart - 3, games) in games)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty game list is rejected`() {
        DailyGameSelector.select(dayIndex = 0, gameIds = emptyList())
    }
}
