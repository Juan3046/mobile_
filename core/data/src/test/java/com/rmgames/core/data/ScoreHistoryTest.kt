package com.rmgames.core.data

import com.rmgames.core.data.store.InMemorySnapshotStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreHistoryTest {

    @Test
    fun `in-memory history lists every daily best sorted by day`() {
        val repo = InMemoryScoreRepository()
        repo.submit(12, "05-soda-shake", 500, higherWins = false)
        repo.submit(10, "03-blink-sniper", 27, higherWins = true)
        repo.submit(12, "05-soda-shake", 427, higherWins = false)
        assertEquals(
            listOf(DailyBest(10, "03-blink-sniper", 27), DailyBest(12, "05-soda-shake", 427)),
            repo.history(),
        )
    }

    @Test
    fun `persistent history includes what was loaded from storage`() = runTest {
        val store = InMemorySnapshotStore(mapOf((3L to "02-onion-roll") to 62L))
        val repo = PersistentScoreRepository(store, backgroundScope).apply { load() }
        repo.submit(4, "04-compass-chaos", 9, higherWins = true)
        assertEquals(
            listOf(DailyBest(3, "02-onion-roll", 62), DailyBest(4, "04-compass-chaos", 9)),
            repo.history(),
        )
    }

    @Test
    fun `personal best is the best daily best ever, by the game's rule`() {
        val repo = InMemoryScoreRepository()
        repo.submit(10, "03-blink-sniper", 27, higherWins = true)
        repo.submit(11, "03-blink-sniper", 31, higherWins = true)
        repo.submit(12, "05-soda-shake", 900, higherWins = false)
        repo.submit(13, "05-soda-shake", 845, higherWins = false)
        assertEquals(DailyBest(11, "03-blink-sniper", 31), repo.personalBest("03-blink-sniper", higherWins = true))
        assertEquals(DailyBest(13, "05-soda-shake", 845), repo.personalBest("05-soda-shake", higherWins = false))
        assertEquals(null, repo.personalBest("02-onion-roll", higherWins = true))
    }
}
