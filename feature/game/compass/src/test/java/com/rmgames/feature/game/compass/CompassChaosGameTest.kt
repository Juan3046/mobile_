package com.rmgames.feature.game.compass

import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.CompassSensor
import com.rmgames.core.sensors.FakeCompassSensor
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
 * Wiring of [CompassChaosGame] to the frozen host contract (docs/08): the run ticks
 * on [GameScope.hostScope], reports point scores and finishes with a win when the
 * fixed 20 s round expires. Virtual time drives the 16 ms loop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CompassChaosGameTest {

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

    /** Sensor that is simply absent: the flow fails on the first collection. */
    private class MissingCompass : CompassSensor {
        override fun headings(): Flow<Float> = flow {
            throw IllegalStateException("magnetometer not available")
        }
    }

    /** Sensor that dies on demand: the next reading fails the flow. */
    private class FlakyCompass : CompassSensor {
        private val source = MutableSharedFlow<Float>(extraBufferCapacity = 64)
        var failing = false

        override fun headings(): Flow<Float> = source.onEach {
            if (failing) throw IllegalStateException("magnetometer unavailable")
        }

        suspend fun emit(heading: Float) = source.emit(heading)
    }

    @Test
    fun `the round ends after twenty seconds with a win`() = runTest {
        val host = RecordingHost(this)
        val game = CompassChaosGame(FakeCompassSensor())

        game.start(host)
        advanceTimeBy(21_000)

        assertEquals(listOf(true), host.finishes)
        assertEquals(CompassPhase.DONE, game.state.value.phase)
        assertEquals(20_000L, game.state.value.elapsedMs)
        assertTrue("expected scores, got ${host.scores}", host.scores.isNotEmpty())
        assertEquals(host.scores, host.scores.sorted())
        assertEquals(game.state.value.score.toLong(), host.scores.last())
    }

    @Test
    fun `a missing magnetometer blocks the run and never reports a score`() = runTest {
        val host = RecordingHost(this)
        val game = CompassChaosGame(MissingCompass())

        game.start(host)
        advanceTimeBy(500)

        assertEquals(CompassPhase.BLOCKED, game.state.value.phase)
        assertTrue("a blocked run must not report a score", host.scores.isEmpty())
        assertTrue("a blocked run must not record a result", host.finishes.isEmpty())
    }

    @Test
    fun `a sensor that dies mid-run blocks instead of recording a result`() = runTest {
        val compass = FlakyCompass()
        val host = RecordingHost(this)
        val game = CompassChaosGame(compass)

        game.start(host)
        advanceTimeBy(1)
        compass.emit(45f) // 45° is the dead boundary for every cardinal: never scores
        advanceTimeBy(500)

        assertEquals(CompassPhase.RUNNING, game.state.value.phase)
        assertEquals(45f, game.state.value.heading, 0.01f)
        assertTrue("the run was live before the sensor died", host.scores.isNotEmpty())

        compass.failing = true
        compass.emit(90f)
        advanceTimeBy(500)

        assertEquals(CompassPhase.BLOCKED, game.state.value.phase)
        assertTrue("a blocked run must not record a result", host.finishes.isEmpty())
    }

    @Test
    fun `stop ends the run and no further scores arrive`() = runTest {
        val host = RecordingHost(this)
        val game = CompassChaosGame(FakeCompassSensor())

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
        val game = CompassChaosGame(FakeCompassSensor())

        game.start(host)
        advanceTimeBy(3_000)
        host.scores.clear()
        host.finishes.clear()

        game.start(host) // retry: fresh run, fresh state
        advanceTimeBy(500)

        assertEquals(CompassPhase.RUNNING, game.state.value.phase)
        assertTrue(
            "fresh clock expected, got ${game.state.value.elapsedMs}",
            game.state.value.elapsedMs in 0L..600L,
        )
        game.stop()
    }

    @Test
    fun `start twice keeps a single loop running`() = runTest {
        val host = RecordingHost(this)
        val game = CompassChaosGame(FakeCompassSensor())

        game.start(host)
        advanceTimeBy(2_000)
        game.start(host) // must cancel the previous loop
        host.scores.clear()

        advanceTimeBy(2_000)

        // One loop advances the round ~2 s; a leaked double loop would double the clock.
        val elapsed = game.state.value.elapsedMs
        assertTrue("expected a single loop, elapsed=$elapsed", elapsed in 1_500L..2_500L)
        game.stop()
    }

    @Test
    fun `the debug rotation can score without sensor readings`() = runTest {
        val host = RecordingHost(this)
        val game = CompassChaosGame(FakeCompassSensor())

        game.start(host)
        advanceTimeBy(50) // the first direction is called on the first tick

        val target = game.state.value.targetDegrees ?: error("no direction called yet")
        // The sensor never fires, so the needle rests at 0°: sweep the short way round.
        val forward = ((target % 360) + 360) % 360
        game.setDebugRotate(if (forward <= 180) 1f else -1f)
        advanceTimeBy(1_500) // 180°/s → a 270° sweep passes through every zone
        game.setDebugRotate(null)
        advanceTimeBy(100)

        assertTrue(
            "expected at least one scored direction, score=${game.state.value.score}",
            game.state.value.score >= 1,
        )
        assertTrue(host.scores.isNotEmpty())
        game.stop()
    }
}
