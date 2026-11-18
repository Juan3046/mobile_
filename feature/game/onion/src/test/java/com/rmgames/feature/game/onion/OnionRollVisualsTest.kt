package com.rmgames.feature.game.onion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Presentation math of Onion Roll (docs/games/02-onion-roll.md): the danger
 * glow, the fall animation curves and the onion's face proportions. Keeping
 * these pure means polish passes can't silently regress them.
 */
class OnionRollVisualsTest {

    private val eps = 0.0001f

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

    @Test
    fun `loss banner fades in at the start of the fall and then stays`() {
        assertEquals(0f, lossBannerAlpha(0L), eps)
        assertTrue(lossBannerAlpha(100L) in 0.1f..0.9f)
        assertEquals(1f, lossBannerAlpha(250L), eps)
        assertEquals(1f, lossBannerAlpha(2_000L), eps)
    }

    @Test
    fun `loss banner pops in slightly oversized then settles`() {
        assertTrue(lossBannerScale(0L) < 1f)
        assertTrue(lossBannerScale(200L) > 1f)
        assertEquals(1f, lossBannerScale(600L), eps)
    }

    @Test
    fun `danger level grows from the home row to the end roller`() {
        assertEquals(0f, dangerLevel(ONION_Y), eps)
        assertEquals(1f, dangerLevel(END_OF_BELT_Y), eps)
        assertEquals(0.5f, dangerLevel((ONION_Y + END_OF_BELT_Y) / 2f), eps)
        assertEquals(1f, dangerLevel(1.2f), eps) // clamped past the roller
    }

    @Test
    fun `danger glow is always faintly on and pulses`() {
        val samples = (0 until 40).map { dangerGlowAlpha(elapsedMs = it * 50L, danger = 0f) }

        assertTrue("never off: ${samples.min()}", samples.min() >= 0.16f)
        assertTrue("faint: ${samples.max()}", samples.max() <= 0.221f)
        assertTrue("pulses", samples.max() - samples.min() > 0.03f)
    }

    @Test
    fun `danger glow at the edge is always brighter than at home`() {
        val home = (0 until 40).maxOf { dangerGlowAlpha(it * 50L, danger = 0f) }
        val edge = (0 until 40).minOf { dangerGlowAlpha(it * 50L, danger = 1f) }

        assertTrue("home max=$home edge min=$edge", edge > home)
        assertTrue((0 until 40).all { dangerGlowAlpha(it * 50L, danger = 1f) <= 0.701f })
    }
}
