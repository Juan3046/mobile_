package com.rmgames.feature.game.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Pure JVM tests for the Sound Barrier run logic (AGENTS.md §5 — TDD).
 * Everything here is time-injected and seeded, so the suite is deterministic.
 *
 * Spec: docs/games/06-sound-barrier.md — the track advances on its own, the ball
 * rises while sound is detected and falls while silent, a barrier scores +1 only
 * when the ball is inside the gap at the instant the barrier finishes passing it,
 * and missing that instant ends the run keeping the score.
 */
class SoundBarrierEngineTest {

    /**
     * Deterministic config: the gap sits exactly at the resting height of the ball
     * (0.5) and, unless overridden, gravity and thrust are off, so a test can reason
     * about barriers without having to pilot the ball first.
     *
     * [spawnX] is pushed far to the right in the pure-physics tests so the first
     * barrier cannot arrive and end the run while the ball is deliberately away
     * from the gap.
     */
    private fun testConfig(
        durationMs: Long = 60_000,
        startSpeed: Double = 0.34,
        endSpeed: Double = 0.34,
        spacing: Double = 0.55,
        startGapHalf: Double = 0.17,
        endGapHalf: Double = 0.17,
        gapCenterMin: Double = 0.5,
        gapCenterMax: Double = 0.5,
        gravity: Double = 0.0,
        thrust: Double = 0.0,
        maxRiseSpeed: Double = 1.4,
        spawnX: Double = 1.15,
    ) = SoundBarrierConfig(
        durationMs = durationMs,
        startSpeed = startSpeed,
        endSpeed = endSpeed,
        spacing = spacing,
        startGapHalf = startGapHalf,
        endGapHalf = endGapHalf,
        gapCenterMin = gapCenterMin,
        gapCenterMax = gapCenterMax,
        gravity = gravity,
        thrust = thrust,
        maxRiseSpeed = maxRiseSpeed,
        spawnX = spawnX,
    )

    private fun engine(config: SoundBarrierConfig = testConfig()) =
        SoundBarrierEngine(config, Random(seed = 42))

    /** Advances the clock with [stepMs] ticks; sound stays off unless told otherwise. */
    private fun runTo(
        engine: SoundBarrierEngine,
        untilMs: Long,
        soundDetected: Boolean = false,
        stepMs: Long = 50L,
    ) {
        var t = engine.elapsedMs
        while (t < untilMs) {
            t = minOf(t + stepMs, untilMs)
            engine.tick(t, soundDetected = soundDetected)
        }
    }

    // --- Spawning & movement -------------------------------------------------

    @Test
    fun `the first tick spawns a barrier just off the right edge`() {
        val engine = engine()

        engine.tick(0, soundDetected = false)

        val barrier = engine.snapshot().single()
        assertEquals(1, engine.spawnedCount)
        assertTrue("x=${barrier.x}", barrier.x > 1.0)
        assertEquals(0.5, barrier.gapCenter, 0.0)
        assertFalse(engine.isFinished)
    }

    @Test
    fun `barriers travel left across the screen`() {
        val engine = engine()
        engine.tick(0, soundDetected = false)
        val startX = engine.snapshot().single().x

        runTo(engine, 1_000)

        val movedX = engine.snapshot().first().x
        assertTrue("expected $movedX < $startX", movedX < startX)
    }

    @Test
    fun `barriers are spaced evenly along the track`() {
        val engine = engine(testConfig(spacing = 0.30))

        runTo(engine, 20_000)

        val xs = engine.snapshot().map { it.x }.sorted()
        assertTrue("expected several barriers, got ${xs.size}", xs.size >= 3)
        for (i in 1 until xs.size) {
            assertEquals(0.30, xs[i] - xs[i - 1], 1e-9)
        }
    }

    // --- Ball physics --------------------------------------------------------

    @Test
    fun `sound lifts the ball upward`() {
        val engine = engine(testConfig(thrust = 1.0, spawnX = 1_000.0))
        engine.tick(0, soundDetected = false)
        val startY = engine.ballY

        engine.tick(500, soundDetected = true)

        assertTrue("expected $engine.ballY > $startY", engine.ballY > startY)
    }

    @Test
    fun `silence lets the ball fall`() {
        val engine = engine(testConfig(gravity = 1.0, spawnX = 1_000.0))
        engine.tick(0, soundDetected = false)

        engine.tick(500, soundDetected = false)

        assertTrue("expected ${engine.ballY} < 0.5", engine.ballY < 0.5)
    }

    @Test
    fun `the floor and the ceiling stop the ball without ending the run`() {
        val config = testConfig(gravity = 9.0, thrust = 9.0, spawnX = 1_000.0)
        val engine = SoundBarrierEngine(config, Random(seed = 42))
        engine.tick(0, soundDetected = false)

        runTo(engine, 3_000)
        assertEquals(config.playerRadius, engine.ballY, 1e-9)
        assertFalse("hitting the floor must not kill", engine.isFinished)

        runTo(engine, 6_000, soundDetected = true)
        assertEquals(1.0 - config.playerRadius, engine.ballY, 1e-9)
        assertFalse("hitting the ceiling must not kill", engine.isFinished)
    }

    // --- Scoring -------------------------------------------------------------

    @Test
    fun `being inside the gap when the barrier passes scores one point`() {
        val engine = engine()
        engine.tick(0, soundDetected = false)

        // World travel to finish passing: spawnX - (playerX - barWidth) = 1.00
        // widths at 0.34 widths/s -> the first barrier clears at ~2.94s.
        runTo(engine, 4_000)

        assertEquals(1, engine.score)
        assertFalse(engine.isFinished)
    }

    @Test
    fun `a barrier passing with the ball outside the gap ends the run`() {
        // Gap down at 0.20, ball parked at 0.50 -> never inside.
        val engine = engine(testConfig(gapCenterMin = 0.20, gapCenterMax = 0.20))
        engine.tick(0, soundDetected = false)

        runTo(engine, 4_000)

        assertTrue(engine.isFinished)
        assertEquals(EndReason.LEFT_BEHIND, engine.endReason)
        assertEquals(0, engine.score)
    }

    @Test
    fun `overlapping a barrier does not end the run before it has finished passing`() {
        val engine = engine(testConfig(gapCenterMin = 0.20, gapCenterMax = 0.20))
        engine.tick(0, soundDetected = false)

        // Leading edge reaches the ball at 0.90 widths -> ~2.65s; the trailing edge
        // only clears it at 1.00 widths -> ~2.94s. In between the ball sits inside
        // the barrier and nothing may happen (spec: contact costs nothing).
        runTo(engine, 2_700)

        val barrier = engine.snapshot().first()
        assertTrue("barrier should have reached the ball", barrier.x <= 0.25)
        assertTrue("barrier should not have left yet", barrier.x + barrier.width > 0.25)
        assertEquals(0, engine.score)
        assertFalse("contact must not kill mid-sweep", engine.isFinished)

        runTo(engine, 4_000)
        assertTrue(engine.isFinished)
        assertEquals(EndReason.LEFT_BEHIND, engine.endReason)
    }

    @Test
    fun `leaving the gap before the barrier finishes passing forfeits the point`() {
        // The ball starts inside the gap and only climbs out while the barrier is
        // still sweeping past: the position at the end of the sweep is what counts.
        val engine = engine(testConfig(thrust = 50.0, maxRiseSpeed = 2.0))
        engine.tick(0, soundDetected = false)
        runTo(engine, 2_700)
        assertEquals(0.5, engine.ballY, 1e-9)
        assertEquals(0, engine.score)
        assertFalse(engine.isFinished)

        engine.tick(2_750, soundDetected = true)
        runTo(engine, 4_000, soundDetected = true)

        assertTrue(engine.isFinished)
        assertEquals(EndReason.LEFT_BEHIND, engine.endReason)
        assertEquals(0, engine.score)
    }

    // --- Difficulty ----------------------------------------------------------

    @Test
    fun `the track scrolls faster near the end of the run`() {
        val engine = engine(
            testConfig(spacing = 0.30, startSpeed = 0.20, endSpeed = 0.80),
        )
        engine.tick(0, soundDetected = false)

        runTo(engine, 10_000)
        val early = engine.spawnedCount
        runTo(engine, 50_000)
        val middle = engine.spawnedCount
        runTo(engine, 60_000)
        val late = engine.spawnedCount - middle

        assertTrue("early=$early late=$late", late > early)
    }

    @Test
    fun `gaps tighten as the run progresses`() {
        val engine = engine(
            testConfig(startGapHalf = 0.18, endGapHalf = 0.08, spacing = 0.30),
        )
        engine.tick(0, soundDetected = false)

        runTo(engine, 5_000)
        val early = engine.snapshot().map { it.gapHalf }
        runTo(engine, 55_000)
        val late = engine.snapshot().map { it.gapHalf }

        assertTrue("early=$early", early.isNotEmpty() && early.all { it > 0.15 })
        assertTrue("late=$late", late.isNotEmpty() && late.all { it < 0.10 })
    }

    // --- Run end -------------------------------------------------------------

    @Test
    fun `the timer ends the run as a win and keeps the score`() {
        val engine = engine(testConfig(durationMs = 10_000, spacing = 0.30))
        engine.tick(0, soundDetected = false)

        runTo(engine, 11_000)

        assertTrue(engine.isFinished)
        assertEquals(EndReason.TIME_UP, engine.endReason)
        assertTrue("score=${engine.score}", engine.score > 0)
    }

    @Test
    fun `ticking after the run ended freezes the track`() {
        val engine = engine(testConfig(spacing = 0.30))
        engine.tick(0, soundDetected = false)
        engine.tick(60_000, soundDetected = false)
        assertTrue(engine.isFinished)
        val frozen = engine.snapshot()
        val frozenScore = engine.score

        engine.tick(61_000, soundDetected = false)

        assertEquals(frozen, engine.snapshot())
        assertEquals(frozenScore, engine.score)
        assertEquals(60_000L, engine.elapsedMs)
        assertEquals(EndReason.TIME_UP, engine.endReason)
    }
}
