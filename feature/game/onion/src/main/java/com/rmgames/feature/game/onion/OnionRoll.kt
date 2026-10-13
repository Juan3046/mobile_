package com.rmgames.feature.game.onion

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameScope

/** Game 02 metadata. Gameplay lands in Phase 1 (owner: P3). */
object OnionRollGame : Game {
    override val id = "02-onion-roll"
    override val title = "Onion Roll"
    override val description = "Tilt to steer the onion. Survive the belt."
    override val durationMs = 90_000L
    override val higherWins = true
    override fun start(scope: GameScope) = Unit
    override fun stop() = Unit
}

@Composable
fun OnionRollScreen() {
    Text("Onion Roll (Phase 1)")
}
