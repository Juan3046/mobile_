package com.rmgames.core.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit

private val LocalRmColors = staticCompositionLocalOf { RmPalette.Dark }
private val LocalRmDimens = staticCompositionLocalOf { RmDimens() }
private val LocalRmMotion = staticCompositionLocalOf { RmMotion() }

/**
 * App theme — Direction A "Chunky Cabinet" (docs/09-phase2-shell-plan.md §2A,
 * spec in design-system/). Wraps [MaterialTheme] so stock M3 components pick up the
 * palette, type scale and shapes, and exposes the extra tokens through [RmTheme].
 * Dark-first: follows the system setting, dark palette when unknown.
 */
@Composable
fun RmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) RmPalette.Dark else RmPalette.Light
    CompositionLocalProvider(
        LocalRmColors provides colors,
        LocalRmDimens provides RmDimens(),
        LocalRmMotion provides RmMotion(),
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = rmTypography(display = RmFonts.Display, body = RmFonts.Body),
            shapes = RmShapes,
            content = content,
        )
    }
}

/** Accessors, mirroring `MaterialTheme.colorScheme` & co. */
object RmTheme {
    val colors: RmColors
        @Composable @ReadOnlyComposable get() = LocalRmColors.current

    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography

    val dimens: RmDimens
        @Composable @ReadOnlyComposable get() = LocalRmDimens.current

    val motion: RmMotion
        @Composable @ReadOnlyComposable get() = LocalRmMotion.current

    /** Big score / count style (Bungee, tabular). */
    fun score(size: TextUnit): TextStyle = rmNumberStyle(RmFonts.Display, size)

    /** Small tabular figures for timers and readouts (JetBrains Mono). */
    fun numbers(size: TextUnit): TextStyle = rmNumberStyle(RmFonts.Numbers, size)

    /** Font families, for the rare custom style. */
    val displayFamily: FontFamily get() = RmFonts.Display
    val bodyFamily: FontFamily get() = RmFonts.Body
}
