package com.rmgames.feature.game.compass

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.CompassSensor

/** Game 04 metadata. Gameplay lands in Phase 1b (owner: P4). */
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

/**
 * Frozen Phase 1b entry signature (docs/08-implementation-plan.md — Game Hosting
 * Contract): `:app` wires this once into the `GameRegistry`, so gameplay work never
 * touches shared navigation.
 *
 * @param compass injected by `:app` (Android adapter in production, `FakeCompassSensor`
 * in tests); the placeholder ignores it until gameplay lands.
 */
fun compassChaosEntry(compass: CompassSensor): GameEntry =
    GameEntry(CompassChaosGame) { CompassChaosScreen() }
