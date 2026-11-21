package com.rmgames.feature.home

import com.rmgames.core.common.Game
import com.rmgames.core.common.GameScope
import com.rmgames.core.data.DailyGameSelector
import com.rmgames.core.data.DayClock
import com.rmgames.core.data.InMemoryAttemptRepository
import com.rmgames.core.data.InMemoryScoreRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class HomeViewModelTest {

    private class FakeGame(override val id: String, override val title: String = id) : Game {
        override val description = "How to play $id."
        override val durationMs = 60_000L
        override val higherWins = true
        override fun start(scope: GameScope) {}
        override fun stop() {}
    }

    private val games = listOf(FakeGame("g1"), FakeGame("g2"), FakeGame("g3"))
    private val clock = DayClock(ZoneOffset.UTC)
    private val wednesday = Instant.parse("2026-11-18T14:00:00Z").toEpochMilli()
    private val day = clock.dayIndex(wednesday)
    private val attempts = InMemoryAttemptRepository()
    private val scores = InMemoryScoreRepository()
    private fun vm() = HomeViewModel(attempts, scores, clock, games)

    @Test
    fun `a game this phone can't run disables Play with the reason, without touching attempts`() {
        val today = DailyGameSelector.select(day, games.map { it.id })
        val state = HomeViewModel(attempts, scores, clock, games) { id -> if (id == today) "Needs a gyroscope." else null }
            .state(wednesday)
        assertEquals("Needs a gyroscope.", state.unavailableReason)
        assertEquals(2, state.attemptsLeft)
        assertNull(vm().state(wednesday).unavailableReason)
    }

    @Test
    fun `shows today's game with its how-to`() {
        val state = vm().state(wednesday)
        val expected = DailyGameSelector.select(day, games.map { it.id })
        assertEquals(expected, state.gameId)
        assertEquals("How to play $expected.", state.howToPlay)
        assertEquals(60_000L, state.durationMs)
    }

    @Test
    fun `fresh day has two attempts and is not locked`() {
        val state = vm().state(wednesday)
        assertEquals(2, state.attemptsLeft)
        assertEquals(2, state.attemptsTotal)
        assertFalse(state.locked)
        assertNull(state.bestToday)
    }

    @Test
    fun `no attempts left locks Play and shows the day's best`() {
        val gameId = vm().state(wednesday).gameId
        attempts.consume(day); attempts.consume(day)
        scores.submit(day, gameId, 27, higherWins = true)
        val state = vm().state(wednesday)
        assertTrue(state.locked)
        assertEquals(0, state.attemptsLeft)
        assertEquals(27L, state.bestToday)
    }

    @Test
    fun `countdown targets the next noon`() {
        assertEquals(Instant.parse("2026-11-19T12:00:00Z").toEpochMilli(), vm().state(wednesday).nextResetMillis)
    }

    @Test
    fun `season strip shows season 1 day 3 on Wednesday 18 Nov`() {
        val state = vm().state(wednesday)
        assertEquals(1, state.season)
        assertEquals(2, state.seasonDayIndex)
    }

    @Test
    fun `debug reset restores today's attempts`() {
        attempts.consume(day); attempts.consume(day)
        val vm = vm()
        vm.resetAttempts(wednesday)
        assertEquals(2, vm.state(wednesday).attemptsLeft)
    }

    @Test
    fun `remaining attempts drop after a run is consumed elsewhere`() {
        attempts.consume(day)
        assertEquals(1, vm().state(wednesday).attemptsLeft)
    }
}
