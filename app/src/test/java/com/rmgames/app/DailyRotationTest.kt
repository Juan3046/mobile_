package com.rmgames.app

import com.rmgames.core.common.Game
import com.rmgames.core.common.GameScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private val all = listOf("01-poruba-casino", "02-onion-roll", "03-blink-sniper", "04-compass-chaos", "05-soda-shake").map(::G)

    @Test
    fun `every current game is playable, so all five rotate`() {
        assertTrue(NOT_READY_FOR_ROTATION.isEmpty())
        assertEquals(all.map { it.id }, dailyRotation(all).map { it.id })
    }

    @Test
    fun `games that are not ready stay out of the daily rotation`() {
        assertEquals(
            listOf("02-onion-roll", "03-blink-sniper", "04-compass-chaos", "05-soda-shake"),
            dailyRotation(all, notReady = setOf("01-poruba-casino")).map { it.id },
        )
    }

    @Test
    fun `if nothing is ready the full list is used so home never breaks`() {
        val one = listOf(all[0])
        assertEquals(one, dailyRotation(one, notReady = setOf("01-poruba-casino")))
    }
}
