package com.rmgames.feature.game.blink

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import com.rmgames.core.vision.BlinkDetector

/** Game 03 metadata. Gameplay lands in Phase 1b (owner: P3). */
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

/**
 * Frozen Phase 1b entry signature (docs/08-implementation-plan.md — Game Hosting
 * Contract): `:app` wires this once into the `GameRegistry`, so gameplay work never
 * touches shared navigation.
 *
 * @param detector injected by `:app` (`MlKitBlinkDetector` in production,
 * `FakeBlinkDetector` in tests); the placeholder ignores it until gameplay lands.
 */
fun blinkSniperEntry(detector: BlinkDetector): GameEntry =
    GameEntry(BlinkSniperGame) { BlinkSniperScreen() }
