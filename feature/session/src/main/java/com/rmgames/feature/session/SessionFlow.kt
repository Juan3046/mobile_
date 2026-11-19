package com.rmgames.feature.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rmgames.core.common.Game
import com.rmgames.core.data.AttemptRepository
import com.rmgames.core.data.DayClock
import com.rmgames.core.data.MAX_ATTEMPTS_PER_DAY
import com.rmgames.core.data.ScoreRepository

/** The 3-2-1 before every run (plan 09 §2D.2). */
const val COUNTDOWN_FROM = 3

/** Where the player is in one visit to a game. */
sealed interface SessionPhase {
    /** How-to + Start. No attempt spent yet. */
    data object Intro : SessionPhase
    /** No attempts left today: nothing can start (03 §3.5). */
    data object Locked : SessionPhase
    data class Countdown(val secondsLeft: Int) : SessionPhase
    data object Playing : SessionPhase
    data class Finished(val summary: RunSummary) : SessionPhase
    /** Left mid-run: the attempt stays spent, nothing is recorded (docs/02 "No pause"). */
    data object Abandoned : SessionPhase
}

/** What the Result screen shows (03 §3.3). [score]/[dailyBest] are null when nothing was scored. */
data class RunSummary(
    val score: Long?,
    val dailyBest: Long?,
    val isNewBest: Boolean,
    val win: Boolean,
    val attemptsLeft: Int,
    val attemptsTotal: Int,
    val nextResetMillis: Long,
    /** Debug practice run: nothing was spent or recorded. */
    val practice: Boolean = false,
    /** Best daily best ever for this game (after this run). */
    val personalBest: Long? = null,
    /** This run beat every earlier day (not shown for the very first score). */
    val isPersonalBest: Boolean = false,
)

/**
 * Run flow for one game visit: Intro → Countdown(3..1) → Playing → Finished, with
 * "Play again" going straight back to the countdown while attempts remain.
 *
 * Rules (docs/02): pressing Start spends the attempt; best-of-2 per day (max or min by
 * [Game.higherWins]); a run belongs to the day it started, even if it ends after noon;
 * leaving mid-run forfeits it. [phase] is Compose snapshot state so the screen
 * recomposes on every change; the logic itself is plain and JVM-tested.
 */
class SessionFlow(
    private val game: Game,
    private val attempts: AttemptRepository,
    private val scores: ScoreRepository,
    private val clock: DayClock,
    /**
     * Practice run (debug launcher, a game that is not today's): spends no attempt and
     * records no score, so only real daily runs ever reach the scores and the history.
     */
    private val practice: Boolean = false,
    private val now: () -> Long = System::currentTimeMillis,
) {
    var phase: SessionPhase by mutableStateOf(
        if (practice || attempts.remaining(clock.dayIndex(now())) > 0) SessionPhase.Intro else SessionPhase.Locked,
    )
        private set

    /** Latest score of the live run (for the host HUD); null before the first report. */
    var liveScore: Long? by mutableStateOf(null)
        private set

    private var runDay = clock.dayIndex(now())

    fun attemptsLeft(): Int = attempts.remaining(clock.dayIndex(now()))

    /** Spends an attempt and starts the countdown. False (and Locked) when none are left. */
    fun start(): Boolean {
        if (phase is SessionPhase.Countdown || phase == SessionPhase.Playing) return false
        runDay = clock.dayIndex(now())
        if (!practice && !attempts.consume(runDay)) {
            phase = SessionPhase.Locked
            return false
        }
        liveScore = null
        phase = SessionPhase.Countdown(COUNTDOWN_FROM)
        return true
    }

    /** One second of countdown; after the last one the run is live. */
    fun tick() {
        val p = phase as? SessionPhase.Countdown ?: return
        phase = if (p.secondsLeft > 1) SessionPhase.Countdown(p.secondsLeft - 1) else SessionPhase.Playing
    }

    /** Latest score from the game; ignored outside a live run. */
    fun onScore(value: Long) {
        if (phase == SessionPhase.Playing) liveScore = value
    }

    fun onFinished(win: Boolean) {
        if (phase != SessionPhase.Playing) return
        val score = liveScore
        // Best of every EARLIER day (today's earlier run only counts as the daily best).
        val previousPersonal = scores.history().filter { it.gameId == game.id && it.dayIndex != runDay }
            .let { days -> if (game.higherWins) days.maxOfOrNull { it.best } else days.minOfOrNull { it.best } }
        val previous = scores.best(runDay, game.id)
        val best = if (practice) previous else score?.let { scores.submit(runDay, game.id, it, game.higherWins) } ?: previous
        val improved = !practice && score != null && (previous == null ||
            if (game.higherWins) score > previous else score < previous)
        phase = SessionPhase.Finished(
            RunSummary(
                score = score,
                dailyBest = best,
                isNewBest = improved,
                win = win,
                attemptsLeft = attempts.remaining(runDay),
                attemptsTotal = MAX_ATTEMPTS_PER_DAY,
                nextResetMillis = clock.nextResetMillis(now()),
                practice = practice,
                personalBest = scores.personalBest(game.id, game.higherWins)?.best,
                isPersonalBest = !practice && score != null && previousPersonal != null &&
                    if (game.higherWins) score > previousPersonal else score < previousPersonal,
            ),
        )
    }

    /** Player left during the countdown or the run. */
    fun abandon() {
        if (phase is SessionPhase.Countdown || phase == SessionPhase.Playing) phase = SessionPhase.Abandoned
    }
}
