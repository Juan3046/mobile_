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
    fun `one open eye keeps the face open`() {
        // Spec (docs/games/03-blink-sniper.md): "A blink is triggered when both eye
        // probabilities drop below the detection threshold". ML Kit reports the two
        // eyes very unevenly from a webcam, so an "either eye closed" rule marked
        // open faces as closed and then swallowed the real blink that followed.
        assertEquals(EyeState.OPEN, classifier.classify(left = 0.9f, right = 0.2f))
        assertEquals(EyeState.OPEN, classifier.classify(left = 0.03f, right = 0.95f))
    }

    @Test
    fun `the uneven reading seen on device keeps the face open`() {
        // Observed on the emulator webcam: a fully open face logged as 1.00 / 0.03
        // for 6.3s straight, which the old rule reported as CLOSED.
        assertEquals(EyeState.OPEN, classifier.classify(left = 1.0f, right = 0.03f))
        assertEquals(EyeState.OPEN, classifier.classify(left = 0.99f, right = 0.11f))
    }

    @Test
    fun `both eyes below the threshold is CLOSED`() {
        assertEquals(EyeState.CLOSED, classifier.classify(left = 0.4f, right = 0.3f))
        assertEquals(EyeState.CLOSED, classifier.classify(left = 0.06f, right = 0.45f))
    }

    @Test
    fun `an eye exactly at the threshold keeps the face open`() {
        assertEquals(EyeState.OPEN, classifier.classify(left = 0.5f, right = 0.1f))
        assertEquals(EyeState.OPEN, classifier.classify(left = 0.1f, right = 0.5f))
    }

    @Test
    fun `missing probabilities are treated as OPEN`() {
        // No probability reported (face just appeared) -> assume eyes open, so a face
        // reappearing mid-blink cannot fake a CLOSED and swallow the next real blink.
        assertEquals(EyeState.OPEN, classifier.classify(left = null, right = null))
        assertEquals(EyeState.OPEN, classifier.classify(left = null, right = 0.1f))
        assertEquals(EyeState.OPEN, classifier.classify(left = 0.1f, right = null))
    }
}
