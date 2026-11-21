package com.rmgames.feature.game.compass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF10131A)
private val Face = Color(0xFFF6F1FA)
private val North = Color(0xFFE63946)

/**
 * Thumbnail (Decision 5): a compass mid-spin, needle swinging between quadrants.
 * Drawn on the game's accent background; scales with the box.
 */
@Composable
internal fun CompassChaosThumbnail() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val line = h * 0.03f
        val c = Offset(w * 0.5f, h * 0.5f)
        val r = h * 0.33f
        // Spin arrows around the dial.
        listOf(200f, 20f).forEach { start ->
            drawArc(Ink, start, 70f, useCenter = false, topLeft = Offset(c.x - r * 1.22f, c.y - r * 1.22f),
                size = androidx.compose.ui.geometry.Size(r * 2.44f, r * 2.44f), style = Stroke(line, cap = StrokeCap.Round))
        }
        // Dial with cardinal ticks.
        drawCircle(Face, r, c)
        drawCircle(Ink, r, c, style = Stroke(line))
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val len = if (i % 2 == 0) 0.2f else 0.1f
            val o = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
            val inner = Offset(c.x + (r * (1 - len) * cos(a)).toFloat(), c.y + (r * (1 - len) * sin(a)).toFloat())
            drawLine(Ink, o, inner, strokeWidth = line * if (i % 2 == 0) 1f else 0.6f, cap = StrokeCap.Round)
        }
        // Needle, tilted mid-turn: red half points north-ish.
        rotate(35f, c) {
            val north = Path().apply { moveTo(c.x, c.y - r * 0.72f); lineTo(c.x + r * 0.16f, c.y); lineTo(c.x - r * 0.16f, c.y); close() }
            val south = Path().apply { moveTo(c.x, c.y + r * 0.72f); lineTo(c.x + r * 0.16f, c.y); lineTo(c.x - r * 0.16f, c.y); close() }
            drawPath(north, North)
            drawPath(south, Ink)
            drawPath(north, Ink, style = Stroke(line * 0.6f))
        }
        drawCircle(Face, line * 1.2f, c)
        drawCircle(Ink, line * 1.2f, c, style = Stroke(line * 0.5f))
    }
}
