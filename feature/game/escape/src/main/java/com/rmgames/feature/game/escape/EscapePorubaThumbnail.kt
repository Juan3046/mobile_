package com.rmgames.feature.game.escape

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
private val Road = Color(0xFF3A3F4B)
private val Panel = Color(0xFFE8E2F0)
private val Cone = Color(0xFFFF8A3D)
private val Light = Color(0xFFF6F1FA)

/**
 * Thumbnail (Decision 5): a road running into the distance between two Poruba panel
 * blocks, a cone ahead and the runner in the lower third. Drawn on the game's accent
 * background; scales with the box (2:1 on Main, 16:9 on the intro).
 */
@Composable
internal fun EscapePorubaThumbnail() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val line = h * 0.03f
        // Panel blocks with window grid.
        listOf(w * 0.04f, w * 0.76f).forEach { x ->
            val top = h * 0.14f
            val bw = w * 0.2f
            val bh = h * 0.8f
            drawRect(Panel, Offset(x, top), Size(bw, bh))
            drawRect(Ink, Offset(x, top), Size(bw, bh), style = Stroke(line))
            for (r in 0 until 5) for (c in 0 until 3) {
                drawRect(Ink.copy(alpha = 0.75f), Offset(x + bw * (0.12f + c * 0.29f), top + bh * (0.08f + r * 0.18f)), Size(bw * 0.18f, bh * 0.09f))
            }
        }
        // Road in perspective.
        val road = Path().apply {
            moveTo(w * 0.27f, h); lineTo(w * 0.73f, h); lineTo(w * 0.55f, h * 0.16f); lineTo(w * 0.45f, h * 0.16f); close()
        }
        drawPath(road, Road)
        drawPath(road, Ink, style = Stroke(line))
        for (i in 0 until 4) {
            val t = i / 4f
            val y = h * (0.24f + t * 0.7f)
            drawLine(Light, Offset(w * 0.5f, y), Offset(w * 0.5f, y + h * (0.05f + t * 0.06f)), strokeWidth = line * (0.6f + t), cap = StrokeCap.Round)
        }
        // Cone ahead.
        val cone = Path().apply {
            moveTo(w * 0.555f, h * 0.36f); lineTo(w * 0.585f, h * 0.5f); lineTo(w * 0.525f, h * 0.5f); close()
        }
        drawPath(cone, Cone)
        drawPath(cone, Ink, style = Stroke(line * 0.6f))
        // Runner (lower third).
        val cx = w * 0.43f
        val head = Offset(cx, h * 0.6f)
        val s = Stroke(line * 1.3f, cap = StrokeCap.Round)
        drawCircle(Light, h * 0.055f, head)
        drawCircle(Ink, h * 0.055f, head, style = Stroke(line * 0.6f))
        drawLine(Light, Offset(cx, h * 0.66f), Offset(cx - w * 0.01f, h * 0.8f), strokeWidth = s.width, cap = StrokeCap.Round)
        drawLine(Light, Offset(cx - w * 0.01f, h * 0.8f), Offset(cx - w * 0.04f, h * 0.93f), strokeWidth = s.width, cap = StrokeCap.Round)
        drawLine(Light, Offset(cx - w * 0.01f, h * 0.8f), Offset(cx + w * 0.03f, h * 0.9f), strokeWidth = s.width, cap = StrokeCap.Round)
        drawLine(Light, Offset(cx, h * 0.7f), Offset(cx + w * 0.04f, h * 0.75f), strokeWidth = s.width, cap = StrokeCap.Round)
        drawLine(Light, Offset(cx, h * 0.7f), Offset(cx - w * 0.04f, h * 0.68f), strokeWidth = s.width, cap = StrokeCap.Round)
    }
}
