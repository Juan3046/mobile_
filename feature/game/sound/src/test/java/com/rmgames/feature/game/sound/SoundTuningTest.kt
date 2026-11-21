package com.rmgames.feature.game.sound

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Playtest 2026-11-21 ("extremely bad"): the ball reacted to quiet room noise and
 * shot across the screen. Only a clearly loud input may lift it, and it must move
 * calmly enough to steer (docs/games/07-sound-barrier.md — Tuning).
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
        assertTrue(kotlin.math.abs(soundMeterFill(config.loudThreshold, config.loudThreshold) - 0.5f) < 1e-6f)
        assertTrue(soundMeterFill(0.05f, config.loudThreshold) < 0.5f)
    }

    @Test
    fun `one second of steady sound moves the ball less than half the screen`() {
        val engine = SoundBarrierEngine(config, Random(1))
        engine.tick(0, soundDetected = false)
        val start = engine.ballY
        var t = 0L
        while (t < 1_000) { t += 16; engine.tick(t, soundDetected = true) }
        assertTrue("moved ${engine.ballY - start}", engine.ballY - start in 0.05..0.5)
    }

    @Test
    fun `one second of silence lets the ball drop gently, not slam`() {
        val engine = SoundBarrierEngine(config, Random(1))
        engine.tick(0, soundDetected = false)
        val start = engine.ballY
        var t = 0L
        while (t < 1_000) { t += 16; engine.tick(t, soundDetected = false) }
        assertTrue("moved ${start - engine.ballY}", abs(start - engine.ballY) <= 0.5)
    }
}
