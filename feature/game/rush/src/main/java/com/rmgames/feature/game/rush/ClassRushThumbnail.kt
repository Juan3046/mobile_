package com.rmgames.feature.game.rush

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

private val ThumbInk = Color(0xFF10131A)
private val ThumbRoad = Color(0xFF3B3F4A)
private val ThumbLine = Color(0xFFE8E4DA)
private val ThumbCar = Color(0xFF2A9D8F)
private val ThumbHoodie = Color(0xFFFF5D8F)
private val ThumbSkin = Color(0xFFF2C9A0)
private val ThumbBag = Color(0xFFFFD23F)

/**
 * Thumbnail (Decision 5): a road band across the accent background, a car on it and the
 * student hopping over towards the top. Scales with the box.
 */
@Composable
internal fun ClassRushThumbnail() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val line = h * 0.03f
        // Road band.
        val roadTop = h * 0.45f
        val roadH = h * 0.32f
        drawRect(ThumbRoad, Offset(0f, roadTop), Size(w, roadH))
        drawRect(ThumbInk, Offset(0f, roadTop), Size(w, roadH), style = Stroke(line))
        for (i in 0 until 6) drawRect(ThumbLine, Offset(w * (0.04f + i * 0.17f), roadTop + roadH / 2 - line / 2), Size(w * 0.09f, line))
        // Car.
        val carTop = roadTop + roadH * 0.15f
        drawRoundRect(ThumbCar, Offset(w * 0.12f, carTop), Size(w * 0.24f, roadH * 0.7f), CornerRadius(h * 0.05f))
        drawRoundRect(ThumbInk, Offset(w * 0.12f, carTop), Size(w * 0.24f, roadH * 0.7f), CornerRadius(h * 0.05f), style = Stroke(line))
        // Student mid-hop above the road.
        val c = Offset(w * 0.62f, h * 0.3f)
        val r = h * 0.13f
        drawRoundRect(ThumbBag, Offset(c.x - r * 0.75f, c.y - r * 0.1f), Size(r * 1.5f, r), CornerRadius(r * 0.3f))
        drawCircle(ThumbHoodie, r * 0.85f, c)
        drawCircle(ThumbSkin, r * 0.5f, Offset(c.x, c.y - r * 0.35f))
        drawCircle(ThumbInk, r * 0.85f, c, style = Stroke(line))
        // Hop arc.
        listOf(0.70f, 0.74f, 0.78f).forEachIndexed { i, x -> drawCircle(ThumbInk, line * 0.6f, Offset(w * x, h * (0.48f + i * 0.08f))) }
    }
}
