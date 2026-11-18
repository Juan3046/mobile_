package com.rmgames.app

import com.rmgames.core.common.ui.AudioScene
import com.rmgames.core.common.ui.UiCue
import com.rmgames.core.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiFeedbackWiringTest {

    private val cues = mutableListOf<UiCue>()
    private val scenes = mutableListOf<AudioScene>()
    private fun feedback(settings: Settings) = uiFeedbackFor(settings, cues::add, scenes::add)

    @Test
    fun `button haptics follow the vibration setting`() {
        assertTrue(feedback(Settings(vibrationEnabled = true)).hapticsEnabled)
        assertFalse(feedback(Settings(vibrationEnabled = false)).hapticsEnabled)
    }

    @Test
    fun `button clicks and screen cues reach the audio layer`() {
        val f = feedback(Settings())
        f.click()
        f.cue(UiCue.NewBest)
        assertEquals(listOf(UiCue.Click, UiCue.NewBest), cues)
    }

    @Test
    fun `screens set the audio scene`() {
        feedback(Settings()).scene(AudioScene.Game)
        assertEquals(listOf(AudioScene.Game), scenes)
    }
}
