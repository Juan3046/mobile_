package com.rmgames.core.data

import com.rmgames.core.common.SeasonPoints
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** One player's best of a day, as uploaded to the shared ranking. */
data class DailyEntry(val dayIndex: Long, val gameId: String, val best: Long, val higherWins: Boolean, val nick: String)

/** Where a best stands among the day's players, and the season points it is worth. */
data class Standing(val position: Int, val players: Int, val points: Int) {
    /** "#4 of 212 · 5,225 pts": the day's position with the season points next to it. */
    fun label(): String = "#$position of $players · ${String.format(java.util.Locale.US, "%,d", points)} pts"
}

/** One row of a day's leaderboard. */
data class RankRow(val position: Int, val nick: String, val best: Long, val isMe: Boolean)

/**
 * Standing from counts (docs/01: competition ranking, points = (N − p + 1) × 25). If the
 * count raced my own upload and does not include me yet, I am counted.
 */
fun standingOf(betterCount: Int, players: Int): Standing {
    val position = betterCount + 1
    val total = maxOf(players, position)
    return Standing(position, total, SeasonPoints.pointsFor(position, total))
}

/** A season's points = sum of each played day's points (missed days = 0). */
fun seasonPoints(days: List<Standing?>): Int = days.sumOf { it?.points ?: 0 }

/**
 * The shared daily ranking (plan 09 PR 16) — the seam between the app and Firebase.
 * Implementations never throw: problems mean "unknown" (null / empty), and the UI keeps "—".
 */
interface Ranking {
    val isOnline: Boolean
    /** Uploads (or improves) this player's best of the day. Best effort. */
    suspend fun submit(entry: DailyEntry)
    /** My provisional standing for [myBest] on that day, or null if unknown/offline. */
    suspend fun standing(dayIndex: Long, gameId: String, myBest: Long, higherWins: Boolean): Standing?
    /** The day's top players (best first), or empty if unknown/offline. */
    suspend fun top(dayIndex: Long, gameId: String, higherWins: Boolean, limit: Int = 50): List<RankRow>
}

/** No backend configured: nothing is shared, everything stays "—". */
class OfflineRanking : Ranking {
    override val isOnline = false
    override suspend fun submit(entry: DailyEntry) = Unit
    override suspend fun standing(dayIndex: Long, gameId: String, myBest: Long, higherWins: Boolean): Standing? = null
    override suspend fun top(dayIndex: Long, gameId: String, higherWins: Boolean, limit: Int): List<RankRow> = emptyList()
}

/** My season so far for the Main screen: total points and today's standing (null = not played / unknown). */
data class SeasonStatus(val points: Int, val today: Standing?)

/** One day the player has a daily best for (the day's game). */
data class PlayedDay(val dayIndex: Long, val gameId: String, val best: Long, val higherWins: Boolean)

/**
 * What the screens use (plan 09 PR 16): uploads the player's daily best under their nick and
 * reads standings. Uploads are retried on every report, so a run finished offline reaches the
 * ranking the next time the Result or the Seasons history is opened (today and yesterday only —
 * older days are closed).
 */
class RankingSync(private val ranking: Ranking, private val nick: () -> String?) {
    val isOnline: Boolean get() = ranking.isOnline

    /** Uploads [day]'s best, then reads my standing. Null when offline or unknown. */
    suspend fun report(day: PlayedDay): Standing? {
        if (!ranking.isOnline) return null
        val me = nick() ?: return null
        ranking.submit(DailyEntry(day.dayIndex, day.gameId, day.best, day.higherWins, me))
        return ranking.standing(day.dayIndex, day.gameId, day.best, day.higherWins)
    }

    /** Standing of every played day ([today] and yesterday are re-uploaded first). */
    suspend fun season(days: List<PlayedDay>, today: Long): Map<Long, Standing?> {
        if (!ranking.isOnline || nick() == null) return days.associate { it.dayIndex to null }
        return coroutineScope {
            days.map { d ->
                async {
                    d.dayIndex to if (d.dayIndex >= today - 1) report(d)
                    else ranking.standing(d.dayIndex, d.gameId, d.best, d.higherWins)
                }
            }.awaitAll().toMap()
        }
    }

    /**
     * My season points so far (Main screen strip): the sum of the days whose standing is known.
     * 0 before playing; null when offline or when no played day could be read.
     */
    suspend fun seasonTotal(days: List<PlayedDay>, today: Long): SeasonStatus? {
        if (!ranking.isOnline) return null
        if (days.isEmpty()) return SeasonStatus(0, null)
        val standings = season(days, today)
        if (standings.values.all { it == null }) return null
        return SeasonStatus(seasonPoints(standings.values.toList()), standings[today])
    }

    suspend fun top(dayIndex: Long, gameId: String, higherWins: Boolean, limit: Int = 50): List<RankRow> =
        if (ranking.isOnline) ranking.top(dayIndex, gameId, higherWins, limit) else emptyList()
}

/**
 * The current season's played days up to [today] (the season of [today]): one per day, only
 * the day's daily game when [dailyGameId] knows it (debug practice games never count).
 */
fun seasonPlayedDays(
    today: Long,
    history: List<DailyBest>,
    dailyGameId: (Long) -> String? = { null },
    higherWins: (gameId: String) -> Boolean,
): List<PlayedDay> {
    val season = SeasonCalendar.seasonDay(today).season
    return history
        .filter { b ->
            b.dayIndex <= today && SeasonCalendar.seasonDay(b.dayIndex).season == season &&
                dailyGameId(b.dayIndex).let { it == null || it == b.gameId }
        }
        .groupBy { it.dayIndex }.values.map { it.first() }
        .sortedBy { it.dayIndex }
        .map { PlayedDay(it.dayIndex, it.gameId, it.best, higherWins(it.gameId)) }
}
