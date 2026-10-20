package com.rmgames.feature.game.onion

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.GyroscopeSensor

/** Game 02 metadata. Gameplay lands in Phase 1b (owner: P2). */
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

/**
 * Frozen Phase 1b entry signature (docs/08-implementation-plan.md — Game Hosting
 * Contract): `:app` wires this once into the `GameRegistry`, so gameplay work never
 * touches shared navigation.
 *
 * @param gyroscope injected by `:app` (Android adapter in production, `FakeGyroscopeSensor`
 * in tests); the placeholder ignores it until gameplay lands.
 */
fun onionRollEntry(gyroscope: GyroscopeSensor): GameEntry =
    GameEntry(OnionRollGame) { OnionRollScreen() }
