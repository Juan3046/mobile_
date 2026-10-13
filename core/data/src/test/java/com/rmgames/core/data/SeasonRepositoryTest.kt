package com.rmgames.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonRepositoryTest {

    @Test
    fun `third of four earns fifty points`() {
        val repo = InMemorySeasonRepository()
        val day = repo.dayPoints(
            mapOf("a" to 9000L, "b" to 8000L, "me" to 7000L, "d" to 6000L),
            higherWins = true
        )
        assertEquals(50, day["me"])
    }

    @Test
    fun `season total accumulates daily points`() {
        val repo = InMemorySeasonRepository()
        repo.addDay(mapOf("me" to 50, "a" to 100))
        repo.addDay(mapOf("me" to 25, "a" to 100))
        assertEquals(75, repo.total()["me"])
        assertEquals(200, repo.total()["a"])
    }

    @Test
    fun `missed day scores zero by absence`() {
        val repo = InMemorySeasonRepository()
        repo.addDay(mapOf("a" to 100))
        assertEquals(null, repo.total()["me"])
    }
}
