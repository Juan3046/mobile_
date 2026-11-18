package com.rmgames.app

import com.rmgames.core.common.ui.UiFeedback
import com.rmgames.core.data.Settings

/**
 * Click feedback for the UI kit, driven by the user's Settings: haptics follow the
 * Vibration toggle. The click sound joins here when `:core:audio` lands (plan 09 PR 7).
 */
internal fun uiFeedbackFor(settings: Settings): UiFeedback = object : UiFeedback {
    override val hapticsEnabled: Boolean = settings.vibrationEnabled
}
