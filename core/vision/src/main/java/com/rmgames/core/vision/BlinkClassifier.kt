package com.rmgames.core.vision

/**
 * Pure blink logic (Strategy): turns ML Kit's per-eye open probabilities into an
 * [EyeState]. Kept free of any CameraX/ML Kit type so it runs on plain JUnit.
 *
 * For blink-sniper, a wink (one eye closed) counts as a trigger, so a CLOSED
 * result requires *both* eyes to be reported open.
 *
 * @param openThreshold probability at or above which an eye counts as open.
 */
class BlinkClassifier(private val openThreshold: Float) {

    fun classify(eyeOpenProbability: Float): EyeState =
        if (eyeOpenProbability >= openThreshold) EyeState.OPEN else EyeState.CLOSED

    /** Classifies from both eyes; missing values (face just detected) count as open. */
    fun classify(left: Float?, right: Float?): EyeState {
        val leftState = classify(left ?: DEFAULT_PROBABILITY_WHEN_MISSING)
        val rightState = classify(right ?: DEFAULT_PROBABILITY_WHEN_MISSING)
        // Both eyes must be open for the face to count as "eyes open".
        return if (leftState == EyeState.OPEN && rightState == EyeState.OPEN) {
            EyeState.OPEN
        } else {
            EyeState.CLOSED
        }
    }

    companion object {
        const val DEFAULT_OPEN_THRESHOLD = 0.5f
        private const val DEFAULT_PROBABILITY_WHEN_MISSING = 1f
    }
}
