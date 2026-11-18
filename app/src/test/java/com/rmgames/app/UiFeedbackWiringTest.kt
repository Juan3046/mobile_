package com.rmgames.app

import com.rmgames.core.data.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiFeedbackWiringTest {

    @Test
    fun `button haptics follow the vibration setting`() {
        assertTrue(uiFeedbackFor(Settings(vibrationEnabled = true)).hapticsEnabled)
        assertFalse(uiFeedbackFor(Settings(vibrationEnabled = false)).hapticsEnabled)
    }
}
