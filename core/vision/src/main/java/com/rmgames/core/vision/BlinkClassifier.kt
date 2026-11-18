package com.rmgames.core.vision

/**
 * Pure blink logic (Strategy): turns ML Kit's per-eye open probabilities into an
 * [EyeState]. Kept free of any CameraX/ML Kit type so it runs on plain JUnit.
 *
 * A blink is "both eye probabilities drop below the detection threshold"
 * (docs/games/03-blink-sniper.md), so a face is CLOSED only while *both* eyes sit
 * below [openThreshold]. ML Kit reports the two eyes very unevenly off a webcam — an
 * eye can sit at 0.03 while the other reads 1.00 on a fully open face, for seconds
 * at a time — so the old "both eyes must be open" rule reported open faces as closed
 * and then swallowed the real blink that followed.
 *
 * @param openThreshold probability at or above which an eye counts as open.
 */
class BlinkClassifier(private val openThreshold: Float) {

    fun classify(eyeOpenProbability: Float): EyeState =
        if (eyeOpenProbability >= openThreshold) EyeState.OPEN else EyeState.CLOSED

    /**
     * Classifies from both eyes: open as soon as either one is at or above
     * [openThreshold]. Missing values (face just detected) count as open, so a
     * reappearing face cannot fake a CLOSED and swallow the next real blink.
     */
    fun classify(left: Float?, right: Float?): EyeState {
        val leftState = classify(left ?: DEFAULT_PROBABILITY_WHEN_MISSING)
        val rightState = classify(right ?: DEFAULT_PROBABILITY_WHEN_MISSING)
        return if (leftState == EyeState.OPEN || rightState == EyeState.OPEN) {
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
