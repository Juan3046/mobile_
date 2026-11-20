package com.rmgames.core.common.ui

import androidx.compose.ui.graphics.vector.ImageVector

/** The control a game is played with, for the GameCard chip. */
data class GameSensor(val label: String, val icon: ImageVector)

/**
 * Presentation metadata per game, taken from each game's spec in docs/games/ (the frozen
 * `Game` contract has no sensor field). Unknown games get no chip.
 */
fun gameSensor(gameId: String): GameSensor? = when (gameId.split('-').getOrNull(1)) {
    "poruba" -> GameSensor("Head gestures", RmIcons.Camera)
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
    "poruba" -> "5 hands"
    "soda" -> "20 shakes"
    else -> "${durationMs / 1000} seconds"
}

/** A score ready to show: the number and its unit ("27" "points", "4.27" "seconds"). */
data class ScoreText(val value: String, val unit: String)

/**
 * Formats a raw game score (what the game passed to `GameScope.onScore`) per its spec:
 * Poruba Casino = banana balance, Onion Roll = whole seconds survived, Soda Shake = centiseconds to pop (shown as
 * seconds), everything else = points.
 */
fun gameScoreText(gameId: String, raw: Long): ScoreText = when (gameId.split('-').getOrNull(1)) {
    "onion" -> ScoreText(formatPoints(raw), if (raw == 1L) "second" else "seconds")
    "poruba" -> ScoreText(formatPoints(raw), if (raw == 1L) "banana" else "bananas")
    "soda" -> ScoreText("%d.%02d".format(java.util.Locale.US, raw / 100, raw % 100), "seconds")
    else -> ScoreText(formatPoints(raw), if (raw == 1L) "point" else "points")
}
