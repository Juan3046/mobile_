package com.rmgames.feature.game.blink

import com.rmgames.core.common.GameScope
import com.rmgames.core.vision.EyeState
import com.rmgames.core.vision.FakeBlinkDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Tests for the [BlinkSniperGame] wiring: detector Flow -> shot, virtual clock ->
 * run end, and score reporting to the host (AGENTS.md §5 — TDD).
 *
 * The host scope mimics :feature:session's SessionGameScope: the first
 * onFinished wins and cancels hostScope, which is what stops both loops.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BlinkSniperGameTest {

    private class RecordingScope(override val hostScope: CoroutineScope) : GameScope {
        val scores = mutableListOf<Long>()
        val finishes = mutableListOf<Boolean>()

        override fun onScore(value: Long) {
            scores += value
        }

        override fun onFinished(win: Boolean) {
            finishes += win
            hostScope.cancel()
        }
    }

    /**
     * One spawn at t=0 with no jitter, so the single target reaches the
     * crosshair exactly 1.24s later (x: -0.12 -> 0.5 at 0.5 u/s).
     */
    private fun singleTargetConfig(bombChanceWarmup: Double = 0.0) = BlinkSniperConfig(
        startSpeed = 0.5,
        endSpeed = 0.5,
        speedJitterMin = 1.0,
        speedJitterMax = 1.0,
        startSpawnIntervalMs = 60_000,
        endSpawnIntervalMs = 60_000,
        centerLaneChance = 1.0,
        smallChance = 0.0,
        bombChanceWarmup = bombChanceWarmup,
    )

    @Test
    fun `the run ends with a win when the 60 second timer expires`() = runTest {
        val detector = FakeBlinkDetector()
        val game = BlinkSniperGame(detector, timeSource = { testScheduler.currentTime })
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        testScheduler.advanceTimeBy(60_100)
        testScheduler.runCurrent()

        assertEquals(listOf(true), scope.finishes)
        assertTrue(game.state.value.finished)
        assertEquals(EndReason.TIME_UP, game.state.value.endReason)
    }

    @Test
    fun `a blink fires a shot`() = runTest {
        val detector = FakeBlinkDetector()
        val game = BlinkSniperGame(detector, timeSource = { testScheduler.currentTime })
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        detector.emit(EyeState.OPEN)
        detector.emit(EyeState.CLOSED)
        testScheduler.runCurrent()

        assertEquals(1, game.state.value.shotsFired)
        assertEquals(0, game.state.value.score)
        assertEquals(emptyList<Boolean>(), scope.finishes)
    }

    @Test
    fun `a rapid second blink is blocked by the cooldown`() = runTest {
        val detector = FakeBlinkDetector()
        val game = BlinkSniperGame(detector, timeSource = { testScheduler.currentTime })
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        detector.emit(EyeState.CLOSED)
        testScheduler.runCurrent()
        detector.emit(EyeState.OPEN)
        testScheduler.runCurrent()
        detector.emit(EyeState.CLOSED)
        testScheduler.runCurrent()

        assertEquals(1, game.state.value.shotsFired)

        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()
        detector.emit(EyeState.OPEN)
        testScheduler.runCurrent()
        detector.emit(EyeState.CLOSED)
        testScheduler.runCurrent()

        assertEquals(2, game.state.value.shotsFired)
    }

    @Test
    fun `a scoring blink forwards the score to the host`() = runTest {
        val detector = FakeBlinkDetector()
        val game = BlinkSniperGame(
            detector,
            timeSource = { testScheduler.currentTime },
            engineFactory = { BlinkSniperEngine(singleTargetConfig(), Random(1)) },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        testScheduler.advanceTimeBy(1_240)
        detector.emit(EyeState.CLOSED)
        testScheduler.runCurrent()

        assertEquals(listOf(2L), scope.scores)
        assertEquals(2, game.state.value.score)
    }

    @Test
    fun `shooting a bomb finishes the run as a loss`() = runTest {
        val detector = FakeBlinkDetector()
        val game = BlinkSniperGame(
            detector,
            timeSource = { testScheduler.currentTime },
            engineFactory = {
                BlinkSniperEngine(singleTargetConfig(bombChanceWarmup = 1.0), Random(1))
            },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        testScheduler.advanceTimeBy(1_240)
        detector.emit(EyeState.CLOSED)
        testScheduler.runCurrent()

        assertEquals(listOf(false), scope.finishes)
        assertTrue(game.state.value.finished)
        assertEquals(EndReason.BOMB, game.state.value.endReason)
        assertEquals(0, game.state.value.score)
    }

    @Test
    fun `the score is reported only when it changes`() = runTest {
        val detector = FakeBlinkDetector()
        val game = BlinkSniperGame(
            detector,
            timeSource = { testScheduler.currentTime },
            engineFactory = { BlinkSniperEngine(singleTargetConfig(), Random(1)) },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        testScheduler.advanceTimeBy(1_240)
        detector.emit(EyeState.CLOSED)
        testScheduler.runCurrent()
        // A miss must not push the score again.
        detector.emit(EyeState.OPEN)
        testScheduler.runCurrent()
        detector.emit(EyeState.CLOSED)
        testScheduler.runCurrent()

        assertEquals(listOf(2L), scope.scores)
    }
}
