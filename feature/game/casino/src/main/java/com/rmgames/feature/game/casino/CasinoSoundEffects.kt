package com.rmgames.feature.game.casino

import android.media.AudioManager
import android.media.ToneGenerator

enum class CasinoSound {
    CARD_DEALT,
    PLAYER_HIT,
    PLAYER_STANDS,
    DEALER_DRAWS,
    WIN,
    LOSS,
    PUSH,
    BLACKJACK,
}

interface CasinoSoundPlayer {
    fun play(sound: CasinoSound)
    fun release()
}

object SilentCasinoSoundPlayer : CasinoSoundPlayer {
    override fun play(sound: CasinoSound) = Unit
    override fun release() = Unit
}

/** Short system-generated casino cues; no audio assets or recordings are stored. */
class ToneCasinoSoundPlayer : CasinoSoundPlayer {
    private val tones = ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)

    override fun play(sound: CasinoSound) {
        val (tone, durationMs) = when (sound) {
            CasinoSound.CARD_DEALT -> ToneGenerator.TONE_PROP_BEEP to 75
            CasinoSound.PLAYER_HIT -> ToneGenerator.TONE_PROP_ACK to 130
            CasinoSound.PLAYER_STANDS -> ToneGenerator.TONE_PROP_PROMPT to 180
            CasinoSound.DEALER_DRAWS -> ToneGenerator.TONE_PROP_BEEP to 110
            CasinoSound.WIN -> ToneGenerator.TONE_PROP_ACK to 320
            CasinoSound.LOSS -> ToneGenerator.TONE_PROP_NACK to 320
            CasinoSound.PUSH -> ToneGenerator.TONE_PROP_PROMPT to 220
            CasinoSound.BLACKJACK -> ToneGenerator.TONE_PROP_ACK to 500
        }
        tones.startTone(tone, durationMs)
    }

    override fun release() {
        tones.release()
    }

    private companion object {
        const val VOLUME = 70
    }
}
