package com.rmgames.feature.game.compass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Compass Chaos (docs/games/04-compass-chaos.md): a minimalist compass — fixed
 * ring with the cardinal letters, a red needle that follows the heading, the
 * highlighted target zone (±45° arc around the called direction), the shrinking
 * window bar under the call-out ("POINT EAST!") and the round countdown.
 *
 * Renders [CompassChaosState] only — the [CompassChaosGame] object stays the sole
 * gateway to the host (docs/08). Debug rotation keys (arrows / A-D) feed
 * [onDebugRotate] and are gated on BuildConfig.DEBUG, so release builds ignore them.
 */
@Composable
fun CompassChaosScreen(
    state: StateFlow<CompassChaosState>,
    onDebugRotate: (Float?) -> Unit,
) {
    val s by state.collectAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val density = LocalDensity.current
    val labelPaint = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 20.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event -> handleDebugKey(event, onDebugRotate) },
    ) {
        val ringRadius = minOf(maxWidth, maxHeight) * 0.32f
        val centerX = maxWidth / 2f
        val centerY = maxHeight * 0.56f
        val blocked = s.phase == CompassPhase.BLOCKED

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = BackdropColor)
            if (!blocked) {
                val r = ringRadius.toPx()
                val cx = centerX.toPx()
                val cy = centerY.toPx()
                val center = Offset(cx, cy)

                drawCircle(
                    color = RingColor,
                    radius = r,
                    center = center,
                    style = Stroke(width = 3.dp.toPx()),
                )

                // Target zone: ±45° arc around the called direction (drawArc angles
                // start at 3 o'clock, so a bearing b maps to b − 90).
                val target = s.targetDegrees
                if (target != null) {
                    drawArc(
                        color = TargetZoneColor,
                        startAngle = target - CompassChaosEngine.TOLERANCE_DEGREES - 90f,
                        sweepAngle = 2f * CompassChaosEngine.TOLERANCE_DEGREES,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = 10.dp.toPx()),
                    )
                }

                // Cardinal letters, just outside the ring.
                val offset = 14.dp.toPx()
                val halfText = (labelPaint.ascent() + labelPaint.descent()) / 2f
                val labels = drawContext.canvas.nativeCanvas
                labels.drawText("N", cx, cy - r - offset - halfText, labelPaint)
                labels.drawText("S", cx, cy + r + offset - halfText, labelPaint)
                labels.drawText("E", cx + r + offset, cy - halfText, labelPaint)
                labels.drawText("W", cx - r - offset, cy - halfText, labelPaint)

                // The needle: bearing 0° (north) points up.
                val radians = Math.toRadians(s.heading.toDouble())
                val dirX = sin(radians).toFloat()
                val dirY = (-cos(radians)).toFloat()
                drawLine(
                    color = NeedleColor,
                    start = Offset(cx - dirX * r * 0.18f, cy - dirY * r * 0.18f),
                    end = Offset(cx + dirX * r * 0.86f, cy + dirY * r * 0.86f),
                    strokeWidth = 4.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawCircle(
                    color = NeedleTipColor,
                    radius = 4.dp.toPx(),
                    center = Offset(cx + dirX * r * 0.86f, cy + dirY * r * 0.86f),
                )
                drawCircle(color = NeedleColor, radius = 3.dp.toPx(), center = center)

                // Window bar: shrinks with the time left to align.
                if (s.phase == CompassPhase.RUNNING) {
                    val fraction = (s.windowRemainingMs.toFloat() / s.windowTotalMs.toFloat())
                        .coerceIn(0f, 1f)
                    val barWidth = maxWidth * 0.72f
                    val barLeft = (maxWidth - barWidth) / 2f
                    val barTop = maxHeight * 0.17f
                    val barHeight = 10.dp.toPx()
                    val barSize = Size(barWidth.toPx(), barHeight)
                    drawRect(
                        color = BarTrackColor,
                        topLeft = Offset(barLeft.toPx(), barTop.toPx()),
                        size = barSize,
                    )
                    if (fraction > 0f) {
                        drawRect(
                            color = windowColor(fraction),
                            topLeft = Offset(barLeft.toPx(), barTop.toPx()),
                            size = Size(barWidth.toPx() * fraction, barHeight),
                        )
                    }
                }
            }
        }

        val remainingMs = (CompassChaosEngine.ROUND_MS - s.elapsedMs).coerceAtLeast(0L)
        Text(
            text = countdownLabel(remainingMs),
            color = TextColor,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        )

        if (!blocked) {
            Text(
                text = when (s.phase) {
                    CompassPhase.DONE -> "TIME'S UP!"
                    else -> s.targetDegrees?.let { "POINT ${labelOf(it)}!" } ?: "GET READY…"
                },
                color = CallOutColor,
                fontSize = 34.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.07f),
            )
        }

        if (s.turnDegrees > 0) {
            Text(
                text = "TURN ${s.turnDegrees}°",
                color = HintColor,
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
            )
        }

        if (blocked) {
            Text(
                text = "Compass Chaos needs a magnetometer.\n" +
                    "This device reports no heading, so the run is blocked —\n" +
                    "no score is recorded.",
                color = TextColor,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )
        }
    }
}

private fun handleDebugKey(event: KeyEvent, onDebugRotate: (Float?) -> Unit): Boolean {
    if (!BuildConfig.DEBUG) return false
    val direction = when (event.key) {
        Key.DirectionRight, Key.D -> 1f
        Key.DirectionLeft, Key.A -> -1f
        else -> return false
    }
    return when (event.type) {
        KeyEventType.KeyDown -> {
            onDebugRotate(direction)
            true
        }

        KeyEventType.KeyUp -> {
            onDebugRotate(null)
            true
        }

        else -> false
    }
}

private fun labelOf(degrees: Int): String = when (degrees) {
    0 -> "NORTH"
    90 -> "EAST"
    180 -> "SOUTH"
    270 -> "WEST"
    else -> "?"
}

/** "12.4s" — locale-independent, so the HUD reads the same everywhere. */
private fun countdownLabel(remainingMs: Long): String {
    val tenths = remainingMs / 100
    return "${tenths / 10}.${tenths % 10}s"
}

/** Window bar turns green → orange → red as the call runs out. */
private fun windowColor(fraction: Float): Color = when {
    fraction > 0.5f -> Color(0xFF43A047)
    fraction > 0.25f -> Color(0xFFFB8C00)
    else -> Color(0xFFE53935)
}

private val BackdropColor = Color(0xFF0D1B2A)
private val RingColor = Color(0xFF415A77)
private val TargetZoneColor = Color(0xFFFFB300)
private val NeedleColor = Color(0xFFE53935)
private val NeedleTipColor = Color(0xFFFFF3E0)
private val TextColor = Color(0xFFE0E6ED)
private val HintColor = Color(0xFF9DB2CE)
private val CallOutColor = Color(0xFFFFB300)
private val BarTrackColor = Color(0x33FFFFFF)
