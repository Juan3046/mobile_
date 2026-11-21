package com.rmgames.feature.game.sound

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

private val ThumbInk = Color(0xFF10131A)
private val ThumbBar = Color(0xFFD63384)
private val ThumbBall = Color(0xFF22D3EE)

/**
 * Thumbnail (plan 09 Decision 5): two barriers with a gap, the ball flying through it and
 * sound waves pushing it up. Drawn on the game's accent background; scales with the box.
 */
@Composable
internal fun SoundBarrierThumbnail() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val line = h * 0.03f
        // Barrier with a gap in the middle.
        val barX = w * 0.58f
        val barW = w * 0.1f
        listOf(0f to h * 0.32f, h * 0.68f to h).forEach { (top, bottom) ->
            drawRoundRect(ThumbBar, Offset(barX, top), Size(barW, bottom - top), CornerRadius(barW * 0.3f))
            drawRoundRect(ThumbInk, Offset(barX, top), Size(barW, bottom - top), CornerRadius(barW * 0.3f), style = Stroke(line))
        }
        // Ball heading for the gap.
        val ball = Offset(w * 0.42f, h * 0.5f)
        val r = h * 0.11f
        drawCircle(ThumbBall, r, ball)
        drawCircle(ThumbInk, r, ball, style = Stroke(line))
        // Sound waves under the ball.
        for (i in 1..3) {
            val rr = r * (0.9f + i * 0.45f)
            drawArc(ThumbInk, 200f, 140f, false, Offset(ball.x - rr, ball.y + r * 0.6f - rr), Size(rr * 2, rr * 2),
                style = Stroke(line * 0.8f, cap = StrokeCap.Round))
        }
    }
}
