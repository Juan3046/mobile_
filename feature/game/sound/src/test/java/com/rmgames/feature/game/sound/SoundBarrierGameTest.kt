package com.rmgames.feature.game.sound

import com.rmgames.core.common.GameScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the [SoundBarrierGame] wiring: mic level -> loud/quiet decision, virtual
 * clock -> run end, and score reporting to the host (AGENTS.md §5 — TDD).
 *
 * The host scope mimics :feature:session's SessionGameScope: the first onFinished
 * wins and cancels hostScope, which is what stops both loops.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SoundBarrierGameTest {

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

    @Test
    fun `the entry draws its own HUD so the host does not double-draw`() {
        // The screen renders SCORE/TIME itself; without this flag :feature:session
        // overlays its placeholder "score: N" and "Run finished ..." on top of it.
        assertTrue(soundBarrierEntry(FakeSoundSensor()).drawsOwnHud)
    }

    /**
     * Ball parked at the resting height (no gravity, no thrust) with every gap at
     * the same height, so a test only has to wait for the track to arrive.
     */
    private fun testConfig(
        durationMs: Long = 60_000,
        gapCenter: Double = 0.5,
        gravity: Double = 0.0,
        thrust: Double = 0.0,
        spawnX: Double = 1.15,
    ) = SoundBarrierConfig(
        durationMs = durationMs,
        gapCenterMin = gapCenter,
        gapCenterMax = gapCenter,
        gravity = gravity,
        thrust = thrust,
        spawnX = spawnX,
    )

    @Test
    fun `the game exposes the identity the host expects`() {
        val game = SoundBarrierGame(FakeSoundSensor())

        assertEquals("07-sound-barrier", game.id)
        assertEquals("Sound Barrier", game.title)
        assertEquals(60_000L, game.durationMs)
        assertTrue(game.higherWins)
        assertTrue(game.description.contains("60 seconds"))
    }

    @Test
    fun `the ball starts at the horizontal centre of the screen`() = runTest {
        val game = SoundBarrierGame(
            FakeSoundSensor(),
            config = testConfig(),
            timeSource = { testScheduler.currentTime },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        assertEquals(0.5, game.state.value.ballX, 0.0)
    }

    @Test
    fun `the run ends with a win when the 60 second timer expires`() = runTest {
        val game = SoundBarrierGame(
            FakeSoundSensor(),
            config = testConfig(),
            timeSource = { testScheduler.currentTime },
        )
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
    fun `missing a gap carries the ball off screen and ends the run as a loss`() = runTest {
        // Gap down at 0.20 while the ball rests at 0.50 -> the first barrier sweeps
        // across the ball's hitbox and carries it backwards until it leaves the
        // screen (~5.7s in at the default pace).
        val game = SoundBarrierGame(
            FakeSoundSensor(),
            config = testConfig(gapCenter = 0.20),
            timeSource = { testScheduler.currentTime },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        testScheduler.advanceTimeBy(6_200)
        testScheduler.runCurrent()

        assertEquals(listOf(false), scope.finishes)
        assertTrue(game.state.value.finished)
        assertEquals(EndReason.LEFT_BEHIND, game.state.value.endReason)
        assertEquals(0, game.state.value.score)
    }

    @Test
    fun `the UI state mirrors where the bars pushed the ball`() = runTest {
        // The screen must draw the ball where the engine pushed it, not at the
        // fixed centre it starts from.
        val game = SoundBarrierGame(
            FakeSoundSensor(),
            config = testConfig(gapCenter = 0.20),
            timeSource = { testScheduler.currentTime },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        // Mid-push (contact from ~3.3s, off screen at ~5.7s).
        testScheduler.advanceTimeBy(4_000)
        testScheduler.runCurrent()

        assertTrue("ballX=${game.state.value.ballX}", game.state.value.ballX < 0.5)
        assertFalse(game.state.value.finished)
    }

    @Test
    fun `a barrier passed through the gap forwards the score to the host`() = runTest {
        val game = SoundBarrierGame(
            FakeSoundSensor(),
            config = testConfig(),
            timeSource = { testScheduler.currentTime },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        // First barrier clears at ~3.75s; the next one is not due until ~6.5s.
        testScheduler.advanceTimeBy(4_000)
        testScheduler.runCurrent()

        assertEquals(listOf(1L), scope.scores)
        assertEquals(1, game.state.value.score)
    }

    @Test
    fun `levels below the loud threshold do not lift the ball`() = runTest {
        val config = testConfig(thrust = 4.0, spawnX = 1_000.0)
        val sensor = FakeSoundSensor(config.loudThreshold / 2f)
        val game = SoundBarrierGame(
            sensor,
            config = config,
            timeSource = { testScheduler.currentTime },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        testScheduler.advanceTimeBy(500)
        testScheduler.runCurrent()

        assertEquals(0.5, game.state.value.ballY, 1e-9)
        assertEquals(emptyList<Boolean>(), scope.finishes)
    }

    @Test
    fun `levels at the loud threshold lift the ball`() = runTest {
        val config = testConfig(thrust = 4.0, spawnX = 1_000.0)
        val sensor = FakeSoundSensor(config.loudThreshold)
        val game = SoundBarrierGame(
            sensor,
            config = config,
            timeSource = { testScheduler.currentTime },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        testScheduler.advanceTimeBy(500)
        testScheduler.runCurrent()

        assertTrue("ballY=${game.state.value.ballY}", game.state.value.ballY > 0.5)
        assertTrue(game.state.value.soundDetected)
    }

    @Test
    fun `the debug key counts as sound while the microphone stays quiet`() = runTest {
        val config = testConfig(thrust = 4.0, spawnX = 1_000.0)
        val game = SoundBarrierGame(
            FakeSoundSensor(),
            config = config,
            timeSource = { testScheduler.currentTime },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        game.setDebugSound(true)
        testScheduler.advanceTimeBy(500)
        testScheduler.runCurrent()

        assertTrue(game.state.value.soundDetected)
        assertTrue(game.state.value.ballY > 0.5)
    }

    @Test
    fun `the UI state mirrors the level and the track`() = runTest {
        val sensor = FakeSoundSensor()
        val game = SoundBarrierGame(
            sensor,
            config = testConfig(),
            timeSource = { testScheduler.currentTime },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        sensor.emit(0.42f)
        testScheduler.advanceTimeBy(50)
        testScheduler.runCurrent()

        assertEquals(0.42f, game.state.value.soundLevel, 1e-6f)
        assertTrue(game.state.value.barriers.isNotEmpty())
        assertTrue("elapsed=${game.state.value.elapsedMs}", game.state.value.elapsedMs >= 16)
    }

    @Test
    fun `stopping the game never finishes the run on its own`() = runTest {
        val game = SoundBarrierGame(
            FakeSoundSensor(),
            config = testConfig(),
            timeSource = { testScheduler.currentTime },
        )
        val scope = RecordingScope(backgroundScope)
        game.start(scope)
        testScheduler.runCurrent()

        game.stop()

        assertTrue(scope.finishes.isEmpty())
        assertTrue(scope.scores.isEmpty())
    }
}
