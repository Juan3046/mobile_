package com.rmgames.core.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankingTest {

    @Test
    fun `standing follows competition ranking and the points formula`() {
        // 3 players better than me out of 212 → #4, (212 − 4 + 1) × 25.
        assertEquals(Standing(position = 4, players = 212, points = 5225), standingOf(betterCount = 3, players = 212))
        assertEquals(Standing(1, 1, 25), standingOf(betterCount = 0, players = 1))
    }

    @Test
    fun `my own upload not yet counted still yields a valid standing`() {
        // Counting raced the upload: I'm not in the total yet → count me.
        assertEquals(Standing(1, 1, 25), standingOf(betterCount = 0, players = 0))
        assertEquals(Standing(3, 3, 25), standingOf(betterCount = 2, players = 2))
    }

    @Test
    fun `season points add up each played day`() {
        assertEquals(5225, seasonPoints(listOf(Standing(1, 100, 2500), Standing(2, 110, 2725), null)))
        assertEquals(0, seasonPoints(emptyList()))
    }

    @Test
    fun `offline ranking knows nothing and never fails`() = runTest {
        val r = OfflineRanking()
        r.submit(DailyEntry(dayIndex = 10, gameId = "g", best = 5, higherWins = true, nick = "hola"))
        assertNull(r.standing(10, "g", 5, higherWins = true))
        assertEquals(emptyList<RankRow>(), r.top(10, "g", higherWins = true))
        assertEquals(false, r.isOnline)
    }
}
