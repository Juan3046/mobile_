package com.rmgames.feature.game.escape

import com.rmgames.core.common.GameScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class EscapePorubaGameTest {

    @Test
    fun `successful run reports score and win after escape animation`() = runTest {
        val engine = EscapePorubaEngine(Random(12))
        val game = EscapePorubaGame(engine)
        val scope = RecordingGameScope(backgroundScope)
        game.start(scope)
        engine.disableSpawnsForTest()

        advanceTimeBy(90_000L)
        runCurrent()
        assertEquals(EscapePhase.ESCAPED, game.state.value.phase)
        assertFalse(scope.finished)

        advanceTimeBy(850L)
        runCurrent()
        assertTrue(scope.finished)
        assertTrue(scope.won)
        assertEquals(9_000L, scope.lastScore)
        game.stop()
    }

    @Test
    fun `collision shows impact freeze before reporting game over`() = runTest {
        val engine = EscapePorubaEngine(Random(13))
        val game = EscapePorubaGame(engine)
        val scope = RecordingGameScope(backgroundScope)
        game.start(scope)
        engine.debugSpawnHazard(lane = 2, y = EscapePorubaEngine.PLAYER_Y)

        advanceTimeBy(16L)
        runCurrent()
        assertEquals(EscapePhase.CRASHED, game.state.value.phase)
        assertFalse(game.resultVisible.value)
        assertFalse(scope.finished)

        advanceTimeBy(100L)
        runCurrent()
        assertTrue(game.resultVisible.value)
        assertTrue(scope.finished)
        assertFalse(scope.won)
        game.stop()
    }

    private class RecordingGameScope(override val hostScope: CoroutineScope) : GameScope {
        var lastScore = 0L
        var finished = false
        var won = false

        override fun onScore(value: Long) {
            lastScore = value
        }

        override fun onFinished(win: Boolean) {
            finished = true
            won = win
        }
    }
}
