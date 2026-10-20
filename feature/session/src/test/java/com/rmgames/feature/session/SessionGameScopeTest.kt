package com.rmgames.feature.session

import com.rmgames.core.common.Game
import com.rmgames.core.common.GameScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionGameScopeTest {

    private class FakeGame : Game {
        var startCount = 0
        var stopCount = 0
        override val id = "fake-game"
        override val title = "Fake"
        override val description = "Fake game for host tests"
        override val durationMs = 30_000L
        override val higherWins = true
        override fun start(scope: GameScope) {
            startCount++
        }

        override fun stop() {
            stopCount++
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startRun starts the game exactly once`() {
        val game = FakeGame()
        val run = SessionGameScope(game, reportScore = {}, reportFinished = {})

        run.startRun()
        run.startRun()

        assertEquals(1, game.startCount)
    }

    @Test
    fun `onScore forwards scores to the host while the run is live`() {
        val scores = mutableListOf<Long>()
        val run = SessionGameScope(FakeGame(), reportScore = scores::add, reportFinished = {})
        run.startRun()

        run.onScore(100)
        run.onScore(250)

        assertEquals(listOf(100L, 250L), scores)
    }

    @Test
    fun `scores reported after the run finished are ignored`() {
        val scores = mutableListOf<Long>()
        val run = SessionGameScope(FakeGame(), reportScore = scores::add, reportFinished = {})
        run.startRun()

        run.onScore(100)
        run.onFinished(false)
        run.onScore(999)

        assertEquals(listOf(100L), scores)
    }

    @Test
    fun `only the first onFinished is reported to the host`() {
        val outcomes = mutableListOf<Boolean>()
        val run = SessionGameScope(FakeGame(), reportScore = {}, reportFinished = outcomes::add)
        run.startRun()

        run.onFinished(true)
        run.onFinished(false)

        assertEquals(listOf(true), outcomes)
    }

    @Test
    fun `finishing stops the game and cancels the hostScope`() {
        val game = FakeGame()
        val run = SessionGameScope(game, reportScore = {}, reportFinished = {})
        run.startRun()
        val job = run.hostScope.launch { awaitCancellation() }

        run.onFinished(true)

        assertEquals(1, game.stopCount)
        assertFalse(job.isActive)
        assertFalse(run.hostScope.isActive)
    }

    @Test
    fun `closing without a finish still stops the game and cancels the hostScope`() {
        val game = FakeGame()
        val run = SessionGameScope(game, reportScore = {}, reportFinished = {})
        run.startRun()

        run.close()

        assertEquals(1, game.stopCount)
        assertFalse(run.hostScope.isActive)
    }

    @Test
    fun `game is stopped only once across finish and repeated closes`() {
        val game = FakeGame()
        val run = SessionGameScope(game, reportScore = {}, reportFinished = {})
        run.startRun()

        run.onFinished(true)
        run.close()
        run.close()

        assertEquals(1, game.stopCount)
    }

    @Test
    fun `closing a run that never started does not stop the game`() {
        val game = FakeGame()
        val run = SessionGameScope(game, reportScore = {}, reportFinished = {})

        run.close()

        assertEquals(0, game.stopCount)
        assertFalse(run.hostScope.isActive)
    }
}
