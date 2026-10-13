package com.rmgames.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoreRepositoryTest {

    @Test
    fun `no best before first submit`() {
        val repo = InMemoryScoreRepository()
        assertNull(repo.best(dayIndex = 1, gameId = "g1"))
    }

    @Test
    fun `higher wins keeps the max`() {
        val repo = InMemoryScoreRepository()
        repo.submit(dayIndex = 1, gameId = "g1", value = 5000, higherWins = true)
        assertEquals(8000L, repo.submit(dayIndex = 1, gameId = "g1", value = 8000, higherWins = true))
        assertEquals(8000L, repo.submit(dayIndex = 1, gameId = "g1", value = 1000, higherWins = true))
    }

    @Test
    fun `lower wins keeps the min`() {
        val repo = InMemoryScoreRepository()
        repo.submit(dayIndex = 1, gameId = "g5", value = 900, higherWins = false)
        assertEquals(700L, repo.submit(dayIndex = 1, gameId = "g5", value = 700, higherWins = false))
        assertEquals(700L, repo.submit(dayIndex = 1, gameId = "g5", value = 1200, higherWins = false))
    }
}
