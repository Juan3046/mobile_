package com.rmgames.feature.game.rush

import com.rmgames.core.sensors.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private var now = 0L

    /** Feeds [n] readings 20 ms apart (50 Hz) and returns every hop they produced. */
    private fun TiltHopDetector.hold(v: Vector3, n: Int = 15): List<Hop> = (0 until n).mapNotNull { now += 20; onSample(v, now) }

    /** Calibrated at 45° back, the usual way of holding a phone. */
    private fun calibrated(): TiltHopDetector = TiltHopDetector().apply { hold(held(45.0), CALIBRATION_SAMPLES + 5) }

    @Test
    fun `nothing hops while calibrating or near neutral`() {
        assertTrue(TiltHopDetector().hold(held(80.0), CALIBRATION_SAMPLES).isEmpty())
        assertTrue(calibrated().hold(held(52.0)).isEmpty())
    }

    @Test
    fun `tilting the top away hops forward, towards you hops back`() {
        assertEquals(listOf(Hop.FORWARD), calibrated().hold(held(70.0)))
        assertEquals(listOf(Hop.BACK), calibrated().hold(held(20.0)))
    }

    @Test
    fun `rolling left or right steps sideways`() {
        assertEquals(listOf(Hop.LEFT), calibrated().hold(held(45.0, rollDeg = 25.0)))
        assertEquals(listOf(Hop.RIGHT), calibrated().hold(held(45.0, rollDeg = -25.0)))
    }

    @Test
    fun `holding a tilt is one hop, coming back to neutral re-arms`() {
        val d = calibrated()
        assertEquals(listOf(Hop.FORWARD), d.hold(held(70.0), n = 60)) // held 1.2 s: still one hop
        assertTrue(d.hold(held(48.0)).isEmpty()) // back near neutral: re-armed, no "back" hop
        assertEquals(listOf(Hop.FORWARD), d.hold(held(70.0)))
    }

    @Test
    fun `a quick jolt of the hand is filtered out`() {
        val d = calibrated()
        // One spiky reading (the hand jerks) between calm ones does not hop.
        assertNull(d.onSample(Vector3(0f, 2f, 14f), 10_000))
        assertTrue(d.hold(held(45.0), 5).isEmpty())
    }

    @Test
    fun `diagonal tilts that are not clearly one direction are ignored`() {
        assertTrue(calibrated().hold(held(63.0, rollDeg = 18.0)).isEmpty())
        assertEquals(listOf(Hop.LEFT), calibrated().hold(held(52.0, rollDeg = 28.0)))
    }

    @Test
    fun `the current tilt is exposed for the on-screen indicator`() {
        val d = calibrated()
        d.hold(held(65.0))
        assertTrue(d.tilt.lean > 15f)
        assertTrue(kotlin.math.abs(d.tilt.roll) < 2f)
    }
}
