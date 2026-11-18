package com.rmgames.feature.game.onion

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural sound effects for Onion Roll (docs/games/02-onion-roll.md — Audio &
 * Haptics): 16-bit mono PCM built from sines and envelopes, so the game ships no
 * audio files. Pure and deterministic — OnionRollSoundsTest checks lengths,
 * loudness and click-free edges.
 */
internal const val SOUND_SAMPLE_RATE = 22_050

internal fun synthesize(cue: Cue): ShortArray = when (cue) {
    Cue.HIT -> toPcm(hit())
    Cue.TICK -> toPcm(tick())
    Cue.DANGER -> toPcm(beep() + silence(60) + beep())
    Cue.FALL -> toPcm(fallWhistle() + silence(100) + sadJingle())
}

/** Low "plof": a pitch-dropping thump with a short noisy attack. */
private fun hit(): FloatArray {
    val n = samples(120)
    val noise = Random(7)
    var phase = 0.0
    return FloatArray(n) { i ->
        val t = i.toDouble() / SOUND_SAMPLE_RATE
        val freq = 180.0 * (70.0 / 180.0).pow(t / 0.12)
        phase += 2 * PI * freq / SOUND_SAMPLE_RATE
        val body = sin(phase) * exp(-t / 0.04)
        val attack = (noise.nextFloat() * 2 - 1) * exp(-t / 0.006) * 0.35
        ((body + attack) * 0.8).toFloat()
    }
}

/** Soft high tick for each new second. */
private fun tick(): FloatArray = FloatArray(samples(25)) { i ->
    val t = i.toDouble() / SOUND_SAMPLE_RATE
    (sin(2 * PI * 1_500 * t) * exp(-t / 0.006) * 0.25).toFloat()
}

/** One alarm beep (880 Hz with a touch of 3rd harmonic). */
private fun beep(): FloatArray = note(880.0, 80, amplitude = 0.45)

/** Falling whistle as the onion drops off the roller. */
private fun fallWhistle(): FloatArray {
    val n = samples(450)
    var phase = 0.0
    return FloatArray(n) { i ->
        val p = i.toDouble() / n
        val freq = 700.0 * (150.0 / 700.0).pow(p)
        phase += 2 * PI * freq / SOUND_SAMPLE_RATE
        (sin(phase) * 0.5 * (1 - p * 0.6)).toFloat()
    }
}

/** Short descending "you lost" jingle; the last note wobbles. */
private fun sadJingle(): FloatArray =
    note(392.0, 160) + silence(20) + note(370.0, 160) + silence(20) +
        note(349.0, 160) + silence(20) + note(330.0, 500, vibrato = true)

private fun note(freq: Double, ms: Int, amplitude: Double = 0.5, vibrato: Boolean = false): FloatArray {
    val n = samples(ms)
    var phase = 0.0
    val out = FloatArray(n) { i ->
        val t = i.toDouble() / SOUND_SAMPLE_RATE
        val f = if (vibrato) freq * (1 + 0.015 * sin(2 * PI * 6 * t)) else freq
        phase += 2 * PI * f / SOUND_SAMPLE_RATE
        ((sin(phase) + 0.25 * sin(3 * phase)) / 1.25 * amplitude).toFloat()
    }
    return fadeEdges(out, fadeMs = 8)
}

private fun silence(ms: Int) = FloatArray(samples(ms))

private fun samples(ms: Int) = SOUND_SAMPLE_RATE * ms / 1_000

/** Linear fade in/out so no sound starts or stops with a click. */
private fun fadeEdges(x: FloatArray, fadeMs: Int): FloatArray {
    val fade = minOf(samples(fadeMs), x.size / 2)
    for (i in 0 until fade) {
        val g = i.toFloat() / fade
        x[i] *= g
        x[x.size - 1 - i] *= g
    }
    return x
}

private fun toPcm(x: FloatArray): ShortArray {
    fadeEdges(x, fadeMs = 4)
    return ShortArray(x.size) { i -> (x[i].coerceIn(-1f, 1f) * PCM_HEADROOM * Short.MAX_VALUE).toInt().toShort() }
}

private const val PCM_HEADROOM = 0.9f
