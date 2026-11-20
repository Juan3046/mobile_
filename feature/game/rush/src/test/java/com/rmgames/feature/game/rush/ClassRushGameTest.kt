package com.rmgames.feature.game.rush

import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.AccelerometerSensor
import com.rmgames.core.sensors.FakeAccelerometerSensor
import com.rmgames.core.sensors.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/** Wiring of [ClassRushGame] to the host contract (docs/08), on virtual time. */
@OptIn(ExperimentalCoroutinesApi::class)
class ClassRushGameTest {

    private class RecordingHost(parent: CoroutineScope) : GameScope {
        private val job = SupervisorJob(parent.coroutineContext[Job])
        override val hostScope = CoroutineScope(parent.coroutineContext + job)
        val scores = mutableListOf<Long>()
        val finishes = mutableListOf<Boolean>()
        override fun onScore(value: Long) { scores += value }
        override fun onFinished(win: Boolean) { finishes += win }
        fun close() = hostScope.cancel()
    }

    private fun held(backDeg: Double): Vector3 {
        val b = Math.toRadians(backDeg)
        return Vector3(0f, (9.81 * cos(b)).toFloat(), (9.81 * sin(b)).toFloat())
    }

    @Test
    fun `tilting forward hops, the rows are reported and the clock ends the run`() = runTest {
        val sensor = FakeAccelerometerSensor()
        val host = RecordingHost(this)
        val game = ClassRushGame(sensor, newSeed = { 1L })
        game.start(host)
        runCurrent()
        repeat(CALIBRATION_SAMPLES) { sensor.emit(held(45.0)); advanceTimeBy(20) }
        // Three forward hops through the safe start rows.
        repeat(3) {
            repeat(12) { sensor.emit(held(70.0)); advanceTimeBy(20) } // tilt and hold briefly
            repeat(12) { sensor.emit(held(45.0)); advanceTimeBy(20) } // back to neutral
        }
        assertEquals(3, game.state.value.row)
        assertTrue(host.scores.contains(3L))
        advanceTimeBy(RUN_MS) // idle or the clock ends it
        assertEquals(1, host.finishes.size)
        assertEquals(RushPhase.OVER, game.state.value.phase)
        host.close()
    }

    @Test
    fun `a missing accelerometer blocks the run and reports nothing`() = runTest {
        val missing = object : AccelerometerSensor {
            override fun readings(): Flow<Vector3> = flow { error("no accelerometer") }
        }
        val host = RecordingHost(this)
        val game = ClassRushGame(missing, newSeed = { 1L })
        game.start(host)
        advanceTimeBy(1_000)
        assertEquals(RushPhase.BLOCKED, game.state.value.phase)
        assertTrue(host.finishes.isEmpty())
        host.close()
    }

    @Test
    fun `haptics buzz per hop and harder on an early end, end banners explain why`() {
        val playing = RushState()
        assertEquals(RushBuzz.Hop, rushBuzz(playing, playing.copy(hops = 1)))
        assertEquals(RushBuzz.Crash, rushBuzz(playing, playing.copy(phase = RushPhase.OVER, end = RushEnd.HIT)))
        assertEquals(null, rushBuzz(playing, playing.copy(phase = RushPhase.OVER, end = RushEnd.TIME_UP)))
        assertEquals("Time! 12 rows", endMessage(RushState(maxRow = 12, end = RushEnd.TIME_UP)))
        assertEquals("0:42", clockText(41_200))
        assertEquals("1:00", clockText(RUN_MS))
    }
}
