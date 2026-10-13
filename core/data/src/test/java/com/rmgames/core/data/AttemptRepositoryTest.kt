package com.rmgames.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttemptRepositoryTest {

    @Test
    fun `fresh day has two attempts`() {
        val repo = InMemoryAttemptRepository()
        assertEquals(2, repo.remaining(dayIndex = 10))
    }

    @Test
    fun `consuming reduces remaining attempts`() {
        val repo = InMemoryAttemptRepository()
        assertTrue(repo.consume(dayIndex = 10))
        assertEquals(1, repo.remaining(dayIndex = 10))
    }

    @Test
    fun `third consume on the same day is rejected`() {
        val repo = InMemoryAttemptRepository()
        repo.consume(dayIndex = 10)
        repo.consume(dayIndex = 10)
        assertFalse(repo.consume(dayIndex = 10))
        assertEquals(0, repo.remaining(dayIndex = 10))
    }

    @Test
    fun `new day refills attempts`() {
        val repo = InMemoryAttemptRepository()
        repo.consume(dayIndex = 10)
        repo.consume(dayIndex = 10)
        assertEquals(2, repo.remaining(dayIndex = 11))
    }
}
