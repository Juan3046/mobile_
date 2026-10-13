package com.rmgames.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyGameSelectorTest {

    private val games = listOf("g1", "g2", "g3", "g4", "g5")

    @Test
    fun `day zero selects first game`() {
        assertEquals("g1", DailyGameSelector.select(dayIndex = 0, gameIds = games))
    }

    @Test
    fun `selection is deterministic for the same day`() {
        assertEquals(
            DailyGameSelector.select(dayIndex = 42, gameIds = games),
            DailyGameSelector.select(dayIndex = 42, gameIds = games)
        )
    }

    @Test
    fun `games repeat once the list is exhausted`() {
        assertEquals("g1", DailyGameSelector.select(dayIndex = 5, gameIds = games))
        assertEquals("g2", DailyGameSelector.select(dayIndex = 6, gameIds = games))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty game list is rejected`() {
        DailyGameSelector.select(dayIndex = 0, gameIds = emptyList())
    }
}
