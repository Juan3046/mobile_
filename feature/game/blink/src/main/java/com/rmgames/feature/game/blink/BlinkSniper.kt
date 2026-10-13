package com.rmgames.feature.game.blink

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameScope

/** Game 03 metadata. Gameplay lands in Phase 1 (owner: P4). */
object BlinkSniperGame : Game {
    override val id = "03-blink-sniper"
    override val title = "Blink Sniper"
    override val description = "Blink to shoot targets. 60 seconds."
    override val durationMs = 60_000L
    override val higherWins = true
    override fun start(scope: GameScope) = Unit
    override fun stop() = Unit
}

@Composable
fun BlinkSniperScreen() {
    Text("Blink Sniper (Phase 1)")
}
