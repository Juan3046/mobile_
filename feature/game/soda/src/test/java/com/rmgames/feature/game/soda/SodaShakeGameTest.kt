package com.rmgames.feature.game.soda

import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.AccelerometerSensor
import com.rmgames.core.sensors.FakeAccelerometerSensor
import com.rmgames.core.sensors.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wiring of [SodaShakeGame] to the frozen host contract (docs/08): the run ticks
 * on [GameScope.hostScope], reports stopwatch centiseconds and finishes with a
 * win once the can pops. Virtual time drives the 16 ms loop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SodaShakeGameTest {

    private class RecordingHost(private val delegate: CoroutineScope) : GameScope {
        override val hostScope: CoroutineScope = delegate
        val scores = mutableListOf<Long>()
        val finishes = mutableListOf<Boolean>()
        override fun onScore(value: Long) {
            scores += value
        }

        override fun onFinished(win: Boolean) {
            finishes += win
        }
    }

    private val rest = Vector3(0f, GRAVITY, 0f)

    private fun peak(dir: Float) = Vector3(20f * dir, GRAVITY, 0f)

    /** Accelerometer that dies on demand: the next reading fails the flow. */
    private class FlakyAccelerometer : AccelerometerSensor {
        private val source = MutableSharedFlow<Vector3>(extraBufferCapacity = 64)
        var failing = false

        override fun readings(): Flow<Vector3> = source.onEach {
            if (failing) throw IllegalStateException("accelerometer unavailable")
        }

        suspend fun emit(sample: Vector3) = source.emit(sample)
    }

    /** Sensor that is simply absent: the flow fails on the first collection. */
    private class MissingAccelerometer : AccelerometerSensor {
        override fun readings(): Flow<Vector3> = flow {
            throw IllegalStateException("accelerometer not available")
        }
    }

    @Test
    fun `a full run reports rising scores and finishes with a win when the can pops`() = runTest {
        val accelerometer = FakeAccelerometerSensor()
        val host = RecordingHost(this)
        val game = SodaShakeGame(accelerometer)

        game.start(host)
        advanceTimeBy(1) // let the collector subscribe
        repeat(20) { i ->
            accelerometer.emit(if (i % 2 == 0) peak(+1f) else peak(-1f))
            advanceTimeBy(48) // at least one loop tick sees the peak
            accelerometer.emit(rest)
            advanceTimeBy(160) // settle below the release level + dead-time margin
        }
        advanceTimeBy(500) // pop animation → DONE

        assertEquals(listOf(true), host.finishes)
        assertTrue("expected scores, got ${host.scores}", host.scores.isNotEmpty())
        assertEquals(host.scores, host.scores.sorted())
        assertEquals(game.state.value.scoreCentis, host.scores.last())
        assertTrue("unexpected final score ${host.scores.last()}", host.scores.last() in 300L..600L)
        assertEquals(SodaPhase.DONE, game.state.value.phase)
    }

    @Test
    fun `a missing accelerometer blocks the run and never reports a score`() = runTest {
        val host = RecordingHost(this)
        val game = SodaShakeGame(MissingAccelerometer())

        game.start(host)
        advanceTimeBy(500)

        assertEquals(SodaPhase.BLOCKED, game.state.value.phase)
        assertTrue("a blocked run must not report a score", host.scores.isEmpty())
        assertTrue("a blocked run must not record a result", host.finishes.isEmpty())
    }

    @Test
    fun `a sensor that dies mid-run blocks instead of recording a result`() = runTest {
        val accelerometer = FlakyAccelerometer()
        val host = RecordingHost(this)
        val game = SodaShakeGame(accelerometer)

        game.start(host)
        advanceTimeBy(1)
        repeat(3) { i ->
            accelerometer.emit(if (i % 2 == 0) peak(+1f) else peak(-1f))
            advanceTimeBy(48)
            accelerometer.emit(rest)
            advanceTimeBy(160)
        }
        assertTrue("the run was live before the sensor died", host.scores.isNotEmpty())

        accelerometer.failing = true
        accelerometer.emit(peak(+1f))
        advanceTimeBy(500)

        assertEquals(SodaPhase.BLOCKED, game.state.value.phase)
        assertTrue("a blocked run must not record a result", host.finishes.isEmpty())
    }

    @Test
    fun `stop ends the run and no further scores arrive`() = runTest {
        val host = RecordingHost(this)
        val game = SodaShakeGame(FakeAccelerometerSensor())

        game.start(host)
        advanceTimeBy(5_000)
        game.stop()
        val reported = host.scores.size

        advanceTimeBy(5_000)
        assertEquals(reported, host.scores.size)
        assertTrue(host.finishes.isEmpty())
    }

    @Test
    fun `a second run starts fresh`() = runTest {
        val host = RecordingHost(this)
        val game = SodaShakeGame(FakeAccelerometerSensor())

        game.start(host)
        advanceTimeBy(3_000)
        assertTrue(host.scores.isNotEmpty())
        host.scores.clear()
        host.finishes.clear()

        game.start(host) // retry: fresh run, fresh state
        advanceTimeBy(500)

        assertEquals(SodaPhase.RUNNING, game.state.value.phase)
        assertEquals(0, game.state.value.shakes)
        assertTrue("expected ~0.5 s of score, got ${host.scores.last()}", host.scores.last() in 0L..60L)
        game.stop()
    }

    @Test
    fun `start twice keeps a single loop running`() = runTest {
        val host = RecordingHost(this)
        val game = SodaShakeGame(FakeAccelerometerSensor())

        game.start(host)
        advanceTimeBy(2_000)
        game.start(host) // must cancel the previous loop
        host.scores.clear()

        advanceTimeBy(2_000)

        // One loop advances the stopwatch ~2 s; a leaked double loop would double the clock.
        assertTrue("expected a single loop, score=${host.scores.last()}", host.scores.last() in 150L..250L)
        game.stop()
    }

    @Test
    fun `the debug shake key can finish a run without sensor readings`() = runTest {
        val host = RecordingHost(this)
        val game = SodaShakeGame(FakeAccelerometerSensor())

        game.start(host)
        advanceTimeBy(1)
        repeat(20) {
            game.requestDebugShake() // BuildConfig.DEBUG-only, emulator playtesting
            advanceTimeBy(50)
        }
        advanceTimeBy(500) // pop animation → DONE

        assertEquals(listOf(true), host.finishes)
        assertEquals(game.state.value.scoreCentis, host.scores.last())
        assertEquals(SodaPhase.DONE, game.state.value.phase)
    }

    private companion object {
        const val GRAVITY = 9.81f
    }
}
