package com.rmgames.feature.game.onion

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Procedural sound effects (docs/games/02-onion-roll.md — Audio & Haptics). */
class OnionRollSoundsTest {

    private fun durationMs(samples: ShortArray) = samples.size * 1_000L / SOUND_SAMPLE_RATE

    private fun peak(samples: ShortArray) = samples.maxOf { abs(it.toInt()) }

    @Test
    fun `every cue has a sound`() {
        Cue.entries.forEach { cue -> assertTrue("$cue is empty", synthesize(cue).isNotEmpty()) }
    }

    @Test
    fun `sound lengths match the design`() {
        assertTrue(durationMs(synthesize(Cue.HIT)) in 110L..130L)
        assertTrue(durationMs(synthesize(Cue.TICK)) in 20L..30L)
        assertTrue(durationMs(synthesize(Cue.DANGER)) in 180L..260L)
        // The fall + jingle must finish inside the 2 s "YOU LOST!" banner.
        assertTrue(durationMs(synthesize(Cue.FALL)) in 1_200L..1_900L)
    }

    @Test
    fun `sounds never clip and start and end silent (no clicks)`() {
        Cue.entries.forEach { cue ->
            val s = synthesize(cue)
            assertTrue("$cue clips: ${peak(s)}", peak(s) < Short.MAX_VALUE * 0.95)
            assertTrue("$cue starts with a click", abs(s.first().toInt()) < 1_000)
            assertTrue("$cue ends with a click", abs(s.last().toInt()) < 1_000)
        }
    }

    @Test
    fun `the per-second tick is quieter than a hit`() {
        assertTrue(peak(synthesize(Cue.TICK)) < peak(synthesize(Cue.HIT)))
    }

    @Test
    fun `synthesis is deterministic`() {
        Cue.entries.forEach { cue -> assertArrayEquals(synthesize(cue), synthesize(cue)) }
    }
}
