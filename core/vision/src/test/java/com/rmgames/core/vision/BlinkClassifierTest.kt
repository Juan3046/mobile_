package com.rmgames.core.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class BlinkClassifierTest {

    private val classifier = BlinkClassifier(openThreshold = 0.5f)

    @Test
    fun `probability above threshold is OPEN`() {
        assertEquals(EyeState.OPEN, classifier.classify(0.9f))
    }

    @Test
    fun `probability below threshold is CLOSED`() {
        assertEquals(EyeState.CLOSED, classifier.classify(0.1f))
    }

    @Test
    fun `probability at threshold is OPEN`() {
        assertEquals(EyeState.OPEN, classifier.classify(0.5f))
    }

    @Test
    fun `face with both eyes open is OPEN`() {
        assertEquals(EyeState.OPEN, classifier.classify(left = 0.9f, right = 0.8f))
    }

    @Test
    fun `one closed eye is CLOSED`() {
        // For blink-sniper, a wink counts as a trigger — either eye closed fires.
        assertEquals(EyeState.CLOSED, classifier.classify(left = 0.9f, right = 0.2f))
    }

    @Test
    fun `missing probabilities are treated as OPEN`() {
        // No probability reported (face just appeared) -> assume eyes open.
        assertEquals(EyeState.OPEN, classifier.classify(left = null, right = null))
    }
}
