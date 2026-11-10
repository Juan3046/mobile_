package com.rmgames.feature.game.blink

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Carnival gallery palette: bright targets on a dark backdrop (docs/games/03-blink-sniper.md).
private val GalleryBackground = Color(0xFF14142B)
private val TargetRim = Color(0xFFE23B3B)
private val TargetRing = Color(0xFFFDFDFD)
private val TargetCore = Color(0xFFE23B3B)
private val SmallRim = Color(0xFFF5A623)
private val BombRim = Color(0xFF7A0F0F)
private val BombCore = Color(0xFF101010)
private val BombMark = Color(0xFFFFD166)
private val Crosshair = Color(0xFF7DFCE0)
private val HudText = Color(0xFFFFFFFF)
private val PreviewBackground = Color(0xFF0B0B1A)
private val PreviewBorder = Color(0xFF3A3A5C)
private val PreviewEye = Color(0xFF7DFCE0)
private val PreviewPupil = Color(0xFF101010)
private val PreviewLabel = Color(0xFF8A8AB0)

/**
 * Game 03 screen: the shooting gallery, the minimal HUD and the end-of-run note.
 *
 * Rendering only — the [BlinkSniperGame] owns every rule through its [BlinkSniperEngine],
 * so this file never scores, never counts down on its own and takes no parameters beyond
 * the game instance it closes over (Game Hosting Contract, docs/08-implementation-plan.md).
 *
 * Provisional graphics: everything is drawn procedurally with [drawBehind], no assets.
 */
@Composable
fun BlinkSniperScreen(game: BlinkSniperGame) {
    val state by game.state.collectAsState()

    // Read during composition so the draw modifier is rebuilt whenever the run changes.
    val targets = state.targets
    val score = state.score
    val remainingMs = state.remainingMs
    val eyesOpen = state.eyesOpen
    val finished = state.finished
    val endReason = state.endReason

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawGallery(targets) },
        )

        Text(
            text = "SCORE: $score",
            color = HudText,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        )
        Text(
            text = "TIME: ${formatTenths(remainingMs)}s",
            color = HudText,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        )

        EyeTrackingPreview(
            eyesOpen = eyesOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 16.dp),
        )

        if (finished) {
            Text(
                text = if (endReason == EndReason.BOMB) "BOOM! Game Over" else "TIME'S UP!",
                color = HudText,
                fontSize = 34.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
        }
    }
}

/**
 * Camera feedback box. Phase 1b placeholder: `:core:vision` only exposes eye states,
 * and adding a real CameraX preview means changing that frozen module — so this shows
 * the tracked eye state instead. Tracked as an open question in the game doc.
 */
@Composable
private fun EyeTrackingPreview(eyesOpen: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(76.dp)
            .drawBehind {
                drawRect(color = PreviewBackground)
                drawRect(color = PreviewBorder, style = Stroke(width = 3f))

                val cx = size.width / 2
                val cy = size.height / 2
                if (eyesOpen) {
                    drawOval(
                        color = PreviewEye,
                        topLeft = Offset(cx - 18f, cy - 9f),
                        size = Size(36f, 18f),
                    )
                    drawCircle(color = PreviewPupil, radius = 6f, center = Offset(cx, cy))
                } else {
                    drawLine(
                        color = PreviewEye,
                        start = Offset(cx - 18f, cy),
                        end = Offset(cx + 18f, cy),
                        strokeWidth = 4f,
                    )
                }
            },
    ) {
        Text(
            text = "CAM",
            color = PreviewLabel,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 3.dp),
        )
    }
}

/** Draws the gallery: backdrop, every target and the fixed crosshair. */
private fun DrawScope.drawGallery(targets: List<TargetUi>) {
    val width = size.width
    val height = size.height
    drawRect(color = GalleryBackground)

    targets.forEach { target ->
        val center = Offset((target.x * width).toFloat(), (target.y * height).toFloat())
        val outer = (target.outerRadius * width).toFloat()
        val inner = (target.bullseyeRadius * width).toFloat()

        when (target.kind) {
            TargetKind.STANDARD -> {
                drawCircle(color = TargetRim, radius = outer, center = center)
                drawCircle(color = TargetRing, radius = inner, center = center)
                drawCircle(color = TargetCore, radius = inner * 0.4f, center = center)
            }
            TargetKind.FAST_SMALL -> {
                drawCircle(color = SmallRim, radius = outer, center = center)
                drawCircle(color = TargetRing, radius = inner, center = center)
                drawCircle(color = TargetCore, radius = inner * 0.4f, center = center)
            }
            TargetKind.BOMB -> {
                drawCircle(color = BombRim, radius = outer, center = center)
                drawCircle(color = BombCore, radius = inner, center = center)
                val arm = inner * 0.7f
                drawLine(
                    color = BombMark,
                    start = Offset(center.x - arm, center.y - arm),
                    end = Offset(center.x + arm, center.y + arm),
                    strokeWidth = 4f,
                )
                drawLine(
                    color = BombMark,
                    start = Offset(center.x - arm, center.y + arm),
                    end = Offset(center.x + arm, center.y - arm),
                    strokeWidth = 4f,
                )
            }
        }
    }

    // Fixed crosshair — only the centre lane can ever be hit (see the game doc).
    val crosshair = Offset((CROSSHAIR_X * width).toFloat(), (CROSSHAIR_Y * height).toFloat())
    val radius = (0.075 * width).toFloat()
    drawCircle(color = Crosshair, radius = radius, center = crosshair, style = Stroke(width = 4f))
    drawLine(
        color = Crosshair,
        start = Offset(crosshair.x - radius, crosshair.y),
        end = Offset(crosshair.x + radius, crosshair.y),
        strokeWidth = 3f,
    )
    drawLine(
        color = Crosshair,
        start = Offset(crosshair.x, crosshair.y - radius),
        end = Offset(crosshair.x, crosshair.y + radius),
        strokeWidth = 3f,
    )
    drawCircle(color = Crosshair, radius = 4f, center = crosshair)
}

/** `42.5` from milliseconds, without locale-dependent decimal separators. */
private fun formatTenths(millis: Long): String {
    val tenths = Math.round(millis / 100.0) / 10.0
    return tenths.toString()
}
