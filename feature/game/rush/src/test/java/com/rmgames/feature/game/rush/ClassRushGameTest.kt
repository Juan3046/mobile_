package com.rmgames.feature.game.rush

import com.rmgames.core.common.GameScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test
    fun `taps hop forward, rows are reported and the run ends once`() = runTest {
        val host = RecordingHost(this)
        val game = ClassRushGame(newSeed = { 1L })
        game.start(host)
        runCurrent()
        repeat(SAFE_START_ROWS - 1) { game.onTouch(Hop.FORWARD); advanceTimeBy(100) }
        assertEquals(SAFE_START_ROWS - 1, game.state.value.row)
        assertTrue(host.scores.contains((SAFE_START_ROWS - 1).toLong()))
        advanceTimeBy(RUN_MS) // idle (or the clock) ends it
        assertEquals(1, host.finishes.size)
        assertEquals(RushPhase.OVER, game.state.value.phase)
        game.onTouch(Hop.FORWARD) // nothing moves after the end
        assertEquals(SAFE_START_ROWS - 1, game.state.value.score)
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
