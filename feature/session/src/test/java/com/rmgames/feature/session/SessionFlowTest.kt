package com.rmgames.feature.session

import com.rmgames.core.common.Game
import com.rmgames.core.common.GameScope
import com.rmgames.core.data.DayClock
import com.rmgames.core.data.InMemoryAttemptRepository
import com.rmgames.core.data.InMemoryScoreRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class SessionFlowTest {

    private class FakeGame(override val higherWins: Boolean = true) : Game {
        override val id = "03-blink-sniper"
        override val title = "Blink Sniper"
        override val description = "Blink to shoot."
        override val durationMs = 60_000L
        override fun start(scope: GameScope) {}
        override fun stop() {}
    }

    private val clock = DayClock(ZoneOffset.UTC)
    private var now = Instant.parse("2026-11-18T14:00:00Z").toEpochMilli()
    private val day get() = clock.dayIndex(now)
    private val attempts = InMemoryAttemptRepository()
    private val scores = InMemoryScoreRepository()
    private fun flow(game: Game = FakeGame()) = SessionFlow(game, attempts, scores, clock) { now }

    private fun SessionFlow.playTo(score: Long?, win: Boolean = true): RunSummary {
        assertTrue(start())
        repeat(COUNTDOWN_FROM) { tick() }
        score?.let { onScore(it) }
        onFinished(win)
        return (phase as SessionPhase.Finished).summary
    }

    @Test
    fun `opens on the how-to intro without spending an attempt`() {
        val f = flow()
        assertEquals(SessionPhase.Intro, f.phase)
        assertEquals(2, f.attemptsLeft())
    }

    @Test
    fun `opens locked when no attempts are left`() {
        attempts.consume(day); attempts.consume(day)
        assertEquals(SessionPhase.Locked, flow().phase)
    }

    @Test
    fun `start spends one attempt and counts down 3 2 1 then plays`() {
        val f = flow()
        assertTrue(f.start())
        assertEquals(1, attempts.remaining(day))
        assertEquals(SessionPhase.Countdown(3), f.phase)
        f.tick(); assertEquals(SessionPhase.Countdown(2), f.phase)
        f.tick(); assertEquals(SessionPhase.Countdown(1), f.phase)
        f.tick(); assertEquals(SessionPhase.Playing, f.phase)
    }

    @Test
    fun `start without attempts is refused and locks`() {
        attempts.consume(day); attempts.consume(day)
        val f = flow()
        assertFalse(f.start())
        assertEquals(SessionPhase.Locked, f.phase)
    }

    @Test
    fun `finishing submits the score and reports a new daily best`() {
        val s = flow().playTo(27)
        assertEquals(27L, s.score)
        assertEquals(27L, s.dailyBest)
        assertTrue(s.isNewBest)
        assertEquals(1, s.attemptsLeft)
        assertEquals(27L, scores.best(day, "03-blink-sniper"))
    }

    @Test
    fun `a worse second run keeps the best and is not a new best`() {
        val f = flow()
        f.playTo(27)
        val s = f.playTo(12)
        assertEquals(12L, s.score)
        assertEquals(27L, s.dailyBest)
        assertFalse(s.isNewBest)
        assertEquals(0, s.attemptsLeft)
    }

    @Test
    fun `lower-wins games treat the smaller score as the new best`() {
        val f = flow(FakeGame(higherWins = false))
        f.playTo(512)
        val s = f.playTo(427)
        assertEquals(427L, s.dailyBest)
        assertTrue(s.isNewBest)
    }

    @Test
    fun `equal score is not a new best`() {
        val f = flow()
        f.playTo(10)
        assertFalse(f.playTo(10).isNewBest)
    }

    @Test
    fun `a run without any score records nothing`() {
        val s = flow().playTo(null, win = false)
        assertNull(s.score)
        assertNull(s.dailyBest)
        assertFalse(s.isNewBest)
        assertNull(scores.best(day, "03-blink-sniper"))
    }

    @Test
    fun `scores before the run or after the finish are ignored`() {
        val f = flow()
        f.onScore(99)
        assertTrue(f.start())
        f.onScore(5) // still counting down
        repeat(COUNTDOWN_FROM) { f.tick() }
        f.onScore(8)
        f.onFinished(true)
        f.onScore(50)
        assertEquals(8L, (f.phase as SessionPhase.Finished).summary.score)
    }

    @Test
    fun `the run counts for the day it started even if it ends after noon`() {
        now = Instant.parse("2026-11-19T11:59:00Z").toEpochMilli()
        val startedDay = day
        val f = flow()
        assertTrue(f.start())
        repeat(COUNTDOWN_FROM) { f.tick() }
        f.onScore(30)
        now = Instant.parse("2026-11-19T12:00:30Z").toEpochMilli()
        f.onFinished(true)
        assertEquals(30L, scores.best(startedDay, "03-blink-sniper"))
        assertEquals(1, attempts.remaining(startedDay))
    }

    @Test
    fun `result carries the next reset for the locked countdown`() {
        val f = flow()
        f.playTo(1)
        val s = f.playTo(2)
        assertEquals(Instant.parse("2026-11-19T12:00:00Z").toEpochMilli(), s.nextResetMillis)
    }

    @Test
    fun `abandoning mid run records nothing and the attempt stays spent`() {
        val f = flow()
        assertTrue(f.start())
        repeat(COUNTDOWN_FROM) { f.tick() }
        f.onScore(40)
        f.abandon()
        assertEquals(SessionPhase.Abandoned, f.phase)
        assertNull(scores.best(day, "03-blink-sniper"))
        assertEquals(1, attempts.remaining(day))
    }

    @Test
    fun `play again from the result starts a new countdown`() {
        val f = flow()
        f.playTo(5)
        assertTrue(f.start())
        assertEquals(SessionPhase.Countdown(3), f.phase)
    }

    // ---- practice runs (debug launcher, a game that is not today's) ----

    private fun practice() = SessionFlow(FakeGame(), attempts, scores, clock, practice = true) { now }

    @Test
    fun `practice runs spend no attempt and record nothing`() {
        val s = practice().playTo(27)
        assertTrue(s.practice)
        assertEquals(2, attempts.remaining(day))
        assertNull(scores.best(day, "03-blink-sniper"))
        assertFalse(s.isNewBest)
        assertEquals(27L, s.score)
    }

    @Test
    fun `practice is allowed even with no attempts left`() {
        attempts.consume(day); attempts.consume(day)
        val f = practice()
        assertEquals(SessionPhase.Intro, f.phase)
        assertTrue(f.start())
    }

    // ---- personal best (all-time, across days) ----

    @Test
    fun `beating every earlier day is a personal best`() {
        scores.submit(day - 3, "03-blink-sniper", 20, higherWins = true)
        val s = flow().playTo(27)
        assertTrue(s.isPersonalBest)
        assertEquals(27L, s.personalBest)
    }

    @Test
    fun `a daily best below an earlier day is not a personal best`() {
        scores.submit(day - 3, "03-blink-sniper", 40, higherWins = true)
        val s = flow().playTo(27)
        assertTrue(s.isNewBest)
        assertFalse(s.isPersonalBest)
        assertEquals(40L, s.personalBest)
    }

    @Test
    fun `the very first score is not celebrated as a personal best`() {
        val s = flow().playTo(27)
        assertFalse(s.isPersonalBest)
        assertEquals(27L, s.personalBest)
    }
}
