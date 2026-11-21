package com.rmgames.feature.game.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Device playtests (docs/games/07-sound-barrier.md — Tuning). 2026-11-21 a: the ball reacted
 * to quiet room noise and shot across the screen. 2026-11-21 b: "much better", but the ball
 * should be a bit quicker than the scrolling corridor and the walls' hit boxes a bit smaller
 * than drawn.
 */
class SoundTuningTest {

    private val config = SoundBarrierConfig()

    @Test
    fun `quiet talk and room noise do not count as loud`() {
        assertFalse(isLoud(wasLoud = false, level = 0.05f, config = config))
        assertFalse(isLoud(wasLoud = false, level = 0.10f, config = config))
        assertTrue(isLoud(wasLoud = false, level = 0.20f, config = config))
    }

    @Test
    fun `a loud sound stays loud until it clearly drops (no flicker)`() {
        assertTrue(isLoud(wasLoud = true, level = 0.12f, config = config))
        assertFalse(isLoud(wasLoud = true, level = 0.06f, config = config))
    }

    @Test
    fun `the meter puts the loud threshold at its middle mark`() {
        assertEquals(0.5f, soundMeterFill(config.loudThreshold, config.loudThreshold), 1e-6f)
        assertTrue(soundMeterFill(0.05f, config.loudThreshold) < 0.5f)
    }

    @Test
    fun `the ball climbs quicker than the corridor scrolls, yet stays steerable`() {
        // Top climbing speed beats the fastest scroll (screen-heights vs widths per second).
        assertTrue(config.maxRiseSpeed > config.endSpeed)
        assertTrue(config.maxFallSpeed > config.endSpeed)
        val engine = SoundBarrierEngine(config, Random(1))
        engine.tick(0, soundDetected = false)
        val start = engine.ballY
        var t = 0L
        while (t < 1_000) { t += 16; engine.tick(t, soundDetected = true) }
        val moved = engine.ballY - start
        assertTrue("moved $moved", moved in 0.3..0.75)
    }

    @Test
    fun `one second of silence lets the ball drop without slamming`() {
        val engine = SoundBarrierEngine(config, Random(1))
        engine.tick(0, soundDetected = false)
        val start = engine.ballY
        var t = 0L
        while (t < 1_000) { t += 16; engine.tick(t, soundDetected = false) }
        assertTrue("moved ${start - engine.ballY}", abs(start - engine.ballY) <= 0.75)
    }

    // --- Wall hit boxes: a little smaller than drawn, on every side -----------------------

    private val r = 0.015
    private fun touches(ballX: Double, ballY: Double, inset: Double = config.barHitInset) =
        touchesBar(ballX, ballY, r, barLeft = 0.5, barWidth = 0.1, gapCenter = 0.5, gapHalf = 0.15, inset = inset)

    @Test
    fun `grazing the drawn edge of a wall does not count as touching it`() {
        // Ball overlapping the drawn upper bar by half the inset: drawn contact, no hit.
        val grazeY = 0.65 - r + config.barHitInset / 2
        assertTrue(touches(0.55, grazeY, inset = 0.0)) // with the old, drawn-size box
        assertFalse(touches(0.55, grazeY))
        // Same at the bar's leading face.
        val grazeX = 0.5 - r + config.barHitInset / 2
        assertTrue(touches(grazeX, 0.9, inset = 0.0))
        assertFalse(touches(grazeX, 0.9))
    }

    @Test
    fun `a ball clearly inside a wall still touches it`() {
        assertTrue(touches(0.55, 0.9))
        assertTrue(touches(0.55, 0.1))
        assertFalse(touches(0.55, 0.5)) // in the gap
        assertFalse(touches(0.3, 0.9)) // before the bar
    }

    // --- Decor ------------------------------------------------------------------------------

    @Test
    fun `clouds drift slowly left and wrap around`() {
        val cloud = Cloud(x = 0.2f, y = 0.3f, scale = 1f, speed = 0.02f)
        assertEquals(0.18f, cloudX(cloud, elapsedMs = 1_000), 1e-4f)
        val wrapped = cloudX(cloud, elapsedMs = 20_000)
        assertTrue(wrapped in CLOUD_MIN_X..CLOUD_MAX_X)
    }
}
