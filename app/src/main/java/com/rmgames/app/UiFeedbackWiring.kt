package com.rmgames.app

import com.rmgames.core.common.ui.AudioScene
import com.rmgames.core.common.ui.UiCue
import com.rmgames.core.common.ui.UiFeedback
import com.rmgames.core.data.Settings

/**
 * Feedback for every screen, driven by the user's Settings: haptics follow the Vibration
 * toggle; cues and scenes go to the audio layer ([play]/[scene] — the AudioDirector in
 * the app, which applies the Sound effects switch and the music volume itself).
 */
internal fun uiFeedbackFor(
    settings: Settings,
    play: (UiCue) -> Unit,
    scene: (AudioScene) -> Unit,
): UiFeedback = object : UiFeedback {
    override val hapticsEnabled: Boolean = settings.vibrationEnabled
    override val sfxEnabled: Boolean = settings.sfxEnabled
    override fun cue(cue: UiCue) = play(cue)
    override fun scene(scene: AudioScene) = scene(scene)
}
