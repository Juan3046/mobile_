package com.rmgames.feature.game.onion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Regression: AudioTrack calls on the main thread froze the game ~0.5 s on every
 * per-second tick (device playtest). Cues must be handed off, never played inline.
 */
class AsyncCuePlayerTest {

    @Test
    fun `play returns immediately even when the sound call blocks`() {
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val player = AsyncCuePlayer(executor) { release.await(5, TimeUnit.SECONDS) }

        val startNs = System.nanoTime()
        player.play(Cue.TICK)
        val tookMs = (System.nanoTime() - startNs) / 1_000_000

        release.countDown()
        executor.shutdown()
        assertTrue("play blocked the caller for $tookMs ms", tookMs < 50)
    }

    @Test
    fun `cues are played off the calling thread, in order`() {
        val executor = Executors.newSingleThreadExecutor()
        val played = mutableListOf<Cue>()
        var playThread: Thread? = null
        val player = AsyncCuePlayer(executor) { cue ->
            playThread = Thread.currentThread()
            played += cue
        }

        player.play(Cue.HIT)
        player.play(Cue.FALL)
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        assertEquals(listOf(Cue.HIT, Cue.FALL), played)
        assertNotEquals(Thread.currentThread(), playThread)
    }

    @Test
    fun `cues after shutdown are dropped without crashing`() {
        val executor = Executors.newSingleThreadExecutor()
        val player = AsyncCuePlayer(executor) { }
        executor.shutdown()

        player.play(Cue.HIT) // must not throw RejectedExecutionException
    }
}
