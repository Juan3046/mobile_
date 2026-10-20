package com.rmgames.feature.game.soda

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import com.rmgames.core.sensors.AccelerometerSensor

/** Game 05 metadata. Gameplay lands in Phase 1b (owner: P4). */
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

/**
 * Frozen Phase 1b entry signature (docs/08-implementation-plan.md — Game Hosting
 * Contract): `:app` wires this once into the `GameRegistry`, so gameplay work never
 * touches shared navigation.
 *
 * @param accelerometer injected by `:app` (Android adapter in production,
 * `FakeAccelerometerSensor` in tests); the placeholder ignores it until gameplay lands.
 */
fun sodaShakeEntry(accelerometer: AccelerometerSensor): GameEntry =
    GameEntry(SodaShakeGame) { SodaShakeScreen() }
