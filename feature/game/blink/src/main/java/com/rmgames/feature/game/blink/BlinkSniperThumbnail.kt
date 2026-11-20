package com.rmgames.feature.game.blink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

private val Ink = Color(0xFF10131A)
private val Red = Color(0xFFE63946)
private val White = Color(0xFFF6F1FA)
private val Spark = Color(0xFFFFD23F)

/**
 * Thumbnail (Decision 5): a target sliding into the crosshair, a bomb behind it.
 * Drawn on the game's accent background; scales with the box.
 */
@Composable
internal fun BlinkSniperThumbnail() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val line = h * 0.03f
        // Target (bullseye rings).
        val t = Offset(w * 0.34f, h * 0.52f)
        val r = h * 0.3f
        listOf(1f to Red, 0.75f to White, 0.5f to Red, 0.25f to White).forEach { (k, c) -> drawCircle(c, r * k, t) }
        drawCircle(Ink, r, t, style = Stroke(line))
        // Motion lines behind the target.
        for (i in 0 until 3) {
            val y = t.y + (i - 1) * r * 0.55f
            drawLine(Ink.copy(alpha = 0.6f), Offset(t.x - r * 1.6f, y), Offset(t.x - r * 1.15f, y), strokeWidth = line, cap = StrokeCap.Round)
        }
        // Crosshair, fixed in the middle.
        val c = Offset(w * 0.52f, h * 0.5f)
        val cr = h * 0.2f
        drawCircle(Ink, cr, c, style = Stroke(line * 1.2f))
        drawLine(Ink, Offset(c.x - cr * 1.5f, c.y), Offset(c.x - cr * 0.4f, c.y), strokeWidth = line * 1.2f, cap = StrokeCap.Round)
        drawLine(Ink, Offset(c.x + cr * 0.4f, c.y), Offset(c.x + cr * 1.5f, c.y), strokeWidth = line * 1.2f, cap = StrokeCap.Round)
        drawLine(Ink, Offset(c.x, c.y - cr * 1.5f), Offset(c.x, c.y - cr * 0.4f), strokeWidth = line * 1.2f, cap = StrokeCap.Round)
        drawLine(Ink, Offset(c.x, c.y + cr * 0.4f), Offset(c.x, c.y + cr * 1.5f), strokeWidth = line * 1.2f, cap = StrokeCap.Round)
        drawCircle(Ink, line, c)
        // Bomb with a lit fuse.
        val b = Offset(w * 0.82f, h * 0.58f)
        val br = h * 0.17f
        drawCircle(Ink, br, b)
        drawCircle(White.copy(alpha = 0.35f), br * 0.3f, Offset(b.x - br * 0.4f, b.y - br * 0.4f))
        drawLine(Ink, Offset(b.x + br * 0.5f, b.y - br * 0.85f), Offset(b.x + br * 0.95f, b.y - br * 1.45f), strokeWidth = line, cap = StrokeCap.Round)
        drawCircle(Spark, line * 1.6f, Offset(b.x + br * 1.05f, b.y - br * 1.6f))
    }
}
