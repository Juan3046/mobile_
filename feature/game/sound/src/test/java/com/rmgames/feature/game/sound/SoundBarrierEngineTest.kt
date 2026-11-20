package com.rmgames.feature.game.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

/**
 * Pure JVM tests for the Sound Barrier run logic (AGENTS.md §5 — TDD).
 * Everything here is time-injected and seeded, so the suite is deterministic.
 *
 * Spec: docs/games/06-sound-barrier.md — the track advances on its own, the ball
 * rises while sound is detected and falls while silent, a barrier scores +1 only
 * when the ball is inside the gap at the instant the barrier finishes passing it,
 * a bar that touches the ball carries it backwards (left) and the run ends only
 * when the ball is pushed off the screen.
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
        bounceRestitution: Double = 0.8,
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
        bounceRestitution = bounceRestitution,
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

    /**
     * Ticks 10 ms at a time until [condition] holds. The cap turns "this never
     * happens" into a fast, readable failure instead of a suite that hangs (a rule
     * that stops being reachable must not be able to freeze the build).
     */
    private fun tickUntil(
        engine: SoundBarrierEngine,
        soundDetected: Boolean,
        maxTicks: Int = 10_000,
        condition: () -> Boolean,
    ) {
        var ticks = 0
        while (!condition()) {
            if (++ticks > maxTicks) {
                fail("condition never met after $maxTicks ticks (ballY=${engine.ballY}, v=${engine.ballVelocity})")
            }
            engine.tick(engine.elapsedMs + 10, soundDetected = soundDetected)
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

        // The ball sits in the middle now, so at most three barriers are alive at
        // once (the visible window is spawnX - playerX + barWidth = 0.75 widths).
        // 15.1s is right after a spawn: worldX 5.13 holds 4.517 / 4.817 / 5.117.
        runTo(engine, 15_100)

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
    fun `the floor bounces the ball back up instead of pinning it`() {
        val config = testConfig(gravity = 9.0, thrust = 0.0, spawnX = 1_000.0)
        val engine = SoundBarrierEngine(config, Random(seed = 42))
        engine.tick(0, soundDetected = false)

        // Fall until the ball actually touches the floor.
        tickUntil(engine, soundDetected = false) { engine.ballY <= config.playerRadius + 1e-9 }
        val touching = engine.ballY
        engine.tick(engine.elapsedMs + 10, soundDetected = false)

        assertTrue("expected to leave the floor, y=${engine.ballY}", engine.ballY > touching)
        assertTrue("expected an upward bounce, v=${engine.ballVelocity}", engine.ballVelocity > 0.0)
        assertFalse("bouncing off the floor must not kill", engine.isFinished)
        assertTrue(engine.ballY <= 1.0 - config.playerRadius)
    }

    @Test
    fun `the ceiling bounces the ball back down instead of pinning it`() {
        val config = testConfig(gravity = 0.0, thrust = 9.0, spawnX = 1_000.0)
        val engine = SoundBarrierEngine(config, Random(seed = 42))
        engine.tick(0, soundDetected = false)

        // Rise until the ball actually touches the ceiling (sound is what lifts it).
        tickUntil(engine, soundDetected = true) { engine.ballY >= 1.0 - config.playerRadius - 1e-9 }
        val touching = engine.ballY
        engine.tick(engine.elapsedMs + 10, soundDetected = true)

        assertTrue("expected to leave the ceiling, y=${engine.ballY}", engine.ballY < touching)
        assertTrue("expected a downward bounce, v=${engine.ballVelocity}", engine.ballVelocity < 0.0)
        assertFalse("bouncing off the ceiling must not kill", engine.isFinished)
        assertTrue(engine.ballY >= config.playerRadius)
    }

    @Test
    fun `a bounce keeps the ball inside the corridor at any strength`() {
        // A perfectly elastic bounce must still not let the ball escape the box.
        val config = testConfig(
            gravity = 9.0,
            thrust = 9.0,
            spawnX = 1_000.0,
            bounceRestitution = 1.0,
        )
        val engine = SoundBarrierEngine(config, Random(seed = 42))
        engine.tick(0, soundDetected = false)

        runTo(engine, 10_000, soundDetected = false)

        val floor = config.playerRadius
        val ceiling = 1.0 - config.playerRadius
        assertTrue("y=${engine.ballY}", engine.ballY >= floor && engine.ballY <= ceiling)
        assertFalse(engine.isFinished)
    }

    // --- Scoring -------------------------------------------------------------

    @Test
    fun `being inside the gap when the barrier passes scores one point`() {
        val engine = engine()
        engine.tick(0, soundDetected = false)

        // World travel to finish passing: spawnX - (playerX - barWidth) = 0.75
        // widths at 0.34 widths/s -> the first barrier clears at ~2.21s, while
        // the next one is not due until ~3.82s.
        runTo(engine, 3_000)

        assertEquals(1, engine.score)
        assertFalse(engine.isFinished)
    }

    @Test
    fun `touching a bar pushes the ball backwards instead of ending the run`() {
        // Gap down at 0.20, ball parked at 0.50 -> the bar sweeps straight across
        // the ball's hitbox. Contact must carry the ball backwards (left), never
        // kill it on the spot (docs/games/06-sound-barrier.md — Contact).
        val engine = engine(testConfig(gapCenterMin = 0.20, gapCenterMax = 0.20))
        engine.tick(0, soundDetected = false)

        runTo(engine, 2_600)

        assertFalse("contact must not end the run", engine.isFinished)
        assertTrue("expected the ball pushed left, x=${engine.ballX}", engine.ballX < 0.5)
        assertEquals(0, engine.score)
    }

    @Test
    fun `climbing out of the gap while the barrier sweeps pushes the ball back`() {
        // The ball starts inside the gap and pushes its way into the upper bar
        // while the barrier is still crossing it: the leading face carries it
        // backwards instead of ending the run.
        val engine = engine(testConfig(thrust = 50.0, maxRiseSpeed = 2.0))
        engine.tick(0, soundDetected = false)
        runTo(engine, 1_900)
        assertEquals(0.5, engine.ballY, 1e-9)
        assertFalse("still sweeping, still alive", engine.isFinished)

        runTo(engine, 2_400, soundDetected = true)

        assertFalse("contact must not end the run", engine.isFinished)
        assertTrue("expected the ball pushed left, x=${engine.ballX}", engine.ballX < 0.5)
        assertEquals(0, engine.score)
    }

    @Test
    fun `being carried off the left edge ends the run keeping the score`() {
        // Never aligned: the bar keeps carrying the ball backwards until the whole
        // ball leaves the screen, which is the only contact-driven way to lose.
        val engine = engine(testConfig(gapCenterMin = 0.20, gapCenterMax = 0.20))
        engine.tick(0, soundDetected = false)

        runTo(engine, 5_000)

        assertTrue("the run must end once the ball is off screen", engine.isFinished)
        assertEquals(EndReason.LEFT_BEHIND, engine.endReason)
        assertEquals(0, engine.score)
    }

    // --- Difficulty ----------------------------------------------------------

    @Test
    fun `the first barrier takes about three seconds to arrive`() {
        // Onboarding pace (playtest feedback): at the old0.34 widths/s the first
        // sweep reached the ball at ~1.9s, too fast to even hear the mic.
        val config = SoundBarrierConfig()
        val secondsToBall = (config.spawnX - config.playerX) / config.startSpeed

        assertTrue(
            "first barrier reaches the ball at ${secondsToBall}s",
            secondsToBall >= 3.0,
        )
    }

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
