package com.rmgames.feature.game.onion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Onion Roll's own sounds and vibrations obey the app's Settings (plan 09 §2B). */
class CueSettingsTest {

    @Test
    fun `everything on plays sound and the cue's vibration`() {
        Cue.entries.forEach { cue ->
            val out = cueOutput(cue, sfxEnabled = true, hapticsEnabled = true)
            assertTrue(out.playSound)
            assertEquals(hapticMs(cue), out.hapticMs)
        }
    }

    @Test
    fun `sound effects off silences cues but keeps vibration`() {
        Cue.entries.forEach { cue ->
            val out = cueOutput(cue, sfxEnabled = false, hapticsEnabled = true)
            assertFalse(out.playSound)
            assertEquals(hapticMs(cue), out.hapticMs)
        }
    }

    @Test
    fun `vibration off keeps sound but never vibrates`() {
        Cue.entries.forEach { cue ->
            val out = cueOutput(cue, sfxEnabled = true, hapticsEnabled = false)
            assertTrue(out.playSound)
            assertNull(out.hapticMs)
        }
    }
}
