package com.rmgames.feature.game.sound

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Decorative sunset behind the corridor (docs/games/07-sound-barrier.md — Art Direction):
 * a warm gradient sky, a striped setting sun, drifting clouds and a hill silhouette.
 * Pure decoration: nothing here touches the physics.
 */

// Sky from dusk at the top to the warm horizon at the bottom.
private val SkyTop = Color(0xFF241848)
private val SkyHigh = Color(0xFF5B2A6E)
private val SkyMid = Color(0xFFC9476B)
private val SkyLow = Color(0xFFF4845F)
private val Horizon = Color(0xFFFFC872)
private val SunTop = Color(0xFFFFE27A)
private val SunBottom = Color(0xFFFF7A59)
private val CloudColor = Color(0xFFFFF1E6)
private val HillFar = Color(0xFF7A3B6E)
private val HillNear = Color(0xFF3B2150)

/** A cloud at a normalized spot; [speed] is in screen-widths per second (leftwards). */
data class Cloud(val x: Float, val y: Float, val scale: Float, val speed: Float)

/** Clouds wrap between these normalized x positions (just off each edge). */
const val CLOUD_MIN_X = -0.3f
const val CLOUD_MAX_X = 1.3f

/** Where [cloud] is after [elapsedMs]: drifting left, wrapping back in on the right. */
fun cloudX(cloud: Cloud, elapsedMs: Long): Float {
    val span = CLOUD_MAX_X - CLOUD_MIN_X
    val raw = cloud.x - cloud.speed * elapsedMs / 1000f - CLOUD_MIN_X
    return ((raw % span) + span) % span + CLOUD_MIN_X
}

/** Slower than the corridor (0.20–0.37 widths/s), so they read as far away. */
internal val SUNSET_CLOUDS = listOf(
    Cloud(x = 0.15f, y = 0.18f, scale = 1.1f, speed = 0.018f),
    Cloud(x = 0.62f, y = 0.10f, scale = 0.8f, speed = 0.012f),
    Cloud(x = 0.95f, y = 0.27f, scale = 1.3f, speed = 0.024f),
    Cloud(x = 0.38f, y = 0.36f, scale = 0.7f, speed = 0.015f),
    Cloud(x = 1.18f, y = 0.05f, scale = 0.9f, speed = 0.010f),
)

internal fun DrawScope.drawSunset(elapsedMs: Long) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(SkyTop, SkyHigh, SkyMid, SkyLow, Horizon)))

    // Setting sun with retro stripes cut across its lower half.
    val sunR = minOf(w, h) * 0.24f
    val sun = Offset(w * 0.68f, h * 0.70f)
    drawCircle(Horizon.copy(alpha = 0.35f), sunR * 1.35f, sun)
    drawCircle(
        Brush.verticalGradient(listOf(SunTop, SunBottom), startY = sun.y - sunR, endY = sun.y + sunR),
        sunR,
        sun,
    )
    for (i in 0 until 5) {
        val stripeY = sun.y + sunR * (0.12f + i * 0.2f)
        val stripeH = sunR * (0.04f + i * 0.025f)
        drawRect(SkyLow, Offset(sun.x - sunR, stripeY), Size(sunR * 2, stripeH))
    }

    // Clouds: a flat base with three puffs.
    SUNSET_CLOUDS.forEach { cloud ->
        val cx = cloudX(cloud, elapsedMs) * w
        val cy = cloud.y * h
        val u = minOf(w, h) * 0.06f * cloud.scale
        val color = CloudColor.copy(alpha = 0.75f)
        drawCircle(color, u, Offset(cx - u * 1.1f, cy + u * 0.25f))
        drawCircle(color, u * 1.35f, Offset(cx, cy - u * 0.1f))
        drawCircle(color, u * 0.95f, Offset(cx + u * 1.2f, cy + u * 0.3f))
        drawRect(color, Offset(cx - u * 1.1f, cy + u * 0.25f), Size(u * 2.3f, u))
    }

    // Two hill silhouettes on the horizon.
    drawHill(HillFar, baseY = h * 0.86f, height = h * 0.09f, phase = 0.3f)
    drawHill(HillNear, baseY = h * 0.92f, height = h * 0.07f, phase = 1.4f)
}

private fun DrawScope.drawHill(color: Color, baseY: Float, height: Float, phase: Float) {
    val w = size.width
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, baseY)
        val steps = 24
        for (i in 0..steps) {
            val x = w * i / steps
            val y = baseY - height * (0.5f + 0.5f * kotlin.math.sin(phase + i * 0.55f).toFloat())
            lineTo(x, y)
        }
        lineTo(w, size.height)
        close()
    }
    drawPath(path, color)
}
