package com.rmgames.feature.game.onion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Presentation math of Onion Roll (docs/games/02-onion-roll.md): HUD label
 * format, the fall animation curves and the onion's face proportions. Keeping
 * these pure means polish passes can't silently regress them.
 */
class OnionRollVisualsTest {

    private val eps = 0.0001f

    @Test
    fun `elapsed label formats tenths of a second`() {
        assertEquals("0.0s", elapsedLabel(0))
        assertEquals("0.9s", elapsedLabel(999))
        assertEquals("12.3s", elapsedLabel(12_345))
        assertEquals("82.0s", elapsedLabel(82_000))
    }

    @Test
    fun `fall drop eases in like gravity`() {
        assertEquals(0f, fallDropFraction(0f), eps)
        assertEquals(1f, fallDropFraction(1f), eps)
        assertEquals(0.25f, fallDropFraction(0.5f), eps)
        assertTrue(fallDropFraction(0.3f) < fallDropFraction(0.6f))
    }

    @Test
    fun `fall tilt tumbles a fixed arc`() {
        assertEquals(0f, fallTiltDeg(0f), eps)
        assertEquals(240f, fallTiltDeg(1f), eps)
        assertEquals(60f, fallTiltDeg(0.5f), eps)
    }

    @Test
    fun `face layout is symmetric about the vertical axis`() {
        val face = faceLayout(radius = 100f)

        assertEquals(face.leftEyeX, -face.rightEyeX, eps)
        assertEquals(face.leftEyeY, face.rightEyeY, eps)
        assertEquals(0f, face.smileCx, eps)
    }

    @Test
    fun `eyes sit above the smile and inside the body`() {
        val radius = 100f
        val face = faceLayout(radius)

        assertTrue("eyes should be above centre", face.leftEyeY < 0f)
        assertTrue("smile should be below the eyes", face.smileCy > face.leftEyeY)
        assertTrue("smile must stay inside the body", face.smileCy + face.smileRadius < radius)
        assertTrue("eyes must stay inside the body", face.leftEyeY - face.eyeRadius > -radius)
    }

    @Test
    fun `face layout scales with the onion radius`() {
        val small = faceLayout(radius = 50f)
        val big = faceLayout(radius = 100f)

        assertEquals(small.leftEyeX * 2f, big.leftEyeX, 0.001f)
        assertEquals(small.smileRadius * 2f, big.smileRadius, 0.001f)
        assertEquals(small.eyeRadius * 2f, big.eyeRadius, 0.001f)
    }
}
