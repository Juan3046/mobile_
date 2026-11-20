package com.rmgames.feature.home

import com.rmgames.core.common.Game
import com.rmgames.core.data.AttemptRepository
import com.rmgames.core.data.DailyGameSelector
import com.rmgames.core.data.DayClock
import com.rmgames.core.data.MAX_ATTEMPTS_PER_DAY
import com.rmgames.core.data.ScoreRepository
import com.rmgames.core.data.SeasonCalendar

/** Everything the Main screen (docs/03 §3.1, §3.5) shows. */
data class HomeUiState(
    val gameId: String,
    val title: String,
    val howToPlay: String,
    val durationMs: Long,
    val attemptsLeft: Int,
    val attemptsTotal: Int,
    /** No attempts left: Play disabled + countdown + best of the day (03 §3.5). */
    val locked: Boolean,
    val bestToday: Long?,
    /** Next 12:00 boundary: new daily game + fresh attempts. */
    val nextResetMillis: Long,
    val season: Int,
    val seasonDayIndex: Int,
    /** Season points; null until the online ranking computes them (plan 09 stream E). */
    val seasonPoints: Long?,
)

/** Main-screen logic. Plain class (no Android deps) so it unit-tests on the JVM. */
class HomeViewModel(
    private val attempts: AttemptRepository,
    private val scores: ScoreRepository,
    private val clock: DayClock,
    private val games: List<Game>,
) {
    init {
        require(games.isNotEmpty()) { "games must not be empty" }
    }

    fun state(nowMillis: Long = System.currentTimeMillis()): HomeUiState {
        val day = clock.dayIndex(nowMillis)
        val gameId = DailyGameSelector.select(day, games.map { it.id })
        val game = games.first { it.id == gameId }
        val left = attempts.remaining(day)
        val season = SeasonCalendar.seasonDay(day)
        return HomeUiState(
            gameId = game.id,
            title = game.title,
            howToPlay = game.description,
            durationMs = game.durationMs,
            attemptsLeft = left,
            attemptsTotal = MAX_ATTEMPTS_PER_DAY,
            locked = left <= 0,
            bestToday = scores.best(day, game.id),
            nextResetMillis = clock.nextResetMillis(nowMillis),
            season = season.season,
            seasonDayIndex = season.dayOfSeason,
            seasonPoints = null,
        )
    }

    /** Debug-only "Reset attempts" (docs/02): today's counter only. */
    fun resetAttempts(nowMillis: Long = System.currentTimeMillis()) {
        attempts.reset(clock.dayIndex(nowMillis))
    }
}
