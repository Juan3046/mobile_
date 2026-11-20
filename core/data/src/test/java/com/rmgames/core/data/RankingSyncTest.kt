package com.rmgames.core.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingSyncTest {

    /** Records uploads; every day's standing is #1 of 10 except [unknownDay]. */
    private class FakeRanking(override val isOnline: Boolean = true, val unknownDay: Long = -1) : Ranking {
        val submitted = mutableListOf<DailyEntry>()
        override suspend fun submit(entry: DailyEntry) { submitted += entry }
        override suspend fun standing(dayIndex: Long, gameId: String, myBest: Long, higherWins: Boolean) =
            if (dayIndex == unknownDay) null else standingOf(0, 10)
        override suspend fun top(dayIndex: Long, gameId: String, higherWins: Boolean, limit: Int) = emptyList<RankRow>()
    }

    @Test
    fun `reporting uploads the day's best under my nick and returns my standing`() = runTest {
        val ranking = FakeRanking()
        val standing = RankingSync(ranking) { "hola" }.report(PlayedDay(20, "g", 9, higherWins = false))
        assertEquals(listOf(DailyEntry(20, "g", 9, false, "hola")), ranking.submitted)
        assertEquals(Standing(1, 10, 250), standing)
    }

    @Test
    fun `no nick or offline build means nothing is uploaded and the standing is unknown`() = runTest {
        val noNick = FakeRanking()
        assertNull(RankingSync(noNick) { null }.report(PlayedDay(20, "g", 9, true)))
        assertTrue(noNick.submitted.isEmpty())
        val offline = FakeRanking(isOnline = false)
        assertNull(RankingSync(offline) { "hola" }.report(PlayedDay(20, "g", 9, true)))
        assertTrue(offline.submitted.isEmpty())
    }

    @Test
    fun `season re-uploads only today and yesterday and reads every played day`() = runTest {
        val ranking = FakeRanking(unknownDay = 17)
        val days = listOf(PlayedDay(17, "a", 1, true), PlayedDay(18, "b", 2, true), PlayedDay(19, "c", 3, true), PlayedDay(20, "d", 4, true))
        val standings = RankingSync(ranking) { "hola" }.season(days, today = 20)
        assertEquals(listOf(19L, 20L), ranking.submitted.map { it.dayIndex }.sorted())
        assertNull(standings[17])
        assertEquals(Standing(1, 10, 250), standings[18])
        assertEquals(750, seasonPoints(standings.values.toList()))
    }
}
