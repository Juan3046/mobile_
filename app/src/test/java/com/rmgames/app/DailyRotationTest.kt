package com.rmgames.app

import com.rmgames.core.common.Game
import com.rmgames.core.common.GameScope
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyRotationTest {

    private class G(override val id: String) : Game {
        override val title = id
        override val description = ""
        override val durationMs = 60_000L
        override val higherWins = true
        override fun start(scope: GameScope) {}
        override fun stop() {}
    }

    private val all = listOf("01-escape-poruba", "02-onion-roll", "03-blink-sniper", "04-compass-chaos", "05-soda-shake").map(::G)

    @Test
    fun `games that are not ready stay out of the daily rotation`() {
        assertEquals(
            listOf("02-onion-roll", "03-blink-sniper", "04-compass-chaos", "05-soda-shake"),
            dailyRotation(all).map { it.id },
        )
    }

    @Test
    fun `rotation keeps registry order`() {
        val shuffled = listOf(all[4], all[1], all[3])
        assertEquals(shuffled.map { it.id }, dailyRotation(shuffled).map { it.id })
    }

    @Test
    fun `if nothing is ready the full list is used so home never breaks`() {
        val onlyEscape = listOf(all[0])
        assertEquals(onlyEscape, dailyRotation(onlyEscape))
    }
}
