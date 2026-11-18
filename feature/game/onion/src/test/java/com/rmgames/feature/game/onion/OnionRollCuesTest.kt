package com.rmgames.feature.game.onion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** State changes → sound/vibration cues (docs/games/02-onion-roll.md — Audio & Haptics). */
class OnionRollCuesTest {

    private val start = OnionRollState.INITIAL

    private fun CueDetector.feed(vararg states: OnionRollState): List<Cue> =
        states.flatMap { next(it) }

    @Test
    fun `the first state only syncs and fires nothing`() {
        assertEquals(emptyList<Cue>(), CueDetector().next(start.copy(elapsedMs = 5_000, scoreSeconds = 5, hits = 3)))
    }

    @Test
    fun `a new whole second ticks`() {
        val d = CueDetector()
        d.next(start.copy(elapsedMs = 992, scoreSeconds = 0))

        assertEquals(listOf(Cue.TICK), d.next(start.copy(elapsedMs = 1_008, scoreSeconds = 1)))
        assertEquals(emptyList<Cue>(), d.next(start.copy(elapsedMs = 1_024, scoreSeconds = 1)))
    }

    @Test
    fun `a new hit fires a hit cue`() {
        val d = CueDetector()
        d.next(start.copy(elapsedMs = 500, hits = 0))

        assertEquals(listOf(Cue.HIT), d.next(start.copy(elapsedMs = 516, hits = 1)))
    }

    @Test
    fun `hits closer than the cooldown fire only once`() {
        val d = CueDetector()
        d.next(start.copy(elapsedMs = 500, hits = 0))

        val cues = d.feed(
            start.copy(elapsedMs = 516, hits = 1),
            start.copy(elapsedMs = 580, hits = 2), // 64 ms later: swallowed
            start.copy(elapsedMs = 660, hits = 3), // 144 ms after the first: fires
        )

        assertEquals(listOf(Cue.HIT, Cue.HIT), cues)
    }

    @Test
    fun `being shoved near the roller warns once until the onion recovers`() {
        val d = CueDetector()
        val near = ONION_Y + 0.7f * (END_OF_BELT_Y - ONION_Y)
        val safe = ONION_Y + 0.1f * (END_OF_BELT_Y - ONION_Y)
        d.next(start.copy(elapsedMs = 100))

        assertEquals(listOf(Cue.DANGER), d.next(start.copy(elapsedMs = 116, onionY = near)))
        assertEquals(emptyList<Cue>(), d.next(start.copy(elapsedMs = 132, onionY = near)))
        d.next(start.copy(elapsedMs = 148, onionY = safe)) // re-arms
        assertEquals(listOf(Cue.DANGER), d.next(start.copy(elapsedMs = 164, onionY = near)))
    }

    @Test
    fun `the fall fires once`() {
        val d = CueDetector()
        d.next(start.copy(elapsedMs = 9_984, scoreSeconds = 9))

        val fall = d.next(start.copy(elapsedMs = 10_000, scoreSeconds = 10, phase = Phase.FALLING))
        val later = d.feed(
            start.copy(elapsedMs = 10_016, scoreSeconds = 10, phase = Phase.FALLING),
            start.copy(elapsedMs = 12_000, scoreSeconds = 10, phase = Phase.DONE),
        )

        assertTrue("expected FALL, got $fall", Cue.FALL in fall)
        assertTrue("no tick once the run is over, got $fall", Cue.TICK !in fall)
        assertEquals(emptyList<Cue>(), later)
    }

    @Test
    fun `a restart re-syncs without firing cues`() {
        val d = CueDetector()
        d.next(start.copy(elapsedMs = 20_000, scoreSeconds = 20, hits = 5, phase = Phase.FALLING))

        assertEquals(emptyList<Cue>(), d.next(start.copy(elapsedMs = 16)))
        assertEquals(listOf(Cue.HIT), d.next(start.copy(elapsedMs = 32, hits = 1)))
    }

    @Test
    fun `hits and the fall vibrate, ticks and warnings do not`() {
        assertTrue(hapticMs(Cue.HIT)!! in 10L..60L)
        assertTrue(hapticMs(Cue.FALL)!! > hapticMs(Cue.HIT)!!)
        assertEquals(null, hapticMs(Cue.TICK))
        assertEquals(null, hapticMs(Cue.DANGER))
    }
}
