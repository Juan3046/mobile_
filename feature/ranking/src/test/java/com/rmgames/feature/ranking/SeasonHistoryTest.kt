package com.rmgames.feature.ranking

import com.rmgames.core.data.DailyBest
import com.rmgames.core.data.SeasonCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SeasonHistoryTest {

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()
    private val titles = mapOf("05-soda-shake" to "Soda Shake", "03-blink-sniper" to "Blink Sniper", "02-onion-roll" to "Onion Roll")
    private val wednesday = day(2026, 11, 18)

    private fun build(today: Long, history: List<DailyBest>) = SeasonHistory.build(today, history, titles)

    @Test
    fun `current season lists its seven days with dates and states`() {
        val h = build(wednesday, emptyList())
        assertEquals(1, h.current.season)
        assertEquals("16 – 22 Nov", h.current.dateRange)
        assertEquals(7, h.current.days.size)
        assertEquals("Mon 16 Nov", h.current.days[0].dateLabel)
        assertEquals(listOf(DayState.Missed, DayState.Missed, DayState.Today, DayState.Upcoming, DayState.Upcoming, DayState.Upcoming, DayState.Upcoming),
            h.current.days.map { it.state })
    }

    @Test
    fun `played days carry the game and the best`() {
        val h = build(wednesday, listOf(DailyBest(day(2026, 11, 17), "03-blink-sniper", 27), DailyBest(wednesday, "05-soda-shake", 1132)))
        val tue = h.current.days[1]
        assertEquals(DayState.Played, tue.state)
        assertEquals("Blink Sniper", tue.gameTitle)
        assertEquals(27L, tue.best)
        val today = h.current.days[2]
        assertEquals(DayState.Today, today.state)
        assertEquals("Soda Shake", today.gameTitle)
        assertEquals(2, h.current.daysPlayed)
    }

    @Test
    fun `past seasons are summarised newest first, only if played`() {
        val h = build(
            day(2026, 12, 2), // season 3, day 3
            listOf(
                DailyBest(day(2026, 11, 17), "03-blink-sniper", 27),
                DailyBest(day(2026, 11, 18), "05-soda-shake", 1132),
                DailyBest(day(2026, 11, 25), "02-onion-roll", 62), // season 2 = 23–29 Nov
            ),
        )
        assertEquals(3, h.current.season)
        assertEquals(listOf(2, 1), h.past.map { it.season })
        assertEquals(1, h.past[0].daysPlayed)
        assertEquals("23 – 29 Nov", h.past[0].dateRange)
        assertEquals(2, h.past[1].daysPlayed)
    }

    @Test
    fun `seasons spanning months show both months`() {
        val h = build(day(2026, 11, 30), emptyList())
        assertEquals("30 Nov – 6 Dec", h.current.dateRange)
    }

    @Test
    fun `all-time totals count every played day and season`() {
        val h = build(day(2026, 12, 2), listOf(
            DailyBest(day(2026, 11, 17), "03-blink-sniper", 27),
            DailyBest(day(2026, 11, 18), "05-soda-shake", 1132),
            DailyBest(day(2026, 11, 30), "02-onion-roll", 62),
        ))
        assertEquals(3, h.allTime.daysPlayed)
        assertEquals(2, h.allTime.seasonsPlayed)
    }

    @Test
    fun `pre-launch plays are ignored`() {
        val h = build(wednesday, listOf(DailyBest(day(2026, 11, 10), "05-soda-shake", 900)))
        assertTrue(h.past.isEmpty())
        assertEquals(0, h.allTime.daysPlayed)
    }

    @Test
    fun `unknown game ids fall back to the id`() {
        val h = build(wednesday, listOf(DailyBest(wednesday, "99-new-game", 5)))
        assertEquals("99-new-game", h.current.days[2].gameTitle)
    }

    @Test
    fun `days not played have no game`() {
        assertNull(build(wednesday, emptyList()).current.days[0].gameTitle)
    }
}
