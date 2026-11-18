package com.rmgames.feature.game.blink

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

// Carnival gallery palette: bright targets on a dark backdrop (docs/games/03-blink-sniper.md).
private val GalleryBackground = Color(0xFF14142B)
private val TargetRim = Color(0xFFE23B3B)
private val TargetRing = Color(0xFFFDFDFD)
private val TargetCore = Color(0xFFE23B3B)
private val SmallRim = Color(0xFFF5A623)
private val BombRim = Color(0xFFB01B1B)
private val BombCore = Color(0xFF101010)
private val BombMark = Color(0xFFFFD166)
/** Hazard halo that breathes around every bomb so it never blends into the gallery. */
private val BombHalo = Color(0xFFFF9F1C)
private val Crosshair = Color(0xFF7DFCE0)
/** Impact colours: mint for a bullseye (matches the crosshair), gold for the outer ring. */
private val HitBullseye = Color(0xFF7DFCE0)
private val HitOuter = Color(0xFFFFD166)
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
    val elapsedMs = state.elapsedMs
    val remainingMs = state.remainingMs
    val eyesOpen = state.eyesOpen
    val finished = state.finished
    val endReason = state.endReason
    val lastHit = state.lastHit

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawGallery(targets, elapsedMs, if (finished) null else lastHit) },
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
        } else if (lastHit != null) {
            // A fresh hit gets a fresh composition scope, so the pop restarts every time.
            key(lastHit.sequence) {
                HitPopup(
                    points = lastHit.points,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

/** How long the "+2" pop stays on screen before it has faded away completely. */
private const val HIT_POP_MS = 700

/**
 * Floating "+1"/"+2" over the crosshair: scales in, drifts up and fades out.
 *
 * [key][androidx.compose.runtime.key] on the hit sequence is what makes it replay —
 * without it a second hit would leave the animation finished at alpha 0.
 */
@Composable
private fun HitPopup(points: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = HIT_POP_MS, easing = LinearOutSlowInEasing),
        )
    }

    val travelled = progress.value
    Text(
        text = "+$points",
        color = if (points >= 2) HitBullseye else HitOuter,
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.graphicsLayer {
            alpha = (1f - travelled * travelled).coerceIn(0f, 1f)
            translationY = -travelled * 150.dp.toPx()
            val scale = 1f + 0.5f * (1f - travelled)
            scaleX = scale
            scaleY = scale
        },
    )
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

/**
 * Draws the gallery: backdrop, every target, the fixed crosshair and the shockwave of
 * the most recent hit. [elapsedMs] drives the bomb halo; [hit] is passed as null once
 * the run ends so a frozen burst never lingers behind the end-of-run message.
 */
private fun DrawScope.drawGallery(
    targets: List<TargetUi>,
    elapsedMs: Long,
    hit: HitFeedback?,
) {
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
                // Breathing hazard halo. It sits outside the hit radius on purpose:
                // it is a warning, not part of the target, so it never affects scoring.
                val pulse = bombPulse(elapsedMs)
                val halo = outer * (1.10f + 0.30f * pulse)
                drawCircle(
                    color = BombHalo.copy(alpha = 0.14f + 0.26f * pulse),
                    radius = halo,
                    center = center,
                )
                drawCircle(
                    color = BombHalo.copy(alpha = 0.55f + 0.45f * pulse),
                    radius = halo,
                    center = center,
                    style = Stroke(width = 6f),
                )
                drawCircle(color = BombRim, radius = outer, center = center)
                drawCircle(color = BombCore, radius = inner, center = center)
                drawCircle(
                    color = BombMark,
                    radius = inner,
                    center = center,
                    style = Stroke(width = 4f),
                )
                val arm = inner * 0.75f
                drawLine(
                    color = BombMark,
                    start = Offset(center.x - arm, center.y - arm),
                    end = Offset(center.x + arm, center.y + arm),
                    strokeWidth = 7f,
                )
                drawLine(
                    color = BombMark,
                    start = Offset(center.x - arm, center.y + arm),
                    end = Offset(center.x + arm, center.y - arm),
                    strokeWidth = 7f,
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

    // Impact shockwave: expands and fades where the shot landed, on the run clock.
    hit?.let { feedback ->
        val ageMs = elapsedMs - feedback.atMs
        if (ageMs in 0 until HIT_BURST_MS) {
            val t = ageMs.toFloat() / HIT_BURST_MS
            val color = if (feedback.points >= 2) HitBullseye else HitOuter
            val impact = Offset((feedback.x * width).toFloat(), (feedback.y * height).toFloat())
            val radius = (0.075 * width).toFloat() * (1f + 1.9f * t)
            drawCircle(
                color = color.copy(alpha = 0.22f * (1f - t)),
                radius = radius,
                center = impact,
            )
            drawCircle(
                color = color.copy(alpha = 0.9f * (1f - t)),
                radius = radius,
                center = impact,
                style = Stroke(width = 7f * (1f - t) + 1f),
            )
        }
    }
}

/** How long the impact shockwave expands, in run-clock ms. */
private const val HIT_BURST_MS = 450L

/** Period of the bomb halo's breathing, in run-clock ms. */
private const val BOMB_PULSE_PERIOD_MS = 900L

/**
 * 0..1 breathing value for the bomb hazard halo, driven straight off the run clock so
 * the render needs no animation machinery. It rests at 0.5 on the beat, peaks at a
 * quarter of the period and troughs at three quarters.
 *
 * Internal rather than private purely so tests can pin it down (AGENTS.md §5).
 */
internal fun bombPulse(elapsedMs: Long, periodMs: Long = BOMB_PULSE_PERIOD_MS): Float {
    require(periodMs > 0) { "periodMs must be positive, was $periodMs" }
    val phase = (elapsedMs % periodMs) / periodMs.toFloat()
    return 0.5f + 0.5f * sin(2.0 * PI * phase).toFloat()
}

/** `42.5` from milliseconds, without locale-dependent decimal separators. */
private fun formatTenths(millis: Long): String {
    val tenths = Math.round(millis / 100.0) / 10.0
    return tenths.toString()
}
