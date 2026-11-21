package com.rmgames.feature.game.sound

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Neon sound corridor palette (docs/games/07-sound-barrier.md — Art Direction).
private val CorridorBackground = Color(0xFF0E0E1F)
private val BackdropTop = Color(0xFF161634)
private val BackdropBottom = Color(0xFF06060E)
private val FloorLine = Color(0xFF2A2A44)
private val BarrierFill = Color(0xFFD63384)
private val BarrierRim = Color(0xFFF7B2D9)
private val BallFill = Color(0xFF22D3EE)
private val BallRim = Color(0xFF0E7490)
private val BallCore = Color(0xFF082F49)
private val MeterTrack = Color(0xFF2A2A44)
private val MeterLoud = Color(0xFFF5A623)
private val MeterQuiet = Color(0xFF3A3A5C)
private val HudText = Color(0xFFFFFFFF)
/** Faint cyan wash inside an opening, so the way through reads at a glance. */
private val GapWash = Color(0x1A22D3EE)

/**
 * Rim drawn on the edge of a bar that faces the gap, so the gap reads instantly.
 * It is drawn with the bar's own corner radius, so it must be at least twice as
 * wide as that radius (pinned by SoundBarrierScreenTest).
 */
internal const val GAP_RIM = 20f

/** Widest corner a bar may have, in pixels; keeps the rim strip from degenerating. */
internal const val MAX_BAR_RADIUS = 10f

/** Width of the soft halo drawn around every bar, in pixels. */
private const val BAR_GLOW = 7f

/**
 * Game 07 screen: the corridor, the ball, the minimal HUD and the sound meter.
 *
 * Rendering only — the [SoundBarrierGame] owns every rule through its
 * [SoundBarrierEngine], so this file never scores, never counts down on its own and
 * takes no parameters beyond the game instance it closes over (Game Hosting Contract,
 * docs/08-implementation-plan.md).
 *
 * Provisional graphics: everything is drawn procedurally with [drawBehind], no assets.
 */
@Composable
fun SoundBarrierScreen(game: SoundBarrierGame) {
    val state by game.state.collectAsState()

    // Read during composition so the draw modifier is rebuilt whenever the run changes.
    val barriers = state.barriers
    val ballX = state.ballX
    val ballY = state.ballY
    val ballRadius = state.ballRadius
    val soundLevel = state.soundLevel
    val soundDetected = state.soundDetected
    val score = state.score
    val remainingMs = state.remainingMs
    val finished = state.finished
    val endReason = state.endReason

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .onKeyEvent { event -> handleDebugKey(event, game::setDebugSound) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Bars extend past the corridor edges so only their gap-facing
                // corners round; without the clip that halo would bleed out.
                .clipToBounds()
                .drawBehind {
                    drawCorridor(barriers, ballX, ballY, ballRadius, soundLevel, soundDetected)
                },
        )

        Text(
            text = "SCORE: $score",
            style = HudStyle,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        )
        Text(
            text = "TIME: ${formatTenths(remainingMs)}s",
            style = HudStyle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        )

        if (finished) {
            Text(
                text = endBanner(endReason),
                style = HudStyle.copy(fontSize = 34.sp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
        }
    }
}

/** HUD copy: white with a soft shadow so it stays readable over any barrier. */
private val HudStyle = TextStyle(
    color = HudText,
    shadow = Shadow(color = Color(0xCC000000), offset = Offset.Zero, blurRadius = 10f),
)

/**
 * Draws the corridor: a graded backdrop, neon floor and ceiling, every barrier
 * pair with its glowing rim, the ball and the sound meter pinned to the bottom
 * edge. Everything is normalized to the box, so the physics and the drawing
 * agree at any screen size.
 */
private fun DrawScope.drawCorridor(
    barriers: List<BarrierUi>,
    ballX: Double,
    ballY: Double,
    ballRadius: Double,
    soundLevel: Float,
    soundDetected: Boolean,
) {
    val width = size.width
    val height = size.height

    // Depth instead of a flat fill: lifted at the top, almost black at the floor.
    drawRect(
        brush = Brush.verticalGradient(listOf(BackdropTop, CorridorBackground, BackdropBottom)),
    )

    drawBoundary(y = 1f, width = width)
    drawBoundary(y = height - 2f, width = width)

    barriers.forEach { barrier ->
        val left = (barrier.x * width).toFloat()
        val barWidth = (barrier.width * width).toFloat()
        if (left > width || left + barWidth < 0f) return@forEach

        val gapTop = screenY(barrier.gapCenter + barrier.gapHalf, height)
        val gapBottom = screenY(barrier.gapCenter - barrier.gapHalf, height)
        if (gapBottom <= gapTop) return@forEach
        val radius = barCornerRadius(barWidth)

        // The opening itself: a faint cyan panel so the way through is obvious.
        drawRect(
            color = GapWash,
            topLeft = Offset(left, gapTop),
            size = Size(barWidth, gapBottom - gapTop),
        )

        // Halo first, so it sits behind the solid bar.
        drawRoundRect(
            color = BarrierFill.copy(alpha = 0.20f),
            topLeft = Offset(left - BAR_GLOW, -radius - BAR_GLOW),
            size = Size(barWidth + 2 * BAR_GLOW, gapTop + radius + 2 * BAR_GLOW),
            cornerRadius = CornerRadius(radius + BAR_GLOW),
        )
        drawRoundRect(
            color = BarrierFill.copy(alpha = 0.20f),
            topLeft = Offset(left - BAR_GLOW, gapBottom - BAR_GLOW),
            size = Size(barWidth + 2 * BAR_GLOW, height - gapBottom + radius + 2 * BAR_GLOW),
            cornerRadius = CornerRadius(radius + BAR_GLOW),
        )

        // Bar above the gap, from the ceiling down to it, and the bar below it.
        // Both run past the corridor edge so only the gap-facing corners round.
        drawRoundRect(
            color = BarrierFill,
            topLeft = Offset(left, -radius),
            size = Size(barWidth, gapTop + radius),
            cornerRadius = CornerRadius(radius),
        )
        drawRoundRect(
            color = BarrierFill,
            topLeft = Offset(left, gapBottom),
            size = Size(barWidth, height - gapBottom + radius),
            cornerRadius = CornerRadius(radius),
        )

        // Rims facing the gap so the opening reads instantly.
        drawRoundRect(
            color = BarrierRim,
            topLeft = Offset(left, gapTop - GAP_RIM),
            size = Size(barWidth, GAP_RIM),
            cornerRadius = CornerRadius(radius),
        )
        drawRoundRect(
            color = BarrierRim,
            topLeft = Offset(left, gapBottom),
            size = Size(barWidth, GAP_RIM),
            cornerRadius = CornerRadius(radius),
        )
    }

    // The ball: a halo that breathes with the microphone, a pulse ring while the
    // mic hears something, then the body, its rim and a small core. The radius is
    // scaled by the box *height* so the drawn circle matches the hitbox the engine
    // tests on both axes (the hitbox is `ballRadius` in normalized units).
    val center = Offset((ballX * width).toFloat(), screenY(ballY, height))
    val radius = (ballRadius * height).toFloat()
    val loudness = soundMeterFill(soundLevel)
    drawCircle(
        color = BallFill.copy(alpha = 0.12f + 0.18f * loudness),
        radius = radius * (1.6f + 0.8f * loudness),
        center = center,
    )
    if (soundDetected) {
        drawCircle(
            color = BallFill.copy(alpha = 0.35f),
            radius = radius * 2.3f,
            center = center,
            style = Stroke(width = 3f),
        )
    }
    drawCircle(color = BallFill, radius = radius, center = center)
    drawCircle(color = BallRim, radius = radius, center = center, style = Stroke(width = 6f))
    drawCircle(color = BallCore, radius = radius * 0.35f, center = center)

    // Sound meter: how much input the microphone is hearing right now.
    val meterWidth = width * 0.6f
    val meterLeft = (width - meterWidth) / 2f
    val meterTop = height - 46f
    val meterHeight = 16f
    val pill = CornerRadius(meterHeight / 2f)
    val fill = soundMeterFill(soundLevel)

    drawRoundRect(
        color = MeterTrack,
        topLeft = Offset(meterLeft, meterTop),
        size = Size(meterWidth, meterHeight),
        cornerRadius = pill,
    )
    if (fill > 0f) {
        if (soundDetected) {
            // A live reading has to be obvious even at a few percent.
            drawRoundRect(
                color = MeterLoud.copy(alpha = 0.30f),
                topLeft = Offset(meterLeft - 4f, meterTop - 4f),
                size = Size(meterWidth * fill + 8f, meterHeight + 8f),
                cornerRadius = CornerRadius(meterHeight / 2f + 4f),
            )
        }
        drawRoundRect(
            color = if (soundDetected) MeterLoud else MeterQuiet,
            topLeft = Offset(meterLeft, meterTop),
            size = Size(meterWidth * fill, meterHeight),
            cornerRadius = pill,
        )
    }
}

/** A neon boundary: a wide soft halo with a crisp line sitting on top of it. */
private fun DrawScope.drawBoundary(y: Float, width: Float) {
    drawLine(
        color = FloorLine.copy(alpha = 0.55f),
        start = Offset(0f, y),
        end = Offset(width, y),
        strokeWidth = 16f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = FloorLine,
        start = Offset(0f, y),
        end = Offset(width, y),
        strokeWidth = 4f,
    )
}

/**
 * Space held = sound, released = silence. Keyboard input is a debug affordance for
 * the emulator and desktop only; phones drive the game with the microphone.
 */
private fun handleDebugKey(event: KeyEvent, onDebugSound: (Boolean) -> Unit): Boolean {
    if (event.key != Key.Spacebar) return false
    return when (event.type) {
        KeyEventType.KeyDown -> {
            onDebugSound(true)
            true
        }
        KeyEventType.KeyUp -> {
            onDebugSound(false)
            true
        }
        else -> false
    }
}

/** `42.5` from milliseconds, without locale-dependent decimal separators. */
internal fun formatTenths(millis: Long): String {
    val tenths = Math.round(millis / 100.0) / 10.0
    return tenths.toString()
}

/** Copy for the end-of-run banner: winning the timer reads differently from being pushed off screen. */
internal fun endBanner(endReason: EndReason?): String = when (endReason) {
    EndReason.TIME_UP -> "TIME'S UP!"
    EndReason.LEFT_BEHIND -> "LEFT BEHIND!"
    null -> ""
}

/**
 * 0..1 fill of the sound meter, so a screaming microphone can never draw past its
 * track. Internal rather than private purely so tests can pin it down (AGENTS.md §5).
 */
internal fun soundMeterFill(level: Float): Float = level.coerceIn(0f, 1f)

/**
 * Corner radius for a bar, in pixels: enough to read as rounded, but capped at
 * half the width (a thin bar can never become an impossible shape) and at
 * [MAX_BAR_RADIUS] so the gap rim — which is drawn with this same radius — still
 * fits inside its own [GAP_RIM] strip. Internal so tests can pin it (AGENTS.md §5).
 */
internal fun barCornerRadius(barWidth: Float): Float =
    minOf(barWidth / 2f, MAX_BAR_RADIUS)

/**
 * Maps a physics height onto screen pixels. The two axes point opposite ways —
 * physics runs 0 = floor -> 1 = ceiling, pixels run 0 = top -> height = bottom —
 * so every vertical draw (the ball and both bars of a barrier) must go through
 * this single inversion, or gravity would pull the ball up and the bars would
 * overlap and hide their gap (docs/games/07-sound-barrier.md).
 *
 * Internal rather than private purely so tests can pin it down (AGENTS.md §5).
 */
internal fun screenY(physicsY: Double, height: Float): Float =
    ((1.0 - physicsY) * height).toFloat()
