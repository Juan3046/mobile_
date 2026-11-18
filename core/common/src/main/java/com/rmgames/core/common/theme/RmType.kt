package com.rmgames.core.common.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rmgames.core.common.R

/**
 * Bundled fonts (SIL Open Font License — see licenses/fonts and CREDITS.md):
 * - [Display] Bungee: display/headline styles, scores and the hero Play button.
 * - [Body] Rubik (variable): every functional text — titles, body, labels, buttons.
 * - [Numbers] JetBrains Mono (variable): small tabular figures (timers, debug readouts).
 */
object RmFonts {
    val Display: FontFamily = FontFamily(Font(R.font.bungee_regular, FontWeight.Normal))

    val Body: FontFamily = variableFamily(R.font.rubik_variable, 400, 500, 600, 700)

    val Numbers: FontFamily = variableFamily(R.font.jetbrains_mono_variable, 500, 700)

    // Variable-font axes are still marked experimental in Compose UI 1.7; stable on API 26+.
    @OptIn(ExperimentalTextApi::class)
    private fun variableFamily(resId: Int, vararg weights: Int) = FontFamily(
        weights.map { w ->
            Font(resId, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
        },
    )
}

/**
 * Material 3 type scale (15 styles). Display + headline use the [display] face, everything
 * functional uses the [body] (reading) face. Label medium/small are uppercase in the design
 * system: apply `.uppercase()` to the string at the call site (TextStyle has no transform).
 */
fun rmTypography(display: FontFamily, body: FontFamily): Typography {
    fun d(size: Int, line: Int) = TextStyle(fontFamily = display, fontWeight = FontWeight.Normal,
        fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)
    fun b(weight: FontWeight, size: Int, line: Int, tracking: TextUnit = 0.sp) = TextStyle(
        fontFamily = body, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking)
    return Typography(
        displayLarge = d(57, 64), displayMedium = d(45, 52), displaySmall = d(36, 44),
        headlineLarge = d(32, 40), headlineMedium = d(28, 36), headlineSmall = d(24, 32),
        titleLarge = b(FontWeight.SemiBold, 22, 28),
        titleMedium = b(FontWeight.SemiBold, 16, 24, 0.01.em),
        titleSmall = b(FontWeight.SemiBold, 14, 20, 0.01.em),
        bodyLarge = b(FontWeight.Normal, 16, 24),
        bodyMedium = b(FontWeight.Normal, 14, 20),
        bodySmall = b(FontWeight.Normal, 12, 16),
        labelLarge = b(FontWeight.SemiBold, 14, 20, 0.01.em),
        labelMedium = b(FontWeight.SemiBold, 12, 16, 0.06.em),
        labelSmall = b(FontWeight.SemiBold, 11, 16, 0.06.em),
    )
}

/** Scores, timers, ranks: tabular figures so digits don't jitter while counting. */
fun rmNumberStyle(family: FontFamily, size: TextUnit, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = family, fontWeight = weight, fontSize = size, lineHeight = size,
    fontFeatureSettings = "tnum",
)
