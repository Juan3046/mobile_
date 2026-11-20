package com.rmgames.feature.game.rush

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

/** Vibration moments of a run (docs/games/06 "Feedback"). */
enum class RushBuzz { Hop, Crash }

/** What to buzz between two frames; null = nothing. A crash wins over a hop in the same frame. */
internal fun rushBuzz(before: RushState, after: RushState): RushBuzz? = when {
    after.phase == RushPhase.OVER && before.phase == RushPhase.PLAYING && after.end?.win == false -> RushBuzz.Crash
    after.hops > before.hops -> RushBuzz.Hop
    else -> null
}

/** End-of-run banner text. */
internal fun endMessage(s: RushState): String? = when (s.end) {
    RushEnd.HIT -> "Hit by traffic!"
    RushEnd.FELL -> "Into the pond!"
    RushEnd.TOO_SLOW -> "Too slow, the lecture started"
    RushEnd.TIME_UP -> "Time! ${s.score} ${if (s.score == 1) "row" else "rows"}"
    null -> null
}

/** "0:42" for the HUD. */
internal fun clockText(ms: Long): String {
    val seconds = (ms + 999) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

internal class RushVibrator(context: Context) {
    private val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator

    fun play(buzz: RushBuzz) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        v.vibrate(
            when (buzz) {
                RushBuzz.Hop -> VibrationEffect.createOneShot(15, 120)
                RushBuzz.Crash -> VibrationEffect.createOneShot(220, VibrationEffect.DEFAULT_AMPLITUDE)
            },
        )
    }
}
