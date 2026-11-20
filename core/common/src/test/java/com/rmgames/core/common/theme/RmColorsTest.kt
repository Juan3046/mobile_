package com.rmgames.core.common.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RmColorsTest {

    private val palettes = listOf(RmPalette.Dark, RmPalette.Light)

    @Test
    fun `every declared pair meets its WCAG minimum in both themes`() {
        val failures = palettes.flatMap { colors ->
            colors.contrastPairs().filter { Contrast.ratio(it.foreground, it.background) < it.minimum }
                .map { "${if (colors.isDark) "dark" else "light"} ${it.name}: " +
                    "%.2f < %.1f".format(Contrast.ratio(it.foreground, it.background), it.minimum) }
        }
        assertTrue("Contrast failures:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `contrast pairs cover every fill role and its on-role`() {
        val names = RmPalette.Dark.contrastPairs().map { it.name }.toSet()
        listOf(
            "onPrimary/primary", "onPrimaryContainer/primaryContainer",
            "onSecondary/secondary", "onSecondaryContainer/secondaryContainer",
            "onTertiary/tertiary", "onTertiaryContainer/tertiaryContainer",
            "onError/error", "onErrorContainer/errorContainer", "onSuccess/success",
            "onBackground/background", "onSurface/surface", "onSurfaceVariant/surface",
            "onSurfaceVariant/surfaceContainerHighest", "inverseOnSurface/inverseSurface",
            "primaryInk/surface", "primaryInk/surfaceContainer", "outline/surface",
            "onGame/gameBlink",
        ).forEach { assertTrue("missing pair $it", it in names) }
    }

    @Test
    fun `dark and light flags are set`() {
        assertTrue(RmPalette.Dark.isDark)
        assertFalse(RmPalette.Light.isDark)
    }

    @Test
    fun `palettes match the Direction A design-system values`() {
        // design-system/styles.css is the spec — guard against drift.
        assertEquals(Color(0xFFFF5D8F), RmPalette.Dark.primary)
        assertEquals(Color(0xFF17121F), RmPalette.Dark.background)
        assertEquals(Color(0xFFFFD23F), RmPalette.Dark.tertiary)
        assertEquals(Color(0xFF07050A), RmPalette.Dark.ledge)
        assertEquals(Color(0xFFD61F5F), RmPalette.Light.primary)
        assertEquals(Color(0xFFFBF7FF), RmPalette.Light.background)
        assertEquals(Color(0xFF1C1524), RmPalette.Light.ledge)
        assertEquals(Color(0xFF6FE3D0), RmPalette.Dark.gameBlink)
    }

    @Test
    fun `game accents are shared by both themes`() {
        with(RmPalette) {
            assertEquals(Dark.gameCasino, Light.gameCasino)
            assertEquals(Dark.gameOnion, Light.gameOnion)
            assertEquals(Dark.gameBlink, Light.gameBlink)
            assertEquals(Dark.gameCompass, Light.gameCompass)
            assertEquals(Dark.gameSoda, Light.gameSoda)
        }
    }

    @Test
    fun `material scheme carries the M3 roles`() {
        val dark = RmPalette.Dark.toMaterialColorScheme()
        assertEquals(RmPalette.Dark.primary, dark.primary)
        assertEquals(RmPalette.Dark.onPrimary, dark.onPrimary)
        assertEquals(RmPalette.Dark.secondaryContainer, dark.secondaryContainer)
        assertEquals(RmPalette.Dark.tertiary, dark.tertiary)
        assertEquals(RmPalette.Dark.surfaceContainerHighest, dark.surfaceContainerHighest)
        assertEquals(RmPalette.Dark.outlineVariant, dark.outlineVariant)
        assertEquals(RmPalette.Dark.inversePrimary, dark.inversePrimary)
        // Flat surfaces: no M3 tint overlay on elevated surfaces (the ledge carries depth).
        assertEquals(RmPalette.Dark.surface, dark.surfaceTint)

        val light = RmPalette.Light.toMaterialColorScheme()
        assertEquals(RmPalette.Light.background, light.background)
        assertEquals(RmPalette.Light.onSurfaceVariant, light.onSurfaceVariant)
    }
}
