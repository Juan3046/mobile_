package com.rmgames.feature.game.onion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

private val Ink = Color(0xFF10131A)
private val Belt = Color(0xFF2B2F3A)
private val OnionSkin = Color(0xFFE9C46A)
private val Sprout = Color(0xFF7BC67B)
private val Apple = Color(0xFFE63946)
private val Orange = Color(0xFFFF9F1C)

/**
 * Thumbnail (Decision 5): the onion riding the checkout belt with fruit coming at it.
 * Drawn on the game's accent background; scales with the box.
 */
@Composable
internal fun OnionRollThumbnail() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val line = h * 0.03f
        // Belt + rollers.
        val beltTop = h * 0.66f
        drawRect(Belt, Offset(0f, beltTop), Size(w, h * 0.16f))
        drawLine(Ink, Offset(0f, beltTop), Offset(w, beltTop), strokeWidth = line)
        for (i in 0..6) drawCircle(Ink, h * 0.05f, Offset(w * (0.07f + i * 0.145f), h * 0.86f))
        // Fruit on the belt.
        drawCircle(Apple, h * 0.1f, Offset(w * 0.17f, beltTop - h * 0.1f))
        drawCircle(Ink, h * 0.1f, Offset(w * 0.17f, beltTop - h * 0.1f), style = Stroke(line))
        drawLine(Ink, Offset(w * 0.17f, beltTop - h * 0.2f), Offset(w * 0.18f, beltTop - h * 0.26f), strokeWidth = line, cap = StrokeCap.Round)
        drawCircle(Orange, h * 0.09f, Offset(w * 0.85f, beltTop - h * 0.09f))
        drawCircle(Ink, h * 0.09f, Offset(w * 0.85f, beltTop - h * 0.09f), style = Stroke(line))
        // The onion.
        val cx = w * 0.5f
        val bottom = beltTop
        val r = h * 0.2f
        val body = Path().apply {
            moveTo(cx, h * 0.12f)
            cubicTo(cx + r * 0.2f, h * 0.24f, cx + r * 1.15f, bottom - r * 1.4f, cx + r * 1.05f, bottom - r * 0.6f)
            cubicTo(cx + r * 0.95f, bottom, cx - r * 0.95f, bottom, cx - r * 1.05f, bottom - r * 0.6f)
            cubicTo(cx - r * 1.15f, bottom - r * 1.4f, cx - r * 0.2f, h * 0.24f, cx, h * 0.12f)
            close()
        }
        drawPath(body, OnionSkin)
        drawPath(body, Ink, style = Stroke(line))
        listOf(-0.45f, 0f, 0.45f).forEach { k ->
            drawLine(Ink.copy(alpha = 0.5f), Offset(cx + r * k * 0.5f, h * 0.3f), Offset(cx + r * k, bottom - r * 0.25f), strokeWidth = line * 0.5f, cap = StrokeCap.Round)
        }
        drawLine(Sprout, Offset(cx, h * 0.12f), Offset(cx - w * 0.02f, h * 0.03f), strokeWidth = line * 1.2f, cap = StrokeCap.Round)
        drawLine(Sprout, Offset(cx, h * 0.12f), Offset(cx + w * 0.025f, h * 0.04f), strokeWidth = line * 1.2f, cap = StrokeCap.Round)
    }
}
