package com.rmgames.core.audio

import com.rmgames.core.common.ui.UiCue

/**
 * Every app-level sound, synthesized in code (plan 09 Decisions 1 & 6: no audio files,
 * no licences). Swap any entry for decoded audio later without touching callers.
 */
object SoundBook {

    fun cue(cue: UiCue): ShortArray = when (cue) {
        // Short, high blip: button press.
        UiCue.Click -> Synth.render(listOf(Note(midi(84), 28, Wave.SQUARE, 0.30)))
        // Countdown 3-2-1.
        UiCue.Tick -> Synth.render(listOf(Note(midi(81), 90, Wave.SQUARE, 0.40)))
        // Run starts: rising fifth.
        UiCue.Go -> Synth.render(listOf(Note(midi(81), 70, Wave.SQUARE, 0.40), Note(midi(88), 170, Wave.SQUARE, 0.45)))
        // New best: C-major arpeggio, square lead doubled by a triangle an octave down.
        UiCue.NewBest -> Synth.mix(
            listOf(
                Synth.render(listOf(72, 76, 79).map { Note(midi(it), 90, Wave.SQUARE, 0.30) } + Note(midi(84), 230, Wave.SQUARE, 0.32)),
                Synth.render(listOf(60, 64, 67).map { Note(midi(it), 90, Wave.TRIANGLE, 0.45) } + Note(midi(72), 230, Wave.TRIANGLE, 0.45)),
            ),
        )
    }

    /** Menu theme: 4 bars of eighth-note arpeggios over C – Am – F – G at 128 bpm (~7.5 s loop). */
    fun menuLoop(): ShortArray {
        val eighth = 60_000 / 128 / 2
        val melody = listOf(
            72, 76, 79, 76, 84, 79, 76, 79, // C
            72, 76, 81, 76, 84, 81, 76, 74, // Am
            72, 77, 81, 77, 84, 81, 77, 76, // F
            74, 79, 83, 79, 86, 83, 79, 74, // G
        )
        val roots = listOf(48, 45, 41, 43)
        val bass = roots.flatMap { r -> List(4) { listOf(r, r + 12) }.flatten() }
        return Synth.mix(
            listOf(
                Synth.render(melody.map { Note(midi(it), eighth, Wave.SQUARE, 0.16) }),
                Synth.render(bass.map { Note(midi(it), eighth, Wave.TRIANGLE, 0.38) }),
            ),
        )
    }
}
