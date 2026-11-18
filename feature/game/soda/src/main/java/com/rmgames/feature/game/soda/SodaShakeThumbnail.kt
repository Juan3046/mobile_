package com.rmgames.feature.game.soda

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

private val Ink = Color(0xFF10131A)
private val Can = Color(0xFFE63946)
private val Band = Color(0xFFF6F1FA)
private val Lid = Color(0xFFB8BEC9)

/**
 * Thumbnail (Decision 5): a soda can mid-shake, fizz escaping from the top.
 * Drawn on the game's accent background; scales with the box.
 */
@Composable
internal fun SodaShakeThumbnail() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val line = h * 0.03f
        val c = Offset(w * 0.5f, h * 0.56f)
        val cw = h * 0.36f
        val ch = h * 0.66f
        // Shake lines on both sides.
        listOf(-1f, 1f).forEach { side ->
            for (i in 0 until 3) {
                val x = c.x + side * (cw * 0.95f + i * w * 0.035f)
                drawLine(Ink, Offset(x, c.y - ch * 0.18f + i * h * 0.03f), Offset(x, c.y + ch * 0.18f - i * h * 0.03f),
                    strokeWidth = line, cap = StrokeCap.Round)
            }
        }
        rotate(-14f, c) {
            val topLeft = Offset(c.x - cw / 2, c.y - ch / 2)
            drawRoundRect(Can, topLeft, Size(cw, ch), CornerRadius(cw * 0.2f))
            drawRect(Band, Offset(topLeft.x, c.y - ch * 0.1f), Size(cw, ch * 0.2f))
            drawRoundRect(Ink, topLeft, Size(cw, ch), CornerRadius(cw * 0.2f), style = Stroke(line))
            drawOval(Lid, Offset(topLeft.x + cw * 0.08f, topLeft.y - ch * 0.05f), Size(cw * 0.84f, ch * 0.12f))
            drawOval(Ink, Offset(topLeft.x + cw * 0.08f, topLeft.y - ch * 0.05f), Size(cw * 0.84f, ch * 0.12f), style = Stroke(line * 0.8f))
        }
        // Fizz.
        listOf(0.42f to 0.1f, 0.55f to 0.05f, 0.62f to 0.14f, 0.48f to 0.02f).forEachIndexed { i, (x, y) ->
            val r = h * (0.035f + 0.01f * (i % 2))
            drawCircle(Band, r, Offset(w * x, h * y + h * 0.04f))
            drawCircle(Ink, r, Offset(w * x, h * y + h * 0.04f), style = Stroke(line * 0.5f))
        }
    }
}
