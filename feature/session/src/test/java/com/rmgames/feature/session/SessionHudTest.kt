package com.rmgames.feature.session

import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Host placeholder texts vs games that draw their own HUD (docs/08 — own-HUD opt-out). */
class SessionHudTest {

    private val game = object : Game {
        override val id = "fake"
        override val title = "Fake"
        override val description = "Test"
        override val durationMs = 30_000L
        override val higherWins = true
        override fun start(scope: GameScope) = Unit
        override fun stop() = Unit
    }

    @Test
    fun `the host shows its score and finish texts by default`() {
        assertTrue(hostHudVisible(GameEntry(game) { }))
    }

    @Test
    fun `the host hides its texts for a game that draws its own HUD`() {
        assertFalse(hostHudVisible(GameEntry(game, drawsOwnHud = true) { }))
    }
}
