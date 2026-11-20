package com.rmgames.core.online

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rmgames.core.data.DailyEntry
import com.rmgames.core.data.RankRow
import com.rmgames.core.data.Ranking
import com.rmgames.core.data.Standing
import com.rmgames.core.data.standingOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** A stored daily best. */
data class StoredBest(val uid: String, val nick: String, val best: Long)

/** The shared `days/{day}/games/{game}/players/{uid}` collection. Throws when offline. */
interface RankingStore {
    /** Writes [entry] for [uid] only if it is better than what is stored (or nothing is). */
    suspend fun upsertBest(uid: String, entry: DailyEntry)
    suspend fun countBetter(dayIndex: Long, gameId: String, best: Long, higherWins: Boolean): Int
    suspend fun countPlayers(dayIndex: Long, gameId: String): Int
    suspend fun top(dayIndex: Long, gameId: String, higherWins: Boolean, limit: Int): List<StoredBest>
}

/**
 * Online daily ranking (plan 09 PR 16): uploads the player's daily best and reads standings
 * with count queries (cheap on the Spark quota) and a top-N page. Every failure or a call slower
 * than [timeoutMs] is reported to [onError] and becomes "unknown" (null / empty), never a crash.
 */
class OnlineRanking(
    private val auth: AnonymousAuth,
    private val store: RankingStore,
    private val timeoutMs: Long = 10_000,
    private val onError: (Throwable) -> Unit = {},
) : Ranking {
    override val isOnline = true

    override suspend fun submit(entry: DailyEntry) {
        safely(Unit) { store.upsertBest(auth.uid(), entry) }
    }

    override suspend fun standing(dayIndex: Long, gameId: String, myBest: Long, higherWins: Boolean): Standing? =
        safely(null) {
            auth.uid()
            standingOf(store.countBetter(dayIndex, gameId, myBest, higherWins), store.countPlayers(dayIndex, gameId))
        }

    override suspend fun top(dayIndex: Long, gameId: String, higherWins: Boolean, limit: Int): List<RankRow> =
        safely(emptyList()) {
            val me = auth.uid()
            val rows = store.top(dayIndex, gameId, higherWins, limit)
            rows.map { r ->
                val better = rows.count { if (higherWins) it.best > r.best else it.best < r.best }
                RankRow(position = better + 1, nick = r.nick, best = r.best, isMe = r.uid == me)
            }
        }

    private suspend fun <T> safely(fallback: T, block: suspend () -> T): T = try {
        withTimeout(timeoutMs) { block() }
    } catch (e: TimeoutCancellationException) {
        onError(e); fallback
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onError(e); fallback
    }
}

/**
 * Firestore implementation. Path `days/{dayIndex}/games/{gameId}/players/{uid}` keeps each
 * day's game in its own collection, so ordering and counting by `best` need no composite index.
 * Writes are checked by `firestore.rules` (own doc, owned nick, current day, only improving).
 */
class FirestoreRankingStore(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) : RankingStore {

    private fun players(dayIndex: Long, gameId: String) =
        db.collection("days").document(dayIndex.toString()).collection("games").document(gameId).collection("players")

    override suspend fun upsertBest(uid: String, entry: DailyEntry) {
        val doc = players(entry.dayIndex, entry.gameId).document(uid)
        db.runTransaction { tx ->
            val old = tx.get(doc).getLong("best")
            val better = old == null || if (entry.higherWins) entry.best > old else entry.best < old
            if (better) {
                tx.set(doc, mapOf(
                    "nick" to entry.nick, "best" to entry.best, "higherWins" to entry.higherWins,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ))
            }
        }.await()
    }

    override suspend fun countBetter(dayIndex: Long, gameId: String, best: Long, higherWins: Boolean): Int {
        val q = players(dayIndex, gameId).let { if (higherWins) it.whereGreaterThan("best", best) else it.whereLessThan("best", best) }
        return q.count().get(AggregateSource.SERVER).await().count.toInt()
    }

    override suspend fun countPlayers(dayIndex: Long, gameId: String): Int =
        players(dayIndex, gameId).count().get(AggregateSource.SERVER).await().count.toInt()

    override suspend fun top(dayIndex: Long, gameId: String, higherWins: Boolean, limit: Int): List<StoredBest> =
        players(dayIndex, gameId)
            .orderBy("best", if (higherWins) Query.Direction.DESCENDING else Query.Direction.ASCENDING)
            .limit(limit.toLong()).get().await()
            .documents.map { StoredBest(it.id, it.getString("nick").orEmpty(), it.getLong("best") ?: 0) }
}
