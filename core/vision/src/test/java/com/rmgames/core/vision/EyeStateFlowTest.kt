package com.rmgames.core.vision

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression test for issue #22: the CameraX `unbindAll()` / `bindToLifecycle()`
 * calls used to run on `Dispatchers.Default` and crashed the app with
 * `IllegalStateException: Not in application's main thread`.
 *
 * The rig records the thread every CameraX operation ran on: they must all land
 * on the main dispatcher the flow is built with, never on a `Default` worker.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EyeStateFlowTest {

    /** Fake rig: records the thread of every operation and emits one state. */
    private class RecordingRig : CameraRig {
        val threads = mutableListOf<Thread>()

        override suspend fun bind(emit: (EyeState) -> Unit) {
            threads += Thread.currentThread()
            emit(EyeState.OPEN)
        }

        override fun unbind() {
            threads += Thread.currentThread()
        }

        override fun close() {
            threads += Thread.currentThread()
        }
    }

    @Test
    fun `camera bind and unbind run on the main dispatcher`() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        val rig = RecordingRig()
        val testThread = Thread.currentThread()

        // Receiving one state cancels the flow, which runs the awaitClose unbind/close.
        eyeStateFlow(main) { rig }.first()
        advanceUntilIdle()

        assertEquals(
            "every CameraX call must run on the main dispatcher",
            listOf(testThread, testThread, testThread),
            rig.threads,
        )
    }

    @Test
    fun `eye states emitted by the rig reach the collector`() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        val rig = RecordingRig()

        val seen = eyeStateFlow(main) { rig }.take(1).toList()

        assertEquals(listOf(EyeState.OPEN), seen)
    }
}
