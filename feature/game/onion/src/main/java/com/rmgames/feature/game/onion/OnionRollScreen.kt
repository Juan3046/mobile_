package com.rmgames.feature.game.onion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow

/**
 * Top-down view of the checkout belt (docs/games/02-onion-roll.md): scrolling
 * belt, the approaching end of the belt, the groceries and the onion, plus the
 * minimal HUD (elapsed time + end-of-belt proximity gauge).
 *
 * Renders [OnionRollState] only — the [Game] object stays the sole gateway to the
 * host (docs/08). Debug keys (A/D, arrows) feed [onDebugSteer] and are gated on
 * BuildConfig.DEBUG, so release builds ignore them entirely.
 */
@Composable
fun OnionRollScreen(
    state: StateFlow<OnionRollState>,
    onDebugSteer: (Float?) -> Unit,
) {
    val s by state.collectAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event -> handleDebugKey(event, onDebugSteer) },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Belt surface + slats scrolling toward the player (the belt's flow).
            drawRect(color = BeltColor)
            val slatGap = size.height * 0.07f
            val travel = (s.elapsedMs / 1000f) * s.beltSpeed * size.height
            var slatY = (travel % slatGap) - slatGap
            while (slatY < size.height) {
                drawRect(color = SlatColor, topLeft = Offset(0f, slatY), size = Size(size.width, 4f))
                slatY += slatGap
            }

            // The end of the belt rises from the bottom as the run progresses; past it = void.
            val endY = (1f - s.progress * (1f - ONION_Y)) * size.height
            drawRect(color = VoidColor, topLeft = Offset(0f, endY), size = Size(size.width, size.height - endY))
            drawRect(color = EndLineColor, topLeft = Offset(0f, endY - 6f), size = Size(size.width, 6f))

            // Groceries riding the belt toward the onion.
            s.fruits.forEach { fruit ->
                val cx = fruit.x * size.width
                val cy = fruit.y * size.height
                if (fruit.type == FruitType.BANANA) {
                    drawRoundRect(
                        color = BananaColor,
                        topLeft = Offset(cx - fruit.type.halfWidth * size.width, cy - fruit.type.halfHeight * size.height),
                        size = Size(fruit.type.halfWidth * 2f * size.width, fruit.type.halfHeight * 2f * size.height),
                        cornerRadius = CornerRadius(16f),
                    )
                } else {
                    drawCircle(
                        color = if (fruit.type == FruitType.CHERRY) CherryColor else AppleColor,
                        radius = fruit.type.halfWidth * size.width,
                        center = Offset(cx, cy),
                    )
                }
            }

            // The onion: an accelerating, tumbling drop once the belt runs out.
            val onionRadius = ONION_HALF_W * size.width
            val onionCenter = Offset(
                s.onionX * size.width,
                ONION_Y * size.height + fallDropFraction(s.fallProgress) * size.height * FALL_DROP_SCREENS,
            )
            rotate(degrees = fallTiltDeg(s.fallProgress), pivot = onionCenter) {
                drawCircle(color = OnionColor, radius = onionRadius, center = onionCenter)
                val face = faceLayout(onionRadius)
                drawCircle(
                    color = FaceColor,
                    radius = face.eyeRadius,
                    center = onionCenter + Offset(face.leftEyeX, face.leftEyeY),
                )
                drawCircle(
                    color = FaceColor,
                    radius = face.eyeRadius,
                    center = onionCenter + Offset(face.rightEyeX, face.rightEyeY),
                )
                // Lower half of a circle = smile.
                drawArc(
                    color = FaceColor,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = onionCenter + Offset(face.smileCx - face.smileRadius, face.smileCy - face.smileRadius),
                    size = Size(face.smileRadius * 2f, face.smileRadius * 2f),
                    style = Stroke(width = face.eyeRadius * 0.8f, cap = StrokeCap.Round),
                )
            }

            // End-of-belt proximity gauge (top-right; the host's score sits top-centre).
            val gaugeWidth = size.width * 0.3f
            val gaugeLeft = size.width - gaugeWidth - 24f
            drawRect(color = GaugeTrack, topLeft = Offset(gaugeLeft, 28f), size = Size(gaugeWidth, 8f))
            drawRect(color = GaugeFill, topLeft = Offset(gaugeLeft, 28f), size = Size(gaugeWidth * s.progress, 8f))
        }

        Text(
            text = elapsedLabel(s.elapsedMs),
            color = HudTextColor,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        )
    }
}

private fun handleDebugKey(event: KeyEvent, onDebugSteer: (Float?) -> Unit): Boolean {
    if (!BuildConfig.DEBUG) return false
    val steer = when (event.key) {
        Key.A, Key.DirectionLeft -> -1f
        Key.D, Key.DirectionRight -> 1f
        else -> return false
    }
    return when (event.type) {
        KeyEventType.KeyDown -> {
            onDebugSteer(steer)
            true
        }

        KeyEventType.KeyUp -> {
            onDebugSteer(null)
            true
        }

        else -> false
    }
}

private val BeltColor = Color(0xFFB0BEC5)
private val SlatColor = Color(0xFF90A4AE)
private val VoidColor = Color(0xFF263238)
private val EndLineColor = Color(0xFFFF7043)
private val CherryColor = Color(0xFFEF5350)
private val AppleColor = Color(0xFFE53935)
private val BananaColor = Color(0xFFFFEE58)
private val OnionColor = Color(0xFFCE93D8)
private val GaugeTrack = Color(0x66FFFFFF)
private val GaugeFill = Color(0xFFFF7043)
private val HudTextColor = Color(0xFF263238)
private val FaceColor = Color(0xFF4A148C)

/** Full fall drop distance, in screen heights. */
private const val FALL_DROP_SCREENS = 0.6f
