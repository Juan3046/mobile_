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
    fun `the banner tells a win from being left behind`() {
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
}
