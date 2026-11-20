package com.rmgames.core.audio

import com.rmgames.core.common.ui.AudioScene
import com.rmgames.core.common.ui.UiCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioDirectorTest {

    /** Records what the director asked the platform to do. */
    private class FakeBackend : AudioBackend {
        val played = mutableListOf<UiCue>()
        var musicVolume = 0f
        var musicRunning = false
        var focusHeld = false
        var grantFocus = true
        override fun playCue(cue: UiCue, volume: Float) { played += cue }
        override fun setMusic(volume: Float, fadeMs: Int) {
            musicVolume = volume
            musicRunning = volume > 0f
        }
        override fun requestFocus(): Boolean { focusHeld = grantFocus; return grantFocus }
        override fun abandonFocus() { focusHeld = false }
        override fun release() {}
    }

    private val backend = FakeBackend()
    private val director = AudioDirector(backend).apply {
        onSettings(musicVolume = 0.7f, sfxEnabled = true)
        onForeground(true)
    }

    @Test
    fun `menu scene plays music at the settings volume`() {
        director.setScene(AudioScene.Menu)
        assertEquals(0.7f, backend.musicVolume, 0.001f)
        assertTrue(backend.focusHeld)
    }

    @Test
    fun `music fades out while a game runs and comes back after`() {
        director.setScene(AudioScene.Menu)
        director.setScene(AudioScene.Game)
        assertEquals(0f, backend.musicVolume, 0.001f)
        director.setScene(AudioScene.Menu)
        assertEquals(0.7f, backend.musicVolume, 0.001f)
    }

    @Test
    fun `music volume zero means no music and no audio focus`() {
        director.setScene(AudioScene.Menu)
        director.onSettings(musicVolume = 0f, sfxEnabled = true)
        assertFalse(backend.musicRunning)
        assertFalse(backend.focusHeld)
    }

    @Test
    fun `background pauses music and blocks cues`() {
        director.setScene(AudioScene.Menu)
        director.onForeground(false)
        assertEquals(0f, backend.musicVolume, 0.001f)
        director.play(UiCue.Click)
        assertTrue(backend.played.isEmpty())
        director.onForeground(true)
        assertEquals(0.7f, backend.musicVolume, 0.001f)
    }

    @Test
    fun `cues respect the sound effects switch`() {
        director.play(UiCue.Click)
        director.onSettings(musicVolume = 0.7f, sfxEnabled = false)
        director.play(UiCue.NewBest)
        assertEquals(listOf(UiCue.Click), backend.played)
    }

    @Test
    fun `losing audio focus silences music, ducking lowers it`() {
        director.setScene(AudioScene.Menu)
        director.onFocusChange(FocusState.Lost)
        assertEquals(0f, backend.musicVolume, 0.001f)
        director.onFocusChange(FocusState.Duck)
        assertEquals(0.7f * DUCK_FACTOR, backend.musicVolume, 0.001f)
        director.onFocusChange(FocusState.Gained)
        assertEquals(0.7f, backend.musicVolume, 0.001f)
    }

    @Test
    fun `denied focus keeps the music off`() {
        backend.grantFocus = false
        director.setScene(AudioScene.Menu)
        assertFalse(backend.musicRunning)
    }

    @Test
    fun `silent scene stops music`() {
        director.setScene(AudioScene.Menu)
        director.setScene(AudioScene.Silent)
        assertFalse(backend.musicRunning)
    }

    @Test
    fun `repeating the same state does not spam the backend`() {
        var calls = 0
        val counting = object : AudioBackend by backend {
            override fun setMusic(volume: Float, fadeMs: Int) { calls++; backend.setMusic(volume, fadeMs) }
        }
        val d = AudioDirector(counting).apply { onSettings(0.7f, true); onForeground(true) }
        d.setScene(AudioScene.Menu)
        val after = calls
        d.setScene(AudioScene.Menu)
        d.onForeground(true)
        assertEquals(after, calls)
    }
}
