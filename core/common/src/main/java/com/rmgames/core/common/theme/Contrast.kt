package com.rmgames.core.common.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** WCAG 2.x contrast maths, used to keep every RmTheme colour pair readable. */
object Contrast {

    /** Relative luminance of an sRGB colour (0 = black, 1 = white); alpha is ignored. */
    fun relativeLuminance(color: Color): Double {
        fun linear(channel: Float): Double {
            val c = channel.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }

    /** Contrast ratio between two colours, from 1.0 (identical) to 21.0 (black on white). */
    fun ratio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Minimum for body text (WCAG AA). */
    const val TEXT = 4.5

    /** Minimum for large text, icons and UI boundaries (WCAG AA). */
    const val UI = 3.0
}
