package com.rmgames.feature.ranking

import com.rmgames.core.data.DailyBest
import com.rmgames.core.data.Standing
import com.rmgames.core.data.seasonPoints
import com.rmgames.core.data.SeasonCalendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class DayState { Played, Missed, Today, Upcoming }

/** One day of a season. [gameTitle]/[best] are set when the player has a score that day. */
data class SeasonDayRow(
    val dayIndex: Long,
    val dateLabel: String,
    val state: DayState,
    val gameId: String?,
    val gameTitle: String?,
    val best: Long?,
)

data class SeasonSummary(val season: Int, val dateRange: String, val days: List<SeasonDayRow>, val daysPlayed: Int)

data class PastSeason(val season: Int, val dateRange: String, val daysPlayed: Int)

data class AllTime(val daysPlayed: Int, val seasonsPlayed: Int)

/** The best day ever for one game. */
data class PersonalBestRow(val gameId: String, val gameTitle: String, val best: Long, val dayIndex: Long, val dateLabel: String)

/**
 * Seasons history built from the player's own daily bests (docs/03 §3.4, local data
 * first — plan 09 PR 13). Points and other players' standings come with the online
 * ranking (stream E); until then the screen shows what was played and the bests.
 */
data class SeasonHistory(
    val current: SeasonSummary,
    val past: List<PastSeason>,
    val allTime: AllTime,
    val personalBests: List<PersonalBestRow> = emptyList(),
) {
    companion object {
        private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)
        private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.US)

        /**
         * @param dailyGameId the day's daily game, when known: only its best counts for that day
         * (debug-launched runs of other games are ignored). Null = any game that day counts.
         */
        fun build(
            todayIndex: Long,
            history: List<DailyBest>,
            titles: Map<String, String>,
            dailyGameId: (Long) -> String? = { null },
            higherWins: (gameId: String) -> Boolean = { true },
        ): SeasonHistory {
            val inSeasons = history.filter { b ->
                SeasonCalendar.seasonDay(b.dayIndex).season >= 1 &&
                    dailyGameId(b.dayIndex).let { daily -> daily == null || daily == b.gameId }
            }
            val byDay = inSeasons.groupBy { it.dayIndex }
            val today = SeasonCalendar.seasonDay(todayIndex)
            val currentSeason = today.season.coerceAtLeast(1)
            val start = seasonStart(currentSeason)
            val days = (0 until SeasonCalendar.SEASON_LENGTH_DAYS).map { offset ->
                val dayIndex = start + offset
                val entry = byDay[dayIndex]?.firstOrNull()
                val state = when {
                    dayIndex == todayIndex -> DayState.Today
                    dayIndex > todayIndex -> DayState.Upcoming
                    entry != null -> DayState.Played
                    else -> DayState.Missed
                }
                SeasonDayRow(
                    dayIndex = dayIndex,
                    dateLabel = LocalDate.ofEpochDay(dayIndex).format(dayFormat),
                    state = state,
                    gameId = entry?.gameId,
                    gameTitle = entry?.let { titles[it.gameId] ?: it.gameId },
                    best = entry?.best,
                )
            }
            val bySeason = inSeasons.groupBy { SeasonCalendar.seasonDay(it.dayIndex).season }
            val past = bySeason.keys.filter { it < currentSeason }.sortedDescending().map { s ->
                PastSeason(s, range(s), bySeason.getValue(s).map { it.dayIndex }.distinct().size)
            }
            return SeasonHistory(
                current = SeasonSummary(currentSeason, range(currentSeason), days, days.count { it.best != null }),
                past = past,
                allTime = AllTime(
                    daysPlayed = inSeasons.map { it.dayIndex }.distinct().size,
                    seasonsPlayed = bySeason.size,
                ),
                personalBests = inSeasons.groupBy { it.gameId }.map { (id, days) ->
                    val best = if (higherWins(id)) days.maxBy { it.best } else days.minBy { it.best }
                    PersonalBestRow(id, titles[id] ?: id, best.best, best.dayIndex, LocalDate.ofEpochDay(best.dayIndex).format(dayFormat))
                }.sortedBy { it.gameTitle },
            )
        }

        private fun seasonStart(season: Int) =
            SeasonCalendar.SEASON_ONE_START_DAY + (season - 1L) * SeasonCalendar.SEASON_LENGTH_DAYS

        /** "16 – 22 Nov", or "30 Nov – 6 Dec" across months. */
        private fun range(season: Int): String {
            val from = LocalDate.ofEpochDay(seasonStart(season))
            val to = from.plusDays(SeasonCalendar.SEASON_LENGTH_DAYS - 1L)
            val left = if (from.month == to.month) from.dayOfMonth.toString() else from.format(dayMonth)
            return "$left – ${to.format(dayMonth)}"
        }
    }
}

/**
 * Season points tile (value to caption). [standings] null = still loading; a map where every
 * day is unknown = no connection. Points add up the days that are known.
 */
fun seasonPointsTile(online: Boolean, standings: Map<Long, Standing?>?): Pair<String, String> = when {
    !online -> "—" to "online ranking off"
    standings == null -> "…" to "checking"
    standings.isEmpty() -> "0" to "play to score"
    standings.values.all { it == null } -> "—" to "no connection"
    else -> String.format(Locale.US, "%,d", seasonPoints(standings.values.toList())) to "provisional"
}

fun standingLabel(s: Standing): String = s.label()
