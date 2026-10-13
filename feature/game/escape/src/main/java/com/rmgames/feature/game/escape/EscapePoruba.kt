package com.rmgames.feature.game.escape

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameScope

/** Game 01 metadata. Gameplay lands in Phase 1 (owner: P3). */
object EscapePorubaGame : Game {
    override val id = "01-escape-poruba"
    override val title = "Escape Poruba"
    override val description = "Drag to dodge. Survive 90 seconds."
    override val durationMs = 90_000L
    override val higherWins = true
    override fun start(scope: GameScope) = Unit
    override fun stop() = Unit
}

@Composable
fun EscapePorubaScreen() {
    Text("Escape Poruba (Phase 1)")
}
