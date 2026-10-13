package com.rmgames.feature.session

import com.rmgames.core.data.AttemptRepository
import com.rmgames.core.data.ScoreRepository

data class RunResult(val dailyBest: Long, val remainingAttempts: Int)

/**
 * Game host logic: 1 run = 1 attempt, best-of-2, lock at 0.
 * Plain class (no Android deps) so it unit-tests on JVM.
 */
class SessionViewModel(
    private val attempts: AttemptRepository,
    private val scores: ScoreRepository,
    private val dayIndex: Long,
    private val gameId: String,
    private val higherWins: Boolean
) {
    fun canPlay(): Boolean = attempts.remaining(dayIndex) > 0

    fun remainingAttempts(): Int = attempts.remaining(dayIndex)

    fun startRun(): Boolean = attempts.consume(dayIndex)

    fun finishRun(value: Long, win: Boolean): RunResult {
        val best = scores.submit(dayIndex, gameId, value, higherWins)
        return RunResult(dailyBest = best, remainingAttempts = attempts.remaining(dayIndex))
    }
}
