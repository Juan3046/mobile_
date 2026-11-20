package com.rmgames.feature.game.onion

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Plays [Cue]s as sound + vibration (docs/games/02-onion-roll.md — Audio &
 * Haptics). Everything — building the tracks, restarting them, vibrating — runs
 * on one dedicated background thread via [AsyncCuePlayer]: AudioTrack calls are
 * blocking binder calls, and on the main thread they froze the game loop. One
 * static [AudioTrack] per cue holds its synthesized PCM. Thin Android adapter: cue
 * rules, sounds and the hand-off are JVM-tested elsewhere. Call [release] when
 * the screen leaves.
 */
internal class OnionRollFeedback(context: Context) {
    /** The app's Sound effects / Vibration switches; set by the screen, read on the audio thread. */
    @Volatile var sfxEnabled: Boolean = true
    @Volatile var hapticsEnabled: Boolean = true

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "onion-roll-audio").apply { isDaemon = true }
    }
    private val player = AsyncCuePlayer(executor, ::playNow)

    // Only touched on the audio thread.
    private var tracks: Map<Cue, AudioTrack> = emptyMap()
    private var vibrator: Vibrator? = null

    init {
        executor.execute {
            tracks = Cue.entries.associateWith { buildTrack(synthesize(it)) }
            vibrator = context.getSystemService(VibratorManager::class.java)
                ?.defaultVibrator
                ?.takeIf { it.hasVibrator() }
        }
    }

    /** Non-blocking: queues [cue] for the audio thread. */
    fun play(cue: Cue) = player.play(cue)

    fun release() {
        runCatching {
            executor.execute {
                tracks.values.forEach { runCatching { it.release() } }
                tracks = emptyMap()
                runCatching { vibrator?.cancel() }
            }
        }
        executor.shutdown()
    }

    private fun playNow(cue: Cue) {
        val out = cueOutput(cue, sfxEnabled, hapticsEnabled)
        if (out.playSound) tracks[cue]?.let { track ->
            runCatching {
                track.stop()
                track.reloadStaticData()
                track.play()
            }
        }
        out.hapticMs?.let { ms ->
            // VIBRATE is declared in the module manifest; never crash the game over haptics.
            runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) }
        }
    }

    private fun buildTrack(pcm: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SOUND_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }
}
