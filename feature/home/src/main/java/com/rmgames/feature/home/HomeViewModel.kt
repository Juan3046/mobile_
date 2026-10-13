package com.rmgames.feature.home

import com.rmgames.core.data.AttemptRepository
import com.rmgames.core.data.DailyGameSelector

/** Main-screen state: attempts left + today's game. Plain class for JVM tests. */
class HomeViewModel(
    private val attempts: AttemptRepository,
    private val dayIndex: Long,
    private val gameIds: List<String>
) {
    fun remainingAttempts(): Int = attempts.remaining(dayIndex)

    fun dailyGameId(): String = DailyGameSelector.select(dayIndex, gameIds)
}
