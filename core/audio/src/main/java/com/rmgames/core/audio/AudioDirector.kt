package com.rmgames.core.audio

import com.rmgames.core.common.ui.AudioScene
import com.rmgames.core.common.ui.UiCue

/** Audio focus as reported by the platform. */
enum class FocusState { Gained, Duck, Lost }

/** Music level while another app asks us to duck. */
const val DUCK_FACTOR = 0.3f

private const val FADE_MS = 250
private const val FADE_TO_GAME_MS = 450

/**
 * What the director needs from the platform. The Android implementation does all audio
 * work on its own thread; calls here must return immediately.
 */
interface AudioBackend {
    fun playCue(cue: UiCue, volume: Float)
    /** Fades the menu music to [volume] (0 = paused) over [fadeMs]. */
    fun setMusic(volume: Float, fadeMs: Int)
    /** Requests audio focus for music; false if denied. */
    fun requestFocus(): Boolean
    fun abandonFocus()
    fun release()
}

/**
 * App-wide audio rules (plan 09 §2B), pure and unit-tested:
 * - menu music only in [AudioScene.Menu], at the Settings volume (0 = off, no focus held);
 * - music fades out while a game runs (games keep their own sounds) and back after;
 * - background → music paused, cues muted; audio focus loss → silent, duck → [DUCK_FACTOR];
 * - cues obey the Sound effects switch.
 * Call it from the main thread; the backend never blocks it.
 */
class AudioDirector(private val backend: AudioBackend) {
    private var musicVolume = 0.7f
    private var sfxEnabled = true
    private var foreground = false
    private var scene = AudioScene.Silent
    private var focus = FocusState.Gained
    private var holdingFocus = false
    private var appliedVolume = 0f

    fun onSettings(musicVolume: Float, sfxEnabled: Boolean) {
        this.musicVolume = musicVolume.coerceIn(0f, 1f)
        this.sfxEnabled = sfxEnabled
        apply(FADE_MS)
    }

    fun onForeground(foreground: Boolean) {
        if (this.foreground == foreground) return
        this.foreground = foreground
        apply(FADE_MS)
    }

    fun setScene(scene: AudioScene) {
        if (this.scene == scene) return
        this.scene = scene
        apply(if (scene == AudioScene.Game) FADE_TO_GAME_MS else FADE_MS)
    }

    fun onFocusChange(state: FocusState) {
        focus = state
        apply(FADE_MS)
    }

    fun play(cue: UiCue) {
        if (foreground && sfxEnabled) backend.playCue(cue, 1f)
    }

    fun release() {
        if (holdingFocus) backend.abandonFocus()
        holdingFocus = false
        backend.release()
    }

    private fun apply(fadeMs: Int) {
        val wantMusic = foreground && scene == AudioScene.Menu && musicVolume > 0f
        if (wantMusic && !holdingFocus) {
            holdingFocus = backend.requestFocus()
            if (holdingFocus) focus = FocusState.Gained
        } else if (!wantMusic && holdingFocus) {
            backend.abandonFocus()
            holdingFocus = false
        }
        val target = if (!wantMusic || !holdingFocus) 0f else when (focus) {
            FocusState.Gained -> musicVolume
            FocusState.Duck -> musicVolume * DUCK_FACTOR
            FocusState.Lost -> 0f
        }
        if (target != appliedVolume) {
            appliedVolume = target
            backend.setMusic(target, fadeMs)
        }
    }
}
