package com.rmgames.feature.game.soda

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

/** Vibration feedback of a run (docs/games/05 — Feedback). */
enum class ShakeBuzz { Shake, Pop }

/** What to buzz when the shake count goes from [previous] to [now]; null = nothing. */
internal fun shakeBuzz(previous: Int, now: Int): ShakeBuzz? = when {
    now <= previous -> null
    now >= SodaShakeEngine.SHAKES_TO_POP -> ShakeBuzz.Pop
    else -> ShakeBuzz.Shake
}

/**
 * Plays [ShakeBuzz]es on the default vibrator. Short and light per shake — far below the
 * shake threshold, so the buzz itself never counts as a shake.
 */
internal class SodaShakeVibrator(context: Context) {
    private val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator

    fun play(buzz: ShakeBuzz) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        v.vibrate(
            when (buzz) {
                ShakeBuzz.Shake -> VibrationEffect.createOneShot(SHAKE_MS, SHAKE_AMPLITUDE)
                ShakeBuzz.Pop -> VibrationEffect.createOneShot(POP_MS, VibrationEffect.DEFAULT_AMPLITUDE)
            },
        )
    }

    private companion object {
        const val SHAKE_MS = 25L
        const val SHAKE_AMPLITUDE = 160
        const val POP_MS = 220L
    }
}
