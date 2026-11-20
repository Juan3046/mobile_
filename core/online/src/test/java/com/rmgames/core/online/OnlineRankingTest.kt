package com.rmgames.core.online

import com.rmgames.core.data.DailyEntry
import com.rmgames.core.data.RankRow
import com.rmgames.core.data.Standing
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineRankingTest {

    private class FakeAuth(private val fail: Boolean = false) : AnonymousAuth {
        override suspend fun uid(): String = if (fail) error("offline") else "me"
    }

    /** In-memory `days/{day}/games/{game}/players/{uid}`. */
    private class FakeStore(val fail: Boolean = false) : RankingStore {
        val rows = mutableMapOf<Pair<Long, String>, MutableMap<String, StoredBest>>()
        override suspend fun upsertBest(uid: String, entry: DailyEntry) {
            if (fail) error("down")
            val day = rows.getOrPut(entry.dayIndex to entry.gameId) { mutableMapOf() }
            val old = day[uid]
            val better = old == null || if (entry.higherWins) entry.best > old.best else entry.best < old.best
            if (better) day[uid] = StoredBest(uid, entry.nick, entry.best)
        }
        override suspend fun countBetter(dayIndex: Long, gameId: String, best: Long, higherWins: Boolean): Int {
            if (fail) error("down")
            return rows[dayIndex to gameId].orEmpty().values.count { if (higherWins) it.best > best else it.best < best }
        }
        override suspend fun countPlayers(dayIndex: Long, gameId: String): Int {
            if (fail) error("down")
            return rows[dayIndex to gameId].orEmpty().size
        }
        override suspend fun top(dayIndex: Long, gameId: String, higherWins: Boolean, limit: Int): List<StoredBest> {
            if (fail) error("down")
            val all = rows[dayIndex to gameId].orEmpty().values
            return (if (higherWins) all.sortedByDescending { it.best } else all.sortedBy { it.best }).take(limit)
        }
        fun put(day: Long, game: String, uid: String, nick: String, best: Long) {
            rows.getOrPut(day to game) { mutableMapOf() }[uid] = StoredBest(uid, nick, best)
        }
    }

    private fun entry(best: Long, higherWins: Boolean = true) = DailyEntry(10, "g", best, higherWins, "hola")

    @Test
    fun `submitting uploads my best under my uid and only improves it`() = runTest {
        val store = FakeStore()
        val r = OnlineRanking(FakeAuth(), store)
        r.submit(entry(9)); r.submit(entry(7)); r.submit(entry(12))
        assertEquals(12L, store.rows[10L to "g"]!!["me"]!!.best)
    }

    @Test
    fun `standing counts the better players and the total`() = runTest {
        val store = FakeStore().apply { put(10, "g", "a", "ana", 20); put(10, "g", "b", "bob", 5); put(10, "g", "me", "hola", 9) }
        assertEquals(Standing(2, 3, 50), OnlineRanking(FakeAuth(), store).standing(10, "g", 9, higherWins = true))
    }

    @Test
    fun `lower-wins games count smaller scores as better`() = runTest {
        val store = FakeStore().apply { put(10, "s", "a", "ana", 400); put(10, "s", "me", "hola", 427) }
        assertEquals(Standing(2, 2, 25), OnlineRanking(FakeAuth(), store).standing(10, "s", 427, higherWins = false))
    }

    @Test
    fun `top lists best first with shared positions on ties and marks me`() = runTest {
        val store = FakeStore().apply { put(10, "g", "a", "ana", 20); put(10, "g", "me", "hola", 20); put(10, "g", "b", "bob", 5) }
        val rows = OnlineRanking(FakeAuth(), store).top(10, "g", higherWins = true)
        assertEquals(listOf(1, 1, 3), rows.map { it.position })
        assertEquals(listOf(5L), rows.filter { it.position == 3 }.map { it.best })
        assertTrue(rows.single { it.nick == "hola" }.isMe)
    }

    @Test
    fun `offline or failing backend gives unknown, never an exception`() = runTest {
        val errors = mutableListOf<Throwable>()
        val r = OnlineRanking(FakeAuth(fail = true), FakeStore(), onError = { errors += it })
        r.submit(entry(9))
        assertNull(r.standing(10, "g", 9, true))
        assertEquals(emptyList<RankRow>(), r.top(10, "g", true))
        val broken = OnlineRanking(FakeAuth(), FakeStore(fail = true), onError = { errors += it })
        assertNull(broken.standing(10, "g", 9, true))
        assertTrue(errors.size >= 4)
    }
}
