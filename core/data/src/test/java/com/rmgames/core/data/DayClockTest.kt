package com.rmgames.core.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class DayClockTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val clock = DayClock(zone)

    private fun millis(date: String, time: String): Long {
        val local = java.time.LocalDate.parse(date).atTime(java.time.LocalTime.parse(time))
        return local.atZone(zone).toInstant().toEpochMilli()
    }

    @Test
    fun `morning belongs to previous day index`() {
        val before = clock.dayIndex(millis("2026-10-13", "11:59"))
        val after = clock.dayIndex(millis("2026-10-13", "12:00"))
        assertEquals(before + 1, after)
    }

    @Test
    fun `next reset from morning is noon today`() {
        val reset = clock.nextResetMillis(millis("2026-10-13", "10:00"))
        assertEquals(millis("2026-10-13", "12:00"), reset)
    }

    @Test
    fun `next reset from afternoon is noon tomorrow`() {
        val reset = clock.nextResetMillis(millis("2026-10-13", "13:00"))
        assertEquals(millis("2026-10-14", "12:00"), reset)
    }
}
