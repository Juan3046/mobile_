package com.rmgames.feature.game.escape

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun EscapePorubaScene(game: EscapePorubaGame) {
    val state by game.state.collectAsState()
    val resultVisible by game.resultVisible.collectAsState()
    val context = LocalContext.current
    val vibrator = remember(context) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }
    val hapticStage = (state.elapsedMs / 5_000L).toInt()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(hapticStage, state.phase) {
        if (state.phase == EscapePhase.RUNNING && state.elapsedMs >= 60_000L && vibrator?.hasVibrator() == true) {
            vibrator.vibrate(VibrationEffect.createOneShot(45L, 55))
        }
    }
    val escapeProgress by animateFloatAsState(
        targetValue = if (state.phase == EscapePhase.ESCAPED) 1f else 0f,
        animationSpec = tween(durationMillis = 850),
        label = "escape-animation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(game) {
                detectDragGestures(
                    onDragStart = { game.dragTo(screenToRoadX(it.x / size.width.coerceAtLeast(1).toFloat())) },
                    onDrag = { change, _ ->
                        game.dragTo(screenToRoadX(change.position.x / size.width.coerceAtLeast(1).toFloat()))
                        change.consume()
                    },
                )
            }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { handleDebugKey(it, game) },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawPorubaWorld(state, escapeProgress)
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "ESCAPE PORUBA",
                color = HudWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(timeLabel(state.elapsedMs), color = HudWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${state.distanceMeters} m", color = HudWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }

        val zone = zoneFor(state.elapsedMs)
        Text(
            zone,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 86.dp),
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )

        if (state.elapsedMs in 74_000L..76_000L) {
            AlertText("GET OUT.", Modifier.align(Alignment.Center))
        } else if (state.elapsedMs in 84_000L..86_000L) {
            AlertText("ALMOST OUT.", Modifier.align(Alignment.Center))
        }

        when (state.phase) {
            EscapePhase.CRASHED -> if (resultVisible) ResultDialog(
                title = "YOU'RE STILL IN PORUBA",
                detail = "SURVIVED ${timeLabel(state.elapsedMs)}  ·  ${state.distanceMeters} m",
            )
            EscapePhase.ESCAPED -> if (resultVisible) ResultDialog(
                title = "YOU ESCAPED PORUBA",
                detail = "ESCAPE TIME: 01:30.00\nNever come back.",
            )
            EscapePhase.READY, EscapePhase.RUNNING -> Unit
        }
    }
}

@Composable
private fun AlertText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = Color.White,
        fontSize = 27.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
    )
}

@Composable
private fun ResultDialog(title: String, detail: String) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title, fontWeight = FontWeight.Black, textAlign = TextAlign.Center) },
        text = { Text(detail, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            TextButton(onClick = { backDispatcher?.onBackPressed() }) {
                Text("BACK TO MENU")
            }
        },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPorubaWorld(
    state: EscapePorubaState,
    escapeProgress: Float,
) {
    val w = size.width
    val h = size.height
    val difficultyTint = state.difficulty * 0.18f
    drawRect(Color(0xFF9EABB2))

    // Distant paneláks with repetitive windows frame the road, cycling by zone.
    val buildingScroll = (state.elapsedMs / 250L) % 48L
    for (side in 0..1) {
        val x = if (side == 0) 0f else w * 0.79f
        val buildingWidth = w * 0.21f
        for (index in -1..3) {
            val y = index * h * 0.28f + buildingScroll * h / 180f
            drawRoundRect(
                color = if (side == 0) Color(0xFF677982) else Color(0xFF7B8587),
                topLeft = Offset(x, y),
                size = Size(buildingWidth, h * 0.23f),
                cornerRadius = CornerRadius(3f),
            )
            for (row in 0..3) for (column in 0..2) {
                val wx = x + buildingWidth * (0.16f + column * 0.27f)
                val wy = y + h * (0.025f + row * 0.047f)
                drawRect(Color(0xFFC4D0D1), Offset(wx, wy), Size(buildingWidth * 0.12f, h * 0.024f))
            }
        }
    }

    // Grass verges, wide Poruba avenue, sidewalks and tram rails.
    drawRect(Color(0xFF788B69), Offset(w * 0.20f, 0f), Size(w * 0.06f, h))
    drawRect(Color(0xFF788B69), Offset(w * 0.74f, 0f), Size(w * 0.06f, h))
    drawRect(Color(0xFFB5B5AE), Offset(w * 0.26f, 0f), Size(w * 0.48f, h))
    drawRect(Color(0xFF4A5255), Offset(w * 0.31f, 0f), Size(w * 0.38f, h))
    drawRect(Color(0xFF77766D), Offset(w * 0.495f, 0f), Size(w * 0.01f, h))
    val dashShift = (state.elapsedMs / 8L % 80L).toFloat()
    var dashY = -80f + dashShift
    while (dashY < h) {
        drawRoundRect(Color(0xFFBAC0B8), Offset(w * 0.495f, dashY), Size(w * 0.01f, 35f), CornerRadius(4f))
        dashY += 80f
    }
    // Red tram rails: a subtle Ostrava landmark, not a collision object.
    drawRect(Color(0xFFAA4B43), Offset(w * 0.39f, 0f), Size(w * 0.008f, h))
    drawRect(Color(0xFFAA4B43), Offset(w * 0.602f, 0f), Size(w * 0.008f, h))

    state.hazards.forEach { hazard ->
        val cx = w * EscapePorubaEngine.laneCenter(hazard.lane)
        val cy = h * hazard.y
        drawHazard(
            hazard.kind,
            cx,
            cy,
            w * EscapePorubaEngine.ROAD_WIDTH / EscapePorubaEngine.LANE_COUNT * 0.46f,
            h * 0.045f,
        )
    }

    drawStudent(
        w * (state.playerX + escapeProgress * 0.28f),
        h * (EscapePorubaEngine.PLAYER_Y - escapeProgress * 0.16f),
        w * 0.07f,
        h * 0.075f,
    )

    if (state.elapsedMs >= 85_000L && state.phase == EscapePhase.RUNNING) {
        drawRoundRect(Color(0xFFDFD9C6), Offset(w * 0.27f, h * 0.13f), Size(w * 0.46f, h * 0.075f), CornerRadius(10f))
    }
    if (difficultyTint > 0f) drawRect(Color(0xFF6E1D1D).copy(alpha = difficultyTint), size = size)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHazard(
    kind: EscapeHazardKind,
    cx: Float,
    cy: Float,
    halfW: Float,
    halfH: Float,
) {
    val color = when (kind) {
        EscapeHazardKind.TRAM -> Color(0xFFC83F37)
        EscapeHazardKind.CAR -> Color(0xFF41677D)
        EscapeHazardKind.BUS -> Color(0xFFD6A23D)
        EscapeHazardKind.BICYCLE, EscapeHazardKind.SCOOTER -> Color(0xFFDFE1D8)
        EscapeHazardKind.PEDESTRIAN -> Color(0xFFB08B70)
        EscapeHazardKind.ICE -> Color(0xFFB8DDE4)
        EscapeHazardKind.POTHOLE -> Color(0xFF272D30)
        EscapeHazardKind.ROADWORKS -> Color(0xFFE88A35)
        EscapeHazardKind.CONTAINER -> Color(0xFF59765C)
    }
    drawRoundRect(color, Offset(cx - halfW, cy - halfH), Size(halfW * 2, halfH * 2), CornerRadius(halfH * 0.35f))
    when (kind) {
        EscapeHazardKind.TRAM, EscapeHazardKind.BUS, EscapeHazardKind.CAR -> {
            drawRect(Color(0xFFB8D2D9), Offset(cx - halfW * 0.6f, cy - halfH * 0.62f), Size(halfW * 1.2f, halfH * 0.35f))
            drawCircle(Color(0xFF22282B), halfH * 0.22f, Offset(cx - halfW * 0.55f, cy + halfH * 0.7f))
            drawCircle(Color(0xFF22282B), halfH * 0.22f, Offset(cx + halfW * 0.55f, cy + halfH * 0.7f))
        }
        EscapeHazardKind.ROADWORKS -> {
            drawRect(Color(0xFFEAD9AE), Offset(cx - halfW * 0.7f, cy - halfH * 0.12f), Size(halfW * 1.4f, halfH * 0.25f))
        }
        EscapeHazardKind.POTHOLE -> drawOval(color, Offset(cx - halfW, cy - halfH * 0.7f), Size(halfW * 2, halfH * 1.4f))
        else -> drawCircle(Color(0xFF252B2D), halfH * 0.22f, Offset(cx, cy - halfH * 0.38f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStudent(cx: Float, cy: Float, width: Float, height: Float) {
    // Backpack, hoodie, head and feet; simple silhouettes remain readable at phone scale.
    drawRoundRect(Color(0xFF614B3E), Offset(cx - width * 0.38f, cy - height * 0.12f), Size(width * 0.76f, height * 0.8f), CornerRadius(width * 0.15f))
    drawRoundRect(Color(0xFF31566A), Offset(cx - width * 0.28f, cy - height * 0.15f), Size(width * 0.56f, height * 0.63f), CornerRadius(width * 0.18f))
    drawCircle(Color(0xFFE1B995), width * 0.2f, Offset(cx, cy - height * 0.39f))
    drawRect(Color(0xFF27343A), Offset(cx - width * 0.32f, cy + height * 0.38f), Size(width * 0.25f, height * 0.22f))
    drawRect(Color(0xFF27343A), Offset(cx + width * 0.07f, cy + height * 0.38f), Size(width * 0.25f, height * 0.22f))
}

private fun timeLabel(elapsedMs: Long): String {
    val tenths = elapsedMs / 100L
    return "${tenths / 10}.${tenths % 10}s"
}

private fun handleDebugKey(event: KeyEvent, game: EscapePorubaGame): Boolean {
    if (!BuildConfig.DEBUG || event.type != KeyEventType.KeyDown) return false
    val direction = when (event.key) {
        Key.A, Key.DirectionLeft -> -1
        Key.D, Key.DirectionRight -> 1
        else -> return false
    }
    game.debugStep(direction)
    return true
}

private fun screenToRoadX(screenX: Float): Float =
    screenX.coerceIn(EscapePorubaEngine.PLAYER_X_MIN, EscapePorubaEngine.PLAYER_X_MAX)

private fun zoneFor(elapsedMs: Long): String = when (elapsedMs) {
    in 0L..14_999L -> "VŠB CAMPUS · PORUBA"
    in 15_000L..29_999L -> "PORUBA DORMS"
    in 30_000L..44_999L -> "PORUBA STREETS"
    in 45_000L..59_999L -> "MAIN AVENUE"
    in 60_000L..74_999L -> "TRAM ZONE"
    in 75_000L..84_999L -> "EDGE OF PORUBA"
    else -> "OSTRAVA CENTRUM →"
}

private val HudWhite = Color(0xFFF4F3EC)
