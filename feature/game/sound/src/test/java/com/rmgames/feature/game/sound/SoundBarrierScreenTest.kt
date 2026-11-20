package com.rmgames.feature.game.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the Sound Barrier screen helpers (AGENTS.md §5 — TDD).
 *
 * The corridor itself is drawn procedurally, so what can be pinned down here is the
 * small amount of logic behind it: the countdown formatting, the end-of-run copy and
 * the sound meter fill (docs/games/06-sound-barrier.md).
 */
class SoundBarrierScreenTest {

    @Test
    fun `the countdown shows tenths of a second`() {
        assertEquals("42.5", formatTenths(42_500))
        assertEquals("60.0", formatTenths(60_000))
        assertEquals("0.0", formatTenths(0))
    }

    @Test
    fun `the countdown rounds rather than truncating`() {
        assertEquals("1.0", formatTenths(990))
        assertEquals("0.4", formatTenths(360))
    }

    @Test
    fun `the banner tells a win from being pushed off screen`() {
        assertEquals("TIME'S UP!", endBanner(EndReason.TIME_UP))
        assertEquals("LEFT BEHIND!", endBanner(EndReason.LEFT_BEHIND))
        assertEquals("", endBanner(null))
    }

    @Test
    fun `the sound meter never renders outside its track`() {
        assertEquals(0f, soundMeterFill(-1f), 1e-6f)
        assertEquals(1f, soundMeterFill(4f), 1e-6f)
        assertEquals(0.42f, soundMeterFill(0.42f), 1e-6f)
        assertTrue(soundMeterFill(0.6f) in 0f..1f)
    }

    @Test
    fun `a bar is rounded but never more than its rim can hold`() {
        // The gap rim is drawn with the same radius as the bar it sits on, so the
        // rounding of the bar must stay inside the rim strip, and a thin bar must
        // never ask for a radius wider than itself.
        for (barWidth in listOf(2f, 12f, 60f, 134f, 900f)) {
            val radius = barCornerRadius(barWidth)
            assertTrue("radius=$radius <= half of $barWidth", radius <= barWidth / 2f)
            assertTrue("rim=$GAP_RIM must hold 2 x radius=$radius", GAP_RIM >= 2f * radius)
        }
    }

    @Test
    fun `the floor is drawn at the bottom of the screen and the ceiling at the top`() {
        val height = 1000f
        // Gravity pulls the ball to physics 0, which must read as the bottom edge.
        assertEquals(height, screenY(0.0, height), 1e-4f)
        assertEquals(0f, screenY(1.0, height), 1e-4f)
        assertEquals(height / 2f, screenY(0.5, height), 1e-4f)
    }

    @Test
    fun `a barrier opening is left open rather than covered by its bars`() {
        val height = 1000f
        val gapCenter = 0.5
        val gapHalf = 0.17
        val top = screenY(gapCenter + gapHalf, height)
        val bottom = screenY(gapCenter - gapHalf, height)
        assertTrue("the opening must run top-down on screen", bottom > top)
        assertEquals((2 * gapHalf * height).toFloat(), bottom - top, 1e-3f)
    }
}
