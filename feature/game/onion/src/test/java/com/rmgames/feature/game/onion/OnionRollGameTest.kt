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
 * on [GameScope.hostScope], reports whole-second scores and finishes with a loss
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

    /** Endless in production; tests cap the run so finishes are deterministic. */
    private fun endingAt(ms: Long) = OnionRollEngine(spawnFruits = false, timeLimitMs = ms)

    /** Endless and fruit-free: random traffic could knock the onion off mid-test (flaky). */
    private fun noTraffic() = OnionRollEngine(spawnFruits = false)

    @Test
    fun `the entry draws its own HUD so the host hides its placeholder texts`() {
        assertTrue(onionRollEntry(FakeGyroscopeSensor()).drawsOwnHud)
    }

    @Test
    fun `the game is endless - no duration cap`() {
        assertEquals(Long.MAX_VALUE, OnionRollGame(FakeGyroscopeSensor()).durationMs)
    }

    @Test
    fun `a full run reports rising scores and finishes with a loss after the fall`() = runTest {
        val host = RecordingHost(this)
        // The belt is endless; a test-only time limit makes the finish deterministic.
        val game = OnionRollGame(FakeGyroscopeSensor(), endingAt(82_000L))

        game.start(host)
        advanceTimeBy(84_500) // fall at 82 s + 2 s "YOU LOST!" banner

        assertTrue("expected scores, got ${host.scores}", host.scores.isNotEmpty())
        assertEquals(host.scores, host.scores.sorted())
        assertEquals(listOf(false), host.finishes)
        assertEquals(82L, host.scores.last()) // whole seconds
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
        val game = OnionRollGame(FakeGyroscopeSensor(), noTraffic())

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
        val game = OnionRollGame(FakeGyroscopeSensor(), endingAt(82_000L))

        game.start(host)
        advanceTimeBy(84_500)
        assertEquals(listOf(false), host.finishes)
        host.scores.clear()
        host.finishes.clear()

        game.start(host) // retry: fresh run, fresh state
        advanceTimeBy(2_000)

        assertEquals(Phase.RUNNING, game.state.value.phase)
        assertTrue(host.scores.isNotEmpty())
        assertTrue("expected ~2 s of score, got ${host.scores.last()}", host.scores.last() in 1L..2L)
        game.stop()
    }

    @Test
    fun `start twice keeps a single loop running`() = runTest {
        val host = RecordingHost(this)
        val game = OnionRollGame(FakeGyroscopeSensor(), noTraffic())

        game.start(host)
        advanceTimeBy(2_000)
        game.start(host) // must cancel the previous loop
        host.scores.clear()

        advanceTimeBy(2_000)

        // One loop advances the engine ~2 s; a leaked double loop would double the clock.
        assertTrue("expected a single loop, score=${host.scores.last()}", host.scores.last() in 1L..2L)
        game.stop()
    }

    @Test
    fun `rolling a flat-held phone steers the onion too`() = runTest {
        val gyro = FakeGyroscopeSensor()
        val host = RecordingHost(this)
        val game = OnionRollGame(gyro)

        game.start(host)
        advanceTimeBy(1)
        gyro.emit(Vector3(0f, 1f, 0f)) // ω_y = +1 → right edge dips → lean right
        advanceTimeBy(300)

        assertTrue("expected rightward movement, got ${game.state.value.onionX}", game.state.value.onionX > 0.5f)
        game.stop()
    }

    @Test
    fun `tilt rate combines the steering-wheel and roll axes`() {
        assertEquals(1f, tiltRate(Vector3(0f, 0f, 1f)), 0.0001f) // upright, lean left
        assertEquals(-1f, tiltRate(Vector3(0f, 1f, 0f)), 0.0001f) // flat, lean right
        assertEquals(0f, tiltRate(Vector3(5f, 0f, 0f)), 0.0001f) // pitching forward never steers
    }

    @Test
    fun `debug restart starts a fresh run on the same loop`() = runTest {
        val host = RecordingHost(this)
        val game = OnionRollGame(FakeGyroscopeSensor(), noTraffic())

        game.start(host)
        advanceTimeBy(5_000)
        game.restart()
        advanceTimeBy(1_000)

        val s = game.state.value
        assertEquals(Phase.RUNNING, s.phase)
        assertTrue("expected ~1 s into the new run, got ${s.elapsedMs}", s.elapsedMs in 900L..1_100L)
        assertTrue(host.finishes.isEmpty())
        game.stop()
    }

    @Test
    fun `debug restart works during the loss banner`() = runTest {
        val host = RecordingHost(this)
        val game = OnionRollGame(FakeGyroscopeSensor(), endingAt(82_000L))

        game.start(host)
        advanceTimeBy(82_500) // falling, "YOU LOST!" on screen
        assertEquals(Phase.FALLING, game.state.value.phase)
        game.restart()
        advanceTimeBy(3_000)

        assertEquals(Phase.RUNNING, game.state.value.phase)
        assertTrue(host.finishes.isEmpty())
        game.stop()
    }

    @Test
    fun `debug restart after the finish starts an unscored practice run`() = runTest {
        val host = RecordingHost(this)
        val game = OnionRollGame(FakeGyroscopeSensor(), endingAt(82_000L))

        game.start(host)
        advanceTimeBy(85_000)
        assertEquals(listOf(false), host.finishes)
        val scoresAtFinish = host.scores.size
        assertEquals(false, game.practice.value)

        game.restart(practiceScope = backgroundScope)
        advanceTimeBy(1_000)

        val s = game.state.value
        assertEquals(Phase.RUNNING, s.phase)
        assertTrue("expected ~1 s into the practice run, got ${s.elapsedMs}", s.elapsedMs in 900L..1_100L)
        assertEquals(true, game.practice.value)
        assertEquals("practice must not report scores", scoresAtFinish, host.scores.size)
        assertEquals(listOf(false), host.finishes)

        advanceTimeBy(85_000) // practice run ends on its own, still unreported
        assertEquals(Phase.DONE, game.state.value.phase)
        assertEquals(listOf(false), host.finishes)
    }

    @Test
    fun `a new scored run clears practice mode`() = runTest {
        val host = RecordingHost(this)
        val game = OnionRollGame(FakeGyroscopeSensor(), endingAt(82_000L))
        game.start(host)
        advanceTimeBy(85_000)
        game.restart(practiceScope = backgroundScope)
        advanceTimeBy(500)
        assertEquals(true, game.practice.value)

        game.start(host)
        advanceTimeBy(500)

        assertEquals(false, game.practice.value)
        game.stop()
    }
}
