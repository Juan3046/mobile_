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
    "class" -> GameSensor("Tap & swipe", RmIcons.Run)
    "sound" -> GameSensor("Microphone", RmIcons.Volume)
    else -> null
}

/** Short instructions shown before a run (03 §3.3 intro): a few steps plus the goal. */
data class HowTo(val steps: List<String>, val goal: String)

/**
 * How to play each game, in a few short lines (from each spec in docs/games/). Shown on the
 * "How to play" screen before Start. Null for unknown games (the one-line description stays).
 */
fun gameHowTo(gameId: String): HowTo? = when (gameId.split('-').getOrNull(1)) {
    "poruba" -> HowTo(
        listOf(
            "Keep your face in view of the front camera.",
            "Nod to take a card (hit). Shake your head to stay (stand).",
            "Get closer to 21 than the dealer without going over. Each hand bets 10 bananas.",
            "Camera acting up? The HIT and STAND buttons always work too.",
        ),
        goal = "Finish the 5 hands with as many bananas as you can.",
    )
    "onion" -> HowTo(
        listOf(
            "Hold the phone in front of you.",
            "Tilt it left or right to steer the onion along the belt.",
            "Dodge the fruit: every hit pushes you towards the edge.",
        ),
        goal = "Stay on the belt as long as you can. It keeps speeding up.",
    )
    "blink" -> HowTo(
        listOf(
            "Keep your face in view of the front camera.",
            "Blink to shoot at the crosshair as targets pass through it.",
            "Centre hit = 2 points, ring = 1. Never shoot a bomb!",
        ),
        goal = "Score as many points as you can in 60 seconds.",
    )
    "compass" -> HowTo(
        listOf(
            "Hold the phone in front of you, top edge pointing ahead.",
            "Turn your whole body to face the direction called (N, E, S or W).",
            "Be quick: each call only lasts a moment.",
        ),
        goal = "Face as many directions as you can in 20 seconds.",
    )
    "soda" -> HowTo(
        listOf(
            "Hold the phone firmly.",
            "Shake it any way you like. Every shake counts and buzzes.",
            "20 shakes pop the can.",
        ),
        goal = "Pop the can as fast as you can.",
    )
    "class" -> HowTo(
        listOf(
            "Tap anywhere to hop forward one row.",
            "Swipe left or right to step aside, swipe down to hop back.",
            "Dodge cars and trams (a red light warns). On the pond, stay on the ice.",
            "Don't stand still: 8 seconds without progress ends the run.",
        ),
        goal = "Cross as many rows as you can in 60 seconds.",
    )
    "sound" -> HowTo(
        listOf(
            "Make a loud sound (hum, shout, clap) to rise past the line. Go quiet to drop.",
            "Fly through each gap: every barrier you clear is +1 point.",
            "Bars push you back. Pushed off the left edge, the run ends.",
        ),
        goal = "Pass as many barriers as you can in 60 seconds.",
    )
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
    "class" -> ScoreText(formatPoints(raw), if (raw == 1L) "row" else "rows")
    "soda" -> ScoreText("%d.%02d".format(java.util.Locale.US, raw / 100, raw % 100), "seconds")
    else -> ScoreText(formatPoints(raw), if (raw == 1L) "point" else "points")
}
