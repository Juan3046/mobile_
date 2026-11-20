package com.rmgames.core.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RmTypographyAndShapesTest {

    private val display = FontFamily.Serif   // stand-ins: the real families need Android resources
    private val body = FontFamily.SansSerif
    private val type = rmTypography(display = display, body = body)

    @Test
    fun `display and headline styles use the display face`() {
        listOf(type.displayLarge, type.displayMedium, type.displaySmall,
            type.headlineLarge, type.headlineMedium, type.headlineSmall)
            .forEach { assertSame(display, it.fontFamily) }
    }

    @Test
    fun `title body and label styles use the reading face`() {
        listOf(type.titleLarge, type.titleMedium, type.titleSmall,
            type.bodyLarge, type.bodyMedium, type.bodySmall,
            type.labelLarge, type.labelMedium, type.labelSmall)
            .forEach { assertSame(body, it.fontFamily) }
    }

    @Test
    fun `type scale follows Material 3 sizes`() {
        assertEquals(57.sp, type.displayLarge.fontSize); assertEquals(64.sp, type.displayLarge.lineHeight)
        assertEquals(32.sp, type.headlineLarge.fontSize)
        assertEquals(22.sp, type.titleLarge.fontSize)
        assertEquals(16.sp, type.bodyLarge.fontSize); assertEquals(24.sp, type.bodyLarge.lineHeight)
        assertEquals(11.sp, type.labelSmall.fontSize)
        assertEquals(FontWeight.SemiBold, type.titleMedium.fontWeight)
        assertEquals(FontWeight.Normal, type.bodyMedium.fontWeight)
    }

    @Test
    fun `number style uses tabular figures`() {
        val num = rmNumberStyle(FontFamily.Monospace, size = 120.sp)
        assertEquals("tnum", num.fontFeatureSettings)
        assertEquals(120.sp, num.fontSize)
        assertSame(FontFamily.Monospace, num.fontFamily)
        assertEquals(LineHeightStyle.Trim.Both, num.lineHeightStyle?.trim)
    }

    @Test
    fun `shape scale matches the design system radii`() {
        val s = RmShapes
        assertEquals(RoundedCornerShape(6.dp), s.extraSmall)
        assertEquals(RoundedCornerShape(10.dp), s.small)
        assertEquals(RoundedCornerShape(14.dp), s.medium)
        assertEquals(RoundedCornerShape(18.dp), s.large)
        assertEquals(RoundedCornerShape(24.dp), s.extraLarge)
    }

    @Test
    fun `dimens hold the spacing roles and the ledge`() {
        val d = RmDimens()
        assertEquals(8.dp, d.gapRelated)
        assertEquals(24.dp, d.gapSection)
        assertEquals(32.dp, d.gapMajor)
        assertEquals(20.dp, d.gutter)
        assertEquals(48.dp, d.targetMin)
        assertEquals(2.dp, d.strokeWidth)
        assertEquals(4.dp, d.ledge)
        assertEquals(1.dp, d.ledgePressed)
        assertEquals(3.dp, d.pressShift)
    }

    @Test
    fun `motion durations match the spec`() {
        val m = RmMotion()
        assertEquals(100, m.durationFast)
        assertEquals(250, m.durationBase)
        assertEquals(450, m.durationSlow)
        assertEquals(900, m.durationCountUp)
    }
}
