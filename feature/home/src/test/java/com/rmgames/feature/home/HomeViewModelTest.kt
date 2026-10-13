package com.rmgames.feature.home

import com.rmgames.core.data.DailyGameSelector
import com.rmgames.core.data.InMemoryAttemptRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun `shows remaining attempts and daily game`() {
        val attempts = InMemoryAttemptRepository()
        val vm = HomeViewModel(
            attempts = attempts,
            dayIndex = 3,
            gameIds = listOf("g1", "g2", "g3")
        )
        assertEquals(2, vm.remainingAttempts())
        assertEquals(DailyGameSelector.select(3, listOf("g1", "g2", "g3")), vm.dailyGameId())
    }

    @Test
    fun `remaining attempts drop after a run is consumed elsewhere`() {
        val attempts = InMemoryAttemptRepository()
        attempts.consume(dayIndex = 3)
        val vm = HomeViewModel(attempts = attempts, dayIndex = 3, gameIds = listOf("g1"))
        assertEquals(1, vm.remainingAttempts())
    }
}
