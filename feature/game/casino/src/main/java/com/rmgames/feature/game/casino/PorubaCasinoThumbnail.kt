package com.rmgames.feature.game.casino

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

private val ThumbInk = Color(0xFF10131A)
private val ThumbCardFace = Color(0xFFF6F1FA)
private val ThumbRed = Color(0xFFE63946)
private val ThumbBanana = Color(0xFFFFD23F)

/**
 * Thumbnail (plan 09 Decision 5): a blackjack hand — an ace of spades and a red king card
 * fanned out — next to a banana, the run's currency. Drawn on the game's accent
 * background; scales with the box (2:1 on Main, 16:9 on the intro).
 */
@Composable
internal fun PorubaCasinoThumbnail() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val line = h * 0.03f
        val cw = h * 0.44f
        val ch = h * 0.62f
        // Back card (red diamond), tilted left.
        card(Offset(w * 0.38f, h * 0.5f), cw, ch, -14f, line) { c ->
            val d = Path().apply {
                moveTo(c.x, c.y - ch * 0.18f); lineTo(c.x + cw * 0.2f, c.y); lineTo(c.x, c.y + ch * 0.18f); lineTo(c.x - cw * 0.2f, c.y); close()
            }
            drawPath(d, ThumbRed)
        }
        // Front card (black spade), tilted right.
        card(Offset(w * 0.5f, h * 0.52f), cw, ch, 10f, line) { c ->
            val r = cw * 0.13f
            drawCircle(ThumbInk, r, Offset(c.x - r * 0.95f, c.y + r * 0.1f))
            drawCircle(ThumbInk, r, Offset(c.x + r * 0.95f, c.y + r * 0.1f))
            val top = Path().apply {
                moveTo(c.x, c.y - ch * 0.2f); lineTo(c.x + r * 1.9f, c.y); lineTo(c.x - r * 1.9f, c.y); close()
            }
            drawPath(top, ThumbInk)
            val stem = Path().apply {
                moveTo(c.x, c.y); lineTo(c.x + r * 0.7f, c.y + ch * 0.17f); lineTo(c.x - r * 0.7f, c.y + ch * 0.17f); close()
            }
            drawPath(stem, ThumbInk)
        }
        // Banana (the currency).
        val b = Path().apply {
            moveTo(w * 0.7f, h * 0.72f)
            quadraticTo(w * 0.84f, h * 0.78f, w * 0.9f, h * 0.36f)
            quadraticTo(w * 0.81f, h * 0.62f, w * 0.7f, h * 0.72f)
            close()
        }
        drawPath(b, ThumbBanana)
        drawPath(b, ThumbInk, style = Stroke(line * 0.8f))
        drawLine(ThumbInk, Offset(w * 0.9f, h * 0.36f), Offset(w * 0.905f, h * 0.31f), strokeWidth = line, cap = StrokeCap.Round)
    }
}

private fun DrawScope.card(center: Offset, cw: Float, ch: Float, degrees: Float, line: Float, pip: DrawScope.(Offset) -> Unit) {
    rotate(degrees, center) {
        val topLeft = Offset(center.x - cw / 2, center.y - ch / 2)
        drawRoundRect(ThumbCardFace, topLeft, Size(cw, ch), CornerRadius(cw * 0.12f))
        drawRoundRect(ThumbInk, topLeft, Size(cw, ch), CornerRadius(cw * 0.12f), style = Stroke(line))
        pip(center)
    }
}
