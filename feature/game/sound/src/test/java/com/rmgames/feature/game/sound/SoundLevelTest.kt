package com.rmgames.feature.game.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the microphone level maths (AGENTS.md §5 — TDD).
 *
 * The `AudioRecord` capture itself cannot run on the JVM, so the one piece of
 * real logic — turning a block of 16-bit PCM into the 0..1 level the game
 * thresholds — is kept pure and pinned down here.
 */
class SoundLevelTest {

    @Test
    fun `silence maps to zero`() {
        assertEquals(0f, SoundLevel.of(ShortArray(64), 64), 1e-6f)
    }

    @Test
    fun `an empty block maps to zero instead of dividing by zero`() {
        assertEquals(0f, SoundLevel.of(ShortArray(0), 0), 1e-6f)
        assertEquals(0f, SoundLevel.of(ShortArray(8), -1), 1e-6f)
    }

    @Test
    fun `a full scale block reads as the maximum level`() {
        val samples = ShortArray(32) { Short.MAX_VALUE }

        assertEquals(1f, SoundLevel.of(samples, 32), 1e-4f)
    }

    @Test
    fun `the level is the root mean square of the block`() {
        val samples = ShortArray(16) { 16_384.toShort() }

        // 16384 / 32767 is about 0.5, so the RMS of a flat block is about 0.5.
        assertEquals(0.5, SoundLevel.of(samples, 16).toDouble(), 0.01)
    }

    @Test
    fun `a louder block reads higher than a quieter one`() {
        val quiet = ShortArray(32) { 500.toShort() }
        val loud = ShortArray(32) { 9_000.toShort() }

        assertTrue(SoundLevel.of(loud, 32) > SoundLevel.of(quiet, 32))
    }

    @Test
    fun `only the first count samples are read`() {
        val samples = shortArrayOf(Short.MAX_VALUE, Short.MAX_VALUE, 0, 0, 0, 0)

        assertEquals(1f, SoundLevel.of(samples, 2), 1e-3f)
        assertEquals(0f, SoundLevel.of(samples, 0), 1e-6f)
    }

    @Test
    fun `a quiet ambience stays below the loud threshold the game uses`() {
        val room = ShortArray(64) { 400.toShort() }

        assertTrue(
            "room level=${SoundLevel.of(room, 64)} threshold=${SoundBarrierConfig().loudThreshold}",
            SoundLevel.of(room, 64) < SoundBarrierConfig().loudThreshold,
        )
    }
}
