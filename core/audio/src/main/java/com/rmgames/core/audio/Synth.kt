package com.rmgames.core.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

enum class Wave { SQUARE, TRIANGLE, SINE }

/** One note: frequency (0 = rest), length, waveform and loudness 0..1. */
data class Note(val freqHz: Double, val durationMs: Int, val wave: Wave = Wave.SQUARE, val volume: Double = 0.5) {
    companion object {
        fun rest(durationMs: Int) = Note(0.0, durationMs, volume = 0.0)
    }
}

/** Equal-temperament frequency of a MIDI note number (69 = A4 = 440 Hz). */
fun midi(note: Int): Double = 440.0 * 2.0.pow((note - 69) / 12.0)

/**
 * Tiny chiptune synthesizer: renders notes to 16-bit mono PCM. Pure Kotlin, so every
 * sound is unit-tested and deterministic. Each note has a short attack and release, so
 * it starts and ends at silence and never clicks.
 */
object Synth {
    const val SAMPLE_RATE = 22_050
    private const val ATTACK_MS = 4
    private const val RELEASE_MS = 14
    private const val PEAK = 0.6 * Short.MAX_VALUE

    fun render(notes: List<Note>): ShortArray {
        val out = ShortArray(notes.sumOf { samplesFor(it.durationMs) })
        var offset = 0
        notes.forEach { n ->
            val count = samplesFor(n.durationMs)
            if (n.freqHz > 0 && n.volume > 0) {
                val attack = samplesFor(ATTACK_MS).coerceAtMost(count / 2).coerceAtLeast(1)
                val release = samplesFor(RELEASE_MS).coerceAtMost(count / 2).coerceAtLeast(1)
                for (i in 0 until count) {
                    val env = when {
                        i < attack -> i.toDouble() / attack
                        i >= count - release -> (count - 1 - i).toDouble() / release
                        else -> 1.0
                    }
                    val phase = (n.freqHz * i / SAMPLE_RATE) % 1.0
                    out[offset + i] = (wave(n.wave, phase) * env * n.volume * PEAK).toInt().toShort()
                }
            }
            offset += count
        }
        return out
    }

    /** Sums voices (shorter ones padded with silence) and clamps to 16 bits. */
    fun mix(voices: List<ShortArray>): ShortArray {
        val length = voices.maxOfOrNull { it.size } ?: 0
        return ShortArray(length) { i ->
            voices.sumOf { v -> if (i < v.size) v[i].toInt() else 0 }
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun samplesFor(ms: Int) = SAMPLE_RATE * ms / 1000

    private fun wave(w: Wave, phase: Double): Double = when (w) {
        Wave.SQUARE -> if (phase < 0.5) 1.0 else -1.0
        Wave.TRIANGLE -> 1.0 - 4.0 * abs(phase - 0.5)
        Wave.SINE -> sin(2 * PI * phase)
    }
}
