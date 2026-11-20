package com.rmgames.feature.game.rush

import com.rmgames.core.sensors.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class TiltHopDetectorTest {

    /**
     * Gravity as the accelerometer reads it for a phone held in portrait, leaned back by
     * [backDeg] (0 = upright, 90 = flat on a table) and rolled by [rollDeg] (+ = left edge down).
     */
    private fun held(backDeg: Double, rollDeg: Double = 0.0): Vector3 {
        val b = Math.toRadians(backDeg)
        val r = Math.toRadians(rollDeg)
        val g = 9.81
        return Vector3((g * sin(r)).toFloat(), (g * cos(r) * cos(b)).toFloat(), (g * cos(r) * sin(b)).toFloat())
    }

    /** Calibrated at 45° back, the usual way of holding a phone. */
    private fun calibrated(): TiltHopDetector = TiltHopDetector().apply {
        repeat(CALIBRATION_SAMPLES) { onSample(held(45.0), it * 20L) }
    }

    @Test
    fun `nothing hops while calibrating or near neutral`() {
        val d = TiltHopDetector()
        repeat(CALIBRATION_SAMPLES) { assertNull(d.onSample(held(80.0), it * 20L)) }
        assertNull(calibrated().onSample(held(55.0), 1_000))
    }

    @Test
    fun `tilting the top away hops forward, towards you hops back`() {
        assertEquals(Hop.FORWARD, calibrated().onSample(held(70.0), 1_000))
        assertEquals(Hop.BACK, calibrated().onSample(held(20.0), 1_000))
    }

    @Test
    fun `rolling left or right steps sideways`() {
        assertEquals(Hop.LEFT, calibrated().onSample(held(45.0, rollDeg = 25.0), 1_000))
        assertEquals(Hop.RIGHT, calibrated().onSample(held(45.0, rollDeg = -25.0), 1_000))
    }

    @Test
    fun `one tilt is one hop until it repeats or the phone comes back to neutral`() {
        val d = calibrated()
        assertEquals(Hop.FORWARD, d.onSample(held(70.0), 1_000))
        assertNull(d.onSample(held(70.0), 1_200))
        assertEquals(Hop.FORWARD, d.onSample(held(70.0), 1_000 + REPEAT_MS)) // held: repeats
        assertNull(d.onSample(held(46.0), 1_500)) // back to neutral re-arms…
        assertEquals(Hop.FORWARD, d.onSample(held(70.0), 1_520)) // …so the next tilt hops at once
    }

    @Test
    fun `the larger tilt wins when both directions move`() {
        assertEquals(Hop.LEFT, calibrated().onSample(held(65.0, rollDeg = 30.0), 1_000))
    }
}
