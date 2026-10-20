package com.rmgames.core.sensors

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeDetectorTest {

    @Test
    fun `emits a shake when magnitude exceeds threshold`() = runTest {
        var now = 0L
        val detector = ShakeDetector(threshold = 2f, cooldownMs = 500, clock = { now })

        val shakes = detector.detect(flowOf(Vector3(0f, 0f, 0f), Vector3(3f, 0f, 0f))).toList()

        assertEquals(1, shakes.size)
    }

    @Test
    fun `ignores samples at or below threshold`() = runTest {
        val detector = ShakeDetector(threshold = 2f, cooldownMs = 500, clock = { 0L })

        val shakes = detector.detect(flowOf(Vector3(0f, 0f, 0f), Vector3(2f, 0f, 0f))).toList()

        assertTrue(shakes.isEmpty())
    }

    @Test
    fun `collapses consecutive shakes within cooldown into one`() = runTest {
        var now = 0L
        val detector = ShakeDetector(threshold = 2f, cooldownMs = 500, clock = { now })

        // Three spikes 100ms apart, all within the 500ms cooldown.
        val shakes = detector.detect(
            flow {
                emit(Vector3(5f, 0f, 0f))
                now += 100
                emit(Vector3(5f, 0f, 0f))
                now += 100
                emit(Vector3(5f, 0f, 0f))
            }
        ).toList()

        assertEquals(1, shakes.size)
    }

    @Test
    fun `emits again once cooldown has elapsed`() = runTest {
        var now = 0L
        val detector = ShakeDetector(threshold = 2f, cooldownMs = 500, clock = { now })

        val shakes = detector.detect(
            flow {
                emit(Vector3(5f, 0f, 0f))
                now += 600
                emit(Vector3(5f, 0f, 0f))
            }
        ).toList()

        assertEquals(2, shakes.size)
    }
}
