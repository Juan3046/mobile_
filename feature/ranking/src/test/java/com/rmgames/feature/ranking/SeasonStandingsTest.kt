package com.rmgames.feature.ranking

import com.rmgames.core.data.DailyBest
import com.rmgames.core.data.PlayedDay
import com.rmgames.core.data.Standing
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SeasonStandingsTest {

    private val wednesday = LocalDate.of(2026, 11, 18).toEpochDay()
    private val history = SeasonHistory.build(
        wednesday,
        listOf(DailyBest(wednesday - 1, "03-blink-sniper", 27), DailyBest(wednesday, "05-soda-shake", 400)),
        emptyMap(),
    ) { it != "05-soda-shake" }

    @Test
    fun `season points tile reads offline, checking, unknown or the total`() {
        assertEquals("—" to "online ranking off", seasonPointsTile(online = false, standings = null))
        assertEquals("…" to "checking", seasonPointsTile(online = true, standings = null))
        assertEquals("—" to "no connection", seasonPointsTile(online = true, standings = mapOf(1L to null)))
        assertEquals("0" to "play to score", seasonPointsTile(online = true, standings = emptyMap()))
        assertEquals("5,225" to "provisional", seasonPointsTile(true, mapOf(1L to Standing(1, 100, 2500), 2L to Standing(2, 110, 2725), 3L to null)))
    }

    @Test
    fun `a day's standing reads as position of players`() {
        assertEquals("#4 of 212", standingLabel(Standing(4, 212, 5225)))
    }
}
