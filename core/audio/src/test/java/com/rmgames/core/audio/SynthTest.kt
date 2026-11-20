package com.rmgames.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SynthTest {

    private val rate = Synth.SAMPLE_RATE

    @Test
    fun `length follows the note durations`() {
        val pcm = Synth.render(listOf(Note(440.0, 100), Note(660.0, 50)))
        assertEquals(rate * 150 / 1000, pcm.size)
    }

    @Test
    fun `a rest is silent`() {
        val pcm = Synth.render(listOf(Note.rest(20)))
        assertTrue(pcm.all { it == 0.toShort() })
    }

    @Test
    fun `every note starts and ends near silence so it never clicks`() {
        val pcm = Synth.render(listOf(Note(880.0, 80, Wave.SQUARE, volume = 1.0)))
        assertTrue("start ${pcm.first()}", abs(pcm.first().toInt()) < 400)
        assertTrue("end ${pcm.last()}", abs(pcm.last().toInt()) < 400)
    }

    @Test
    fun `mixing never clips past 16-bit range`() {
        val loud = List(4) { Note(220.0 * (it + 1), 200, Wave.SQUARE, volume = 1.0) }
        val pcm = Synth.mix(loud.map { Synth.render(listOf(it)) })
        assertTrue(pcm.all { it in Short.MIN_VALUE..Short.MAX_VALUE })
        assertTrue("should be audible", pcm.any { abs(it.toInt()) > 3000 })
    }

    @Test
    fun `mix pads shorter voices`() {
        val a = ShortArray(10) { 100 }
        val b = ShortArray(4) { 100 }
        assertEquals(10, Synth.mix(listOf(a, b)).size)
    }

    @Test
    fun `note helper maps MIDI numbers to equal temperament`() {
        assertEquals(440.0, midi(69), 0.001)
        assertEquals(261.626, midi(60), 0.01)
    }
}

class SoundBookTest {

    @Test
    fun `every cue has a short sound`() {
        com.rmgames.core.common.ui.UiCue.entries.forEach { cue ->
            val pcm = SoundBook.cue(cue)
            val ms = pcm.size * 1000 / Synth.SAMPLE_RATE
            assertTrue("$cue is $ms ms", ms in 20..900)
        }
    }

    @Test
    fun `menu loop is a few seconds and loops cleanly`() {
        val loop = SoundBook.menuLoop()
        val seconds = loop.size.toDouble() / Synth.SAMPLE_RATE
        assertTrue("loop is $seconds s", seconds in 6.0..20.0)
        assertTrue(abs(loop.first().toInt()) < 400 && abs(loop.last().toInt()) < 400)
    }

    @Test
    fun `sounds are deterministic`() {
        assertTrue(SoundBook.menuLoop().contentEquals(SoundBook.menuLoop()))
    }
}
