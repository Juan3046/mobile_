package com.rmgames.feature.game.soda

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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow

/**
 * Soda Shake (docs/games/05-soda-shake.md): a cartoon soda can whose pressure
 * gauge fills 5 % per valid shake, plus the minimal HUD (elapsed stopwatch,
 * shake counter, gauge level) and the blocked-sensor message.
 *
 * Renders [SodaShakeState] only — the [SodaShakeGame] object stays the sole
 * gateway to the host (docs/08). The debug shake key (Space/Enter) feeds
 * [onDebugShake] and is gated on BuildConfig.DEBUG, so release builds ignore it.
 */
@Composable
fun SodaShakeScreen(
    state: StateFlow<SodaShakeState>,
    onDebugShake: () -> Unit,
) {
    val s by state.collectAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event -> handleDebugKey(event, onDebugShake) },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = BackdropColor)

            // The can: body, label band, rim and tab (screen-normalized).
            val canWidth = size.width * 0.42f
            val canHeight = size.height * 0.30f
            val canLeft = (size.width - canWidth) / 2f
            val canTop = (size.height - canHeight) / 2f
            drawRoundRect(
                color = CanColor,
                topLeft = Offset(canLeft, canTop),
                size = Size(canWidth, canHeight),
                cornerRadius = CornerRadius(canWidth * 0.14f),
            )
            drawRect(
                color = CanLabelColor,
                topLeft = Offset(canLeft, canTop + canHeight * 0.38f),
                size = Size(canWidth, canHeight * 0.26f),
            )
            drawRect(
                color = CanRimColor,
                topLeft = Offset(canLeft, canTop),
                size = Size(canWidth, canHeight * 0.10f),
            )
            drawCircle(
                color = CanRimColor,
                radius = canWidth * 0.07f,
                center = Offset(size.width / 2f, canTop + canHeight * 0.05f),
            )

            // Pressure gauge (right edge): track + fill from the bottom, 5 % per shake.
            val gaugeWidth = size.width * 0.07f
            val gaugeLeft = size.width - gaugeWidth - size.width * 0.06f
            val gaugeTop = size.height * 0.20f
            val gaugeHeight = size.height * 0.60f
            drawRoundRect(
                color = GaugeTrack,
                topLeft = Offset(gaugeLeft, gaugeTop),
                size = Size(gaugeWidth, gaugeHeight),
                cornerRadius = CornerRadius(gaugeWidth / 2f),
            )
            val fillHeight = gaugeHeight * s.pressure
            if (fillHeight > 0f) {
                drawRoundRect(
                    color = gaugeColor(s.pressure),
                    topLeft = Offset(gaugeLeft, gaugeTop + gaugeHeight - fillHeight),
                    size = Size(gaugeWidth, fillHeight),
                    cornerRadius = CornerRadius(gaugeWidth / 2f),
                )
            }

            // Pop: the lid flies off and the fizz rises with the animation progress.
            if (s.popProgress > 0f) {
                val rise = s.popProgress * s.popProgress * size.height * 0.35f
                drawRect(
                    color = CanRimColor,
                    topLeft = Offset(canLeft, canTop - canHeight * 0.10f - rise),
                    size = Size(canWidth, canHeight * 0.10f),
                )
                repeat(5) { i ->
                    val spread = (i - 2f) * canWidth * 0.16f
                    drawCircle(
                        color = FizzColor.copy(alpha = (1f - s.popProgress).coerceAtLeast(0.3f)),
                        radius = canWidth * (0.05f + 0.02f * i),
                        center = Offset(size.width / 2f + spread, canTop - rise * 0.6f - i * size.height * 0.03f),
                    )
                }
            }
        }

        Text(
            text = elapsedLabel(s.elapsedMs),
            color = HudTextColor,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        )

        Text(
            text = "${(s.pressure * 100).toInt()}%",
            color = gaugeColor(s.pressure),
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        )

        Text(
            text = "SHAKES ${s.shakes}/${SodaShakeEngine.SHAKES_TO_POP}",
            color = HudTextColor,
            fontSize = 22.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )

        if (s.phase == SodaPhase.POPPING || s.phase == SodaPhase.DONE) {
            Text(
                text = "POP! ${elapsedLabel(s.scoreCentis * 10)}",
                color = PopTextColor,
                fontSize = 44.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (s.phase == SodaPhase.BLOCKED) {
            Text(
                text = "Soda Shake needs a motion sensor.\n" +
                    "This device does not report accelerometer data,\n" +
                    "so the run is blocked — no score is recorded.",
                color = HudTextColor,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )
        }
    }
}

private fun handleDebugKey(event: KeyEvent, onDebugShake: () -> Unit): Boolean {
    if (!BuildConfig.DEBUG) return false
    if (event.key != Key.Spacebar && event.key != Key.Enter) return false
    return when (event.type) {
        KeyEventType.KeyDown -> {
            onDebugShake()
            true
        }

        else -> false
    }
}

/** "62.4s" — locale-independent, so the HUD reads the same everywhere. */
private fun elapsedLabel(elapsedMs: Long): String {
    val tenths = elapsedMs / 100
    return "${tenths / 10}.${tenths % 10}s"
}

/** Gauge rises green → orange → red as the can approaches its pop. */
private fun gaugeColor(pressure: Float): Color = when {
    pressure < 0.60f -> Color(0xFF43A047)
    pressure < 0.85f -> Color(0xFFFB8C00)
    else -> Color(0xFFE53935)
}

private val BackdropColor = Color(0xFFFFF8E1)
private val CanColor = Color(0xFFE53935)
private val CanLabelColor = Color(0xFFFFF3E0)
private val CanRimColor = Color(0xFFB0BEC5)
private val FizzColor = Color(0xFFFFFFFF)
private val GaugeTrack = Color(0x33000000)
private val PopTextColor = Color(0xFFE53935)
private val HudTextColor = Color(0xFF3E2723)
