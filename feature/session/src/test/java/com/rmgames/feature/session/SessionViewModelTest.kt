package com.rmgames.feature.session

import com.rmgames.core.data.InMemoryAttemptRepository
import com.rmgames.core.data.InMemoryScoreRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionViewModelTest {

    private fun viewModel(day: Long = 7) = SessionViewModel(
        attempts = InMemoryAttemptRepository(),
        scores = InMemoryScoreRepository(),
        dayIndex = day,
        gameId = "g1",
        higherWins = true
    )

    @Test
    fun `can play with attempts remaining`() {
        assertTrue(viewModel().canPlay())
    }

    @Test
    fun `starting a run consumes one attempt`() {
        val vm = viewModel()
        assertTrue(vm.startRun())
        assertEquals(1, vm.remainingAttempts())
    }

    @Test
    fun `cannot start a run with zero attempts`() {
        val vm = viewModel()
        vm.startRun()
        vm.finishRun(value = 100, win = false)
        vm.startRun()
        vm.finishRun(value = 200, win = false)
        assertFalse(vm.startRun())
        assertFalse(vm.canPlay())
    }

    @Test
    fun `finishing updates the daily best`() {
        val vm = viewModel()
        vm.startRun()
        val result = vm.finishRun(value = 5000, win = false)
        assertEquals(5000L, result.dailyBest)
        assertEquals(1, result.remainingAttempts)
    }
}
