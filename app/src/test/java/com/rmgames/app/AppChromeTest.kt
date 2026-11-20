package com.rmgames.app

import androidx.compose.ui.graphics.toArgb
import com.rmgames.core.common.theme.RmPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppChromeTest {

    @Test
    fun `splash stays until the persisted state is loaded`() {
        assertTrue(AppChrome.shouldKeepSplash(isLoaded = false))
        assertFalse(AppChrome.shouldKeepSplash(isLoaded = true))
    }

    @Test
    fun `bar scrims are the RmTheme backgrounds`() {
        assertEquals(RmPalette.Light.background.toArgb(), AppChrome.LIGHT_BAR_SCRIM)
        assertEquals(RmPalette.Dark.background.toArgb(), AppChrome.DARK_BAR_SCRIM)
    }

    @Test
    fun `bar icons are light on the dark theme and dark on the light theme`() {
        assertTrue(AppChrome.lightBarIcons(darkTheme = true))
        assertFalse(AppChrome.lightBarIcons(darkTheme = false))
    }

    @Test
    fun `window background colour resources match the palette`() {
        assertEquals(argbHex(RmPalette.Light.background.toArgb()), rmBackground("values"))
        assertEquals(argbHex(RmPalette.Dark.background.toArgb()), rmBackground("values-night"))
    }

    private fun rmBackground(dir: String): String {
        val text = File("src/main/res/$dir/colors.xml").readText()
        val match = Regex("""<color name="rm_background">(#[0-9A-Fa-f]{8})</color>""").find(text)
        return requireNotNull(match) { "rm_background missing in $dir" }.groupValues[1].uppercase()
    }

    private fun argbHex(argb: Int) = "#%08X".format(argb)
}
