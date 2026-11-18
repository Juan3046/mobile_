package com.rmgames.core.common.ui

import androidx.compose.ui.graphics.vector.ImageVector

/** The control a game is played with, for the GameCard chip. */
data class GameSensor(val label: String, val icon: ImageVector)

/**
 * Presentation metadata per game, taken from each game's spec in docs/games/ (the frozen
 * `Game` contract has no sensor field). Unknown games get no chip.
 */
fun gameSensor(gameId: String): GameSensor? = when (gameId.split('-').getOrNull(1)) {
    "escape" -> GameSensor("Touch drag", RmIcons.Run)
    "onion" -> GameSensor("Tilt", RmIcons.Tilt)
    "blink" -> GameSensor("Front camera", RmIcons.Camera)
    "compass" -> GameSensor("Compass", RmIcons.Compass)
    "soda" -> GameSensor("Shake", RmIcons.Shake)
    else -> null
}

/**
 * Duration label for the GameCard. Documented exceptions (docs/02): Onion Roll is endless,
 * Soda Shake ends at 20 shakes; every other game shows its fixed length.
 */
fun gameDurationLabel(gameId: String, durationMs: Long): String = when (gameId.split('-').getOrNull(1)) {
    "onion" -> "Endless"
    "soda" -> "20 shakes"
    else -> "${durationMs / 1000} seconds"
}
