package com.rmgames.feature.game.soda

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameScope

/** Game 05 metadata. Gameplay lands in Phase 1 (owner: P4). */
object SodaShakeGame : Game {
    override val id = "05-soda-shake"
    override val title = "Soda Shake"
    override val description = "Shake 20 times to pop the can. Fastest wins."
    /** Variable round ending at 20 shakes; typical duration as placeholder. */
    override val durationMs = 10_000L
    override val higherWins = false
    override fun start(scope: GameScope) = Unit
    override fun stop() = Unit
}

@Composable
fun SodaShakeScreen() {
    Text("Soda Shake (Phase 1)")
}
