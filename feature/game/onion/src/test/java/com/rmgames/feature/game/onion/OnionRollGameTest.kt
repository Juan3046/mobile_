package com.rmgames.feature.game.onion

import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.FakeGyroscopeSensor
import com.rmgames.core.sensors.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wiring of [OnionRollGame] to the frozen host contract (docs/08): the run ticks
 * on [GameScope.hostScope], reports centisecond scores and finishes with a loss
 * when the onion falls. Virtual time drives the 16 ms loop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnionRollGameTest {

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

    @Test
    fun `a full run reports rising scores and finishes with a loss after the fall`() = runTest {
        val host = RecordingHost(this)
        val game = OnionRollGame(FakeGyroscopeSensor())

        game.start(host)
        advanceTimeBy(83_000)

        assertTrue("expected scores, got ${host.scores}", host.scores.isNotEmpty())
        assertEquals(host.scores, host.scores.sorted())
        assertEquals(listOf(false), host.finishes)
        assertEquals(8_200L, host.scores.last())
        assertEquals(Phase.DONE, game.state.value.phase)
    }

    @Test
    fun `gyroscope readings steer the onion during a run`() = runTest {
        val gyro = FakeGyroscopeSensor()
        val host = RecordingHost(this)
        val game = OnionRollGame(gyro)

        game.start(host)
        advanceTimeBy(1) // let the collector subscribe
        gyro.emit(Vector3(0f, 0f, -1f)) // ω_z = -1 → lean right
        advanceTimeBy(500)

        assertTrue("expected rightward movement, got ${game.state.value.onionX}", game.state.value.onionX > 0.5f)
        game.stop()
    }

    @Test
    fun `stop ends the run and no further scores arrive`() = runTest {
        val host = RecordingHost(this)
        val game = OnionRollGame(FakeGyroscopeSensor())

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
        val game = OnionRollGame(FakeGyroscopeSensor())

        game.start(host)
        advanceTimeBy(83_000)
        assertEquals(listOf(false), host.finishes)
        host.scores.clear()
        host.finishes.clear()

        game.start(host) // retry: fresh run, fresh state
        advanceTimeBy(2_000)

        assertEquals(Phase.RUNNING, game.state.value.phase)
        assertTrue(host.scores.isNotEmpty())
        assertTrue("expected ~2 s of score, got ${host.scores.last()}", host.scores.last() in 150L..250L)
        game.stop()
    }

    @Test
    fun `start twice keeps a single loop running`() = runTest {
        val host = RecordingHost(this)
        val game = OnionRollGame(FakeGyroscopeSensor())

        game.start(host)
        advanceTimeBy(2_000)
        game.start(host) // must cancel the previous loop
        host.scores.clear()

        advanceTimeBy(2_000)

        // One loop advances the engine ~2 s; a leaked double loop would double the clock.
        assertTrue("expected a single loop, score=${host.scores.last()}", host.scores.last() in 150L..250L)
        game.stop()
    }
}
