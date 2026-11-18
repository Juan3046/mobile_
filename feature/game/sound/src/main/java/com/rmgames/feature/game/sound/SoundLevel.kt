package com.rmgames.feature.game.sound

import kotlin.math.sqrt

/**
 * Turns one block of 16-bit PCM into the normalized 0..1 input level the game
 * compares against [SoundBarrierConfig.loudThreshold].
 *
 * Pure on purpose: the `AudioRecord` capture itself cannot run on the JVM, so this
 * is the one piece of level logic the unit tests can pin down (AGENTS.md §5 — TDD).
 */
object SoundLevel {

    /** Full-scale amplitude of a signed 16-bit PCM sample. */
    private const val FULL_SCALE = 32767.0

    /**
     * Root mean square of the first [count] samples of [samples], scaled to 0..1.
     * A short, empty or negative block reads as silence instead of dividing by zero.
     */
    fun of(samples: ShortArray, count: Int): Float {
        val limit = minOf(count, samples.size)
        if (limit <= 0) return 0f

        var sum = 0.0
        for (i in 0 until limit) {
            val normalized = samples[i] / FULL_SCALE
            sum += normalized * normalized
        }
        return sqrt(sum / limit).toFloat().coerceIn(0f, 1f)
    }
}
