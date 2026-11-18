package com.rmgames.core.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeadGestureClassifierTest {
    @Test fun `classifies nod and requires neutral before another gesture`() {
        val classifier = HeadGestureClassifier()
        assertNull(classifier.classify(0f, 0f, 0))
        assertEquals(HeadGesture.NOD, classifier.classify(20f, 1f, 100))
        assertNull(classifier.classify(25f, 1f, 700))
        assertNull(classifier.classify(2f, 1f, 800))
        assertEquals(HeadGesture.NOD, classifier.classify(-20f, 1f, 900))
    }

    @Test fun `classifies shake and suppresses noise and cooldown repeats`() {
        val classifier = HeadGestureClassifier()
        classifier.classify(0f, 0f, 0)
        assertNull(classifier.classify(4f, 11f, 100))
        assertEquals(HeadGesture.SHAKE, classifier.classify(2f, 20f, 200))
        assertNull(classifier.classify(0f, 0f, 300))
        assertNull(classifier.classify(1f, -21f, 400))
        assertNull(classifier.classify(0f, 0f, 700))
        assertEquals(HeadGesture.SHAKE, classifier.classify(0f, -20f, 800))
    }

    @Test fun `ignores invalid pose samples`() {
        val classifier = HeadGestureClassifier()
        classifier.classify(0f, 0f, 0)
        assertNull(classifier.classify(Float.NaN, 0f, 100))
        assertNull(classifier.classify(0f, Float.POSITIVE_INFINITY, 100))
    }

    @Test fun `reset calibrates a newly reacquired face`() {
        val classifier = HeadGestureClassifier()
        classifier.classify(0f, 0f, 0)
        assertEquals(HeadGesture.NOD, classifier.classify(20f, 0f, 100))
        classifier.reset()
        assertNull(classifier.classify(35f, -12f, 200))
        assertEquals(HeadGesture.SHAKE, classifier.classify(35f, 8f, 800))
    }
}
