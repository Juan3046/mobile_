package com.rmgames.core.common.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * RM Games colour roles — Direction A "Chunky Cabinet" (design-system/styles.css is the spec).
 *
 * Material 3 roles plus the app's extras:
 * - [primaryInk]: text-safe primary for labels/links on surfaces.
 * - [success]: positive feedback.
 * - [ledge]: the solid "hard ledge" offset under buttons and cards (the direction's depth).
 * - `game*` + [onGame]: per-game accents, only for the thumbnail fallback and the game chip.
 *
 * Rules: primary is the only interaction family; tertiary (sun yellow) is reserved for
 * celebration ("new best"). Every fill ships its `on*` partner — see [contrastPairs].
 */
@Immutable
data class RmColors(
    val isDark: Boolean,
    val primary: Color, val onPrimary: Color,
    val primaryContainer: Color, val onPrimaryContainer: Color,
    val primaryInk: Color,
    val secondary: Color, val onSecondary: Color,
    val secondaryContainer: Color, val onSecondaryContainer: Color,
    val tertiary: Color, val onTertiary: Color,
    val tertiaryContainer: Color, val onTertiaryContainer: Color,
    val error: Color, val onError: Color,
    val errorContainer: Color, val onErrorContainer: Color,
    val success: Color, val onSuccess: Color,
    val background: Color, val onBackground: Color,
    val surface: Color, val onSurface: Color, val onSurfaceVariant: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val outline: Color, val outlineVariant: Color,
    val inverseSurface: Color, val inverseOnSurface: Color, val inversePrimary: Color,
    val ledge: Color,
    val scrim: Color = Color(0x8F000000),
    val gameCasino: Color = GameAccents.Casino,
    val gameOnion: Color = GameAccents.Onion,
    val gameBlink: Color = GameAccents.Blink,
    val gameCompass: Color = GameAccents.Compass,
    val gameSoda: Color = GameAccents.Soda,
    val gameRush: Color = GameAccents.Rush,
    val gameSound: Color = GameAccents.Sound,
    val onGame: Color = GameAccents.OnGame,
) {
    /** A foreground/background pair and the WCAG minimum it must meet. */
    data class ContrastPair(val name: String, val foreground: Color, val background: Color, val minimum: Double)

    /**
     * Every pair the UI relies on. Unit-tested so a palette tweak can never ship
     * unreadable text. Fills meant as pure decoration (e.g. the yellow "new best" stamp on
     * the light ground) are not listed: they always carry the dark outline.
     */
    fun contrastPairs(): List<ContrastPair> {
        fun text(name: String, fg: Color, bg: Color) = ContrastPair(name, fg, bg, Contrast.TEXT)
        fun ui(name: String, fg: Color, bg: Color) = ContrastPair(name, fg, bg, Contrast.UI)
        return listOf(
            text("onPrimary/primary", onPrimary, primary),
            text("onPrimaryContainer/primaryContainer", onPrimaryContainer, primaryContainer),
            text("onSecondary/secondary", onSecondary, secondary),
            text("onSecondaryContainer/secondaryContainer", onSecondaryContainer, secondaryContainer),
            text("onTertiary/tertiary", onTertiary, tertiary),
            text("onTertiaryContainer/tertiaryContainer", onTertiaryContainer, tertiaryContainer),
            text("onError/error", onError, error),
            text("onErrorContainer/errorContainer", onErrorContainer, errorContainer),
            text("onSuccess/success", onSuccess, success),
            text("onBackground/background", onBackground, background),
            text("onSurface/surface", onSurface, surface),
            text("onSurfaceVariant/surface", onSurfaceVariant, surface),
            text("onSurfaceVariant/surfaceContainerHighest", onSurfaceVariant, surfaceContainerHighest),
            text("onSurface/surfaceContainerHighest", onSurface, surfaceContainerHighest),
            text("inverseOnSurface/inverseSurface", inverseOnSurface, inverseSurface),
            text("primaryInk/surface", primaryInk, surface),
            text("primaryInk/surfaceContainer", primaryInk, surfaceContainer),
            ui("outline/surface", outline, surface),
            ui("primary/surface", primary, surface),
            ui("error/surface", error, surface),
            ui("success/surface", success, surface),
            text("onGame/gameCasino", onGame, gameCasino),
            text("onGame/gameOnion", onGame, gameOnion),
            text("onGame/gameBlink", onGame, gameBlink),
            text("onGame/gameCompass", onGame, gameCompass),
            text("onGame/gameSoda", onGame, gameSoda),
            text("onGame/gameRush", onGame, gameRush),
            text("onGame/gameSound", onGame, gameSound),
        )
    }

    /**
     * Material 3 scheme for stock components. `surfaceTint = surface` turns off M3's
     * tonal elevation overlay: in this direction depth comes from the [ledge].
     */
    fun toMaterialColorScheme(): ColorScheme {
        // Start from the stock scheme, then pin every role by name (no positional drift).
        val base = if (isDark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary, onSecondary = onSecondary,
            secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary, onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceContainerHighest, onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surface,
            inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
            error = error, onError = onError,
            errorContainer = errorContainer, onErrorContainer = onErrorContainer,
            outline = outline, outlineVariant = outlineVariant, scrim = scrim,
            surfaceBright = surfaceContainerHigh, surfaceDim = surfaceContainerLowest,
            surfaceContainerLowest = surfaceContainerLowest, surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
        )
    }
}

/** Per-game accent colours (content colour, never interaction). Shared by both themes. */
object GameAccents {
    val Casino = Color(0xFF8FB8FF)
    val Onion = Color(0xFFC9A7FF)
    val Blink = Color(0xFF6FE3D0)
    val Compass = Color(0xFFB5E36F)
    val Soda = Color(0xFFFFA06B)
    val Rush = Color(0xFFFFB3C7)
    val Sound = Color(0xFFA8C5FF)
    val OnGame = Color(0xFF10131A)
}

/** The two Direction A palettes. Values mirror design-system/styles.css. */
object RmPalette {
    val Dark = RmColors(
        isDark = true,
        primary = Color(0xFFFF5D8F), onPrimary = Color(0xFF2B0011),
        primaryContainer = Color(0xFF5E1430), onPrimaryContainer = Color(0xFFFFD9E2),
        primaryInk = Color(0xFFFF8AAE),
        secondary = Color(0xFFB8A6D9), onSecondary = Color(0xFF22163A),
        secondaryContainer = Color(0xFF3A2D52), onSecondaryContainer = Color(0xFFE6DBFA),
        tertiary = Color(0xFFFFD23F), onTertiary = Color(0xFF241A00),
        tertiaryContainer = Color(0xFF4A3A00), onTertiaryContainer = Color(0xFFFFE68F),
        error = Color(0xFFFF6B5E), onError = Color(0xFF3A0500),
        errorContainer = Color(0xFF6B1A12), onErrorContainer = Color(0xFFFFDAD4),
        success = Color(0xFF7BE38A), onSuccess = Color(0xFF04310B),
        background = Color(0xFF17121F), onBackground = Color(0xFFF6F1FA),
        surface = Color(0xFF17121F), onSurface = Color(0xFFF6F1FA), onSurfaceVariant = Color(0xFFC3B8CF),
        surfaceContainerLowest = Color(0xFF110D18),
        surfaceContainerLow = Color(0xFF1E1828),
        surfaceContainer = Color(0xFF241D30),
        surfaceContainerHigh = Color(0xFF2D2539),
        surfaceContainerHighest = Color(0xFF372E45),
        outline = Color(0xFF8E829C), outlineVariant = Color(0xFF4A4058),
        inverseSurface = Color(0xFFF6F1FA), inverseOnSurface = Color(0xFF2D2539), inversePrimary = Color(0xFFB0245A),
        ledge = Color(0xFF07050A),
    )

    val Light = RmColors(
        isDark = false,
        primary = Color(0xFFD61F5F), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD9E2), onPrimaryContainer = Color(0xFF3F0019),
        primaryInk = Color(0xFFB0154D),
        secondary = Color(0xFF5E4C80), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE9DDFF), onSecondaryContainer = Color(0xFF1E1236),
        tertiary = Color(0xFFFFD23F), onTertiary = Color(0xFF241A00),
        tertiaryContainer = Color(0xFFFFE68F), onTertiaryContainer = Color(0xFF241A00),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        success = Color(0xFF1C7A35), onSuccess = Color(0xFFFFFFFF),
        background = Color(0xFFFBF7FF), onBackground = Color(0xFF1C1524),
        surface = Color(0xFFFBF7FF), onSurface = Color(0xFF1C1524), onSurfaceVariant = Color(0xFF4D4359),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF5EFFA),
        surfaceContainer = Color(0xFFEFE8F5),
        surfaceContainerHigh = Color(0xFFE8E0EF),
        surfaceContainerHighest = Color(0xFFE1D8EA),
        outline = Color(0xFF7E7389), outlineVariant = Color(0xFFCFC5DA),
        inverseSurface = Color(0xFF322A3A), inverseOnSurface = Color(0xFFF6F1FA), inversePrimary = Color(0xFFFF8AAE),
        ledge = Color(0xFF1C1524),
    )
}
