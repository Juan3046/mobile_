package com.rmgames.feature.session

import com.rmgames.core.data.Standing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultRankingTest {

    @Test
    fun `a known standing shows position, players and points`() {
        val note = resultRankingNote(ResultRanking.Known(Standing(4, 212, 5225)))
        assertEquals("PROVISIONAL RANKING", note.label)
        assertEquals("#4 of 212", note.headline)
        assertTrue(note.body, note.body.contains("5,225 points"))
    }

    @Test
    fun `every other state explains itself without a position`() {
        listOf(ResultRanking.Practice, ResultRanking.Offline, ResultRanking.Loading, ResultRanking.Unknown, ResultRanking.NoScore)
            .forEach { assertEquals(null, resultRankingNote(it).headline) }
        assertTrue(resultRankingNote(ResultRanking.Unknown).body.contains("next time"))
        assertEquals("PRACTICE RUN", resultRankingNote(ResultRanking.Practice).label)
    }

    @Test
    fun `the initial state follows the run`() {
        assertEquals(ResultRanking.Practice, initialResultRanking(practice = true, online = true, hasBest = true))
        assertEquals(ResultRanking.Offline, initialResultRanking(practice = false, online = false, hasBest = true))
        assertEquals(ResultRanking.NoScore, initialResultRanking(practice = false, online = true, hasBest = false))
        assertEquals(ResultRanking.Loading, initialResultRanking(practice = false, online = true, hasBest = true))
    }
}
