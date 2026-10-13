package com.rmgames.feature.game.compass

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameScope

/** Game 04 metadata. Gameplay lands in Phase 1 (owner: P4). */
object CompassChaosGame : Game {
    override val id = "04-compass-chaos"
    override val title = "Compass Chaos"
    override val description = "Face the called direction. 20 seconds."
    override val durationMs = 20_000L
    override val higherWins = true
    override fun start(scope: GameScope) = Unit
    override fun stop() = Unit
}

@Composable
fun CompassChaosScreen() {
    Text("Compass Chaos (Phase 1)")
}
