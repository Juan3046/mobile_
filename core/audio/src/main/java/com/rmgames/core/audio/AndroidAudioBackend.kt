package com.rmgames.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.rmgames.core.common.ui.UiCue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Android side of [AudioDirector]. Every AudioTrack call runs on one dedicated
 * "rm-audio" thread — never the main thread (Onion Roll playtest: AudioTrack restarts
 * on the UI thread froze the game). Cues are static tracks rebuilt from [SoundBook];
 * the menu music is one static track looping forever, faded by volume steps.
 *
 * @param onFocusChange receives audio-focus changes on the main thread.
 */
class AndroidAudioBackend(
    context: Context,
    private val onFocusChange: (FocusState) -> Unit,
) : AudioBackend {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val executor = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "rm-audio").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())

    // Only touched on the audio thread.
    private var cues: Map<UiCue, AudioTrack> = emptyMap()
    private var music: AudioTrack? = null
    private var musicLevel = 0f
    private var fade: ScheduledFuture<*>? = null

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setWillPauseWhenDucked(false)
        .setOnAudioFocusChangeListener({ change ->
            val state = when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> FocusState.Gained
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> FocusState.Duck
                else -> FocusState.Lost
            }
            main.post { onFocusChange(state) }
        }, main)
        .build()

    init {
        executor.execute {
            runCatching {
                cues = UiCue.entries.associateWith { buildTrack(SoundBook.cue(it), loop = false) }
                music = buildTrack(SoundBook.menuLoop(), loop = true).apply { setVolume(0f) }
            }
        }
    }

    override fun playCue(cue: UiCue, volume: Float) = run {
        cues[cue]?.let {
            it.stop()
            it.reloadStaticData()
            it.setVolume(volume)
            it.play()
        }
    }

    override fun setMusic(volume: Float, fadeMs: Int) = run {
        fade?.cancel(false)
        val track = music ?: return@run
        val from = musicLevel
        val steps = (fadeMs / FADE_STEP_MS).coerceAtLeast(1)
        if (volume > 0f && track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()
        var step = 0
        fade = executor.scheduleWithFixedDelay({
            step++
            musicLevel = from + (volume - from) * step / steps
            runCatching { track.setVolume(musicLevel) }
            if (step >= steps) {
                if (volume <= 0f) runCatching { track.pause() }
                fade?.cancel(false)
            }
        }, 0, FADE_STEP_MS, TimeUnit.MILLISECONDS)
    }

    override fun requestFocus(): Boolean =
        audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    override fun abandonFocus() {
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    override fun release() {
        run {
            fade?.cancel(false)
            cues.values.forEach { it.release() }
            music?.release()
            cues = emptyMap()
            music = null
        }
        executor.shutdown()
    }

    /** Queues [block] on the audio thread; audio errors never reach the app. */
    private fun run(block: () -> Unit) {
        if (executor.isShutdown) return
        executor.execute { runCatching(block) }
    }

    private fun buildTrack(pcm: ShortArray, loop: Boolean): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(Synth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        if (loop) track.setLoopPoints(0, pcm.size, -1)
        return track
    }

    private companion object {
        const val FADE_STEP_MS = 30L
    }
}
