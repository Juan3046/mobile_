package com.rmgames.core.common.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ContrastTest {

    @Test
    fun `black on white is the maximum 21 to 1`() {
        assertEquals(21.0, Contrast.ratio(Color.Black, Color.White), 0.001)
    }

    @Test
    fun `a colour against itself is 1 to 1`() {
        assertEquals(1.0, Contrast.ratio(Color(0xFFFF5D8F), Color(0xFFFF5D8F)), 0.001)
    }

    @Test
    fun `ratio is symmetric`() {
        val a = Color(0xFFFF5D8F)
        val b = Color(0xFF2B0011)
        assertEquals(Contrast.ratio(a, b), Contrast.ratio(b, a), 0.0)
    }

    @Test
    fun `mid grey on white matches the WCAG reference value`() {
        // #777777 on white is the textbook "just fails AA" example: 4.48:1.
        assertEquals(4.48, Contrast.ratio(Color(0xFF777777), Color.White), 0.01)
    }

    @Test
    fun `relative luminance uses the sRGB transfer curve`() {
        assertEquals(0.0, Contrast.relativeLuminance(Color.Black), 0.0)
        assertEquals(1.0, Contrast.relativeLuminance(Color.White), 0.0001)
        // #808080 → channel 0.502 → linear ≈ 0.2159
        assertEquals(0.2159, Contrast.relativeLuminance(Color(0xFF808080)), 0.0005)
    }
}
