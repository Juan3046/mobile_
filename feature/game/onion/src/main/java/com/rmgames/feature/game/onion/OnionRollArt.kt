package com.rmgames.feature.game.onion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Procedural sprites for Onion Roll (docs/games/02-onion-roll.md — Art Direction):
 * stylised-realistic shapes with gradients, highlights and soft shadows, drawn
 * straight on the Compose Canvas (no bitmap assets).
 *
 * Every sprite is drawn in a **unit box** (-1..1 on both axes) that is stretched
 * onto the entity's drawn box, so what you see always matches the engine's sizes.
 * Swapping in bitmap sprites later only means replacing these functions.
 */

/** Runs [block] in unit coordinates mapped onto the box centred at [center] with half-extents [rx]×[ry]. */
private inline fun DrawScope.inUnitBox(center: Offset, rx: Float, ry: Float, block: DrawScope.() -> Unit) {
    withTransform({
        translate(center.x, center.y)
        scale(rx, ry, pivot = Offset.Zero)
    }, block)
}

/** Soft contact shadow: a dark radial blob, offset down-right (light comes from the top-left). */
private fun DrawScope.softShadow(center: Offset, radius: Float, alpha: Float = 0.45f) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.Black.copy(alpha = alpha),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/** Small specular glint. */
private fun DrawScope.glint(center: Offset, radius: Float, alpha: Float = 0.6f) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = alpha),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

// --- belt ---

/**
 * Ribbed rubber belt between brushed-steel rails, with ribs scrolled by [scrollPx];
 * below [endY] the belt wraps around a steel roller and drops into darkness.
 */
internal fun DrawScope.drawBelt(scrollPx: Float, endY: Float) {
    val w = size.width
    val h = size.height
    val rail = w * 0.022f

    // Rubber surface: darker toward the rails (lighting falloff across the belt).
    drawRect(
        brush = Brush.horizontalGradient(
            0f to BeltEdge,
            0.18f to BeltMid,
            0.5f to BeltCentre,
            0.82f to BeltMid,
            1f to BeltEdge,
        ),
    )

    // Ribs: a dark groove with a thin lit lip just behind it, scrolling with the belt.
    val ribGap = h * 0.032f
    var y = (scrollPx % ribGap) - ribGap
    while (y < endY) {
        drawRect(color = RibShadow, topLeft = Offset(rail, y), size = Size(w - 2 * rail, 3f))
        drawRect(color = RibLight, topLeft = Offset(rail, y + 3f), size = Size(w - 2 * rail, 1.5f))
        y += ribGap
    }

    // Brushed-steel rails with the shadow they cast onto the rubber.
    for (left in listOf(0f, w - rail)) {
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(SteelDark, SteelLight, SteelMid, SteelDark),
                startX = left,
                endX = left + rail,
            ),
            topLeft = Offset(left, 0f),
            size = Size(rail, h),
        )
    }
    val railShadow = rail * 0.9f
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent), rail, rail + railShadow),
        topLeft = Offset(rail, 0f),
        size = Size(railShadow, h),
    )
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)), w - rail - railShadow, w - rail),
        topLeft = Offset(w - rail - railShadow, 0f),
        size = Size(railShadow, h),
    )

    // Past the end of the belt: a dark drop with a shadow under the roller.
    if (endY < h) {
        drawRect(
            brush = Brush.verticalGradient(listOf(DropTop, DropBottom), startY = endY, endY = h),
            topLeft = Offset(0f, endY),
            size = Size(w, h - endY),
        )
        val shadowH = h * 0.03f
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent), endY, endY + shadowH),
            topLeft = Offset(0f, endY),
            size = Size(w, shadowH),
        )
    }

    // Steel end roller: a horizontal cylinder (bright band = reflected ceiling light).
    val rollerH = h * 0.024f
    val rollerTop = endY - rollerH * 0.5f
    drawRect(
        brush = Brush.verticalGradient(
            0f to SteelDark,
            0.3f to SteelLight,
            0.55f to SteelMid,
            1f to SteelShadow,
            startY = rollerTop,
            endY = rollerTop + rollerH,
        ),
        topLeft = Offset(0f, rollerTop),
        size = Size(w, rollerH),
    )
}

/**
 * Red danger light at the bottom (docs/games/02 — HUD): a glow rising from the end
 * roller onto the belt, a red tint over the drop and warning lamps on the roller.
 * [alpha] comes from dangerGlowAlpha; [danger] (0..1) makes the glow taller.
 */
internal fun DrawScope.drawDangerGlow(endY: Float, alpha: Float, danger: Float) {
    val w = size.width
    val h = size.height
    val glowH = h * (0.14f + 0.16f * danger)

    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.6f to DangerRed.copy(alpha = alpha * 0.45f),
            1f to DangerRed.copy(alpha = alpha),
            startY = endY - glowH,
            endY = endY,
        ),
        topLeft = Offset(0f, endY - glowH),
        size = Size(w, glowH),
    )
    if (endY < h) {
        drawRect(
            color = DangerRed.copy(alpha = alpha * 0.5f),
            topLeft = Offset(0f, endY),
            size = Size(w, h - endY),
        )
    }

    // Warning lamps along the roller, blinking with the glow.
    val lampCount = 5
    val lampR = h * 0.009f
    val lampAlpha = (alpha * 1.4f).coerceAtMost(1f)
    for (i in 0 until lampCount) {
        val c = Offset(w * (i + 0.5f) / lampCount, endY)
        drawCircle(
            brush = Brush.radialGradient(
                0f to DangerRed.copy(alpha = lampAlpha),
                1f to Color.Transparent,
                center = c,
                radius = lampR * 4f,
            ),
            radius = lampR * 4f,
            center = c,
        )
        drawCircle(LampCore.copy(alpha = 0.5f + 0.5f * lampAlpha), radius = lampR, center = c)
        drawCircle(Color.White.copy(alpha = 0.7f * lampAlpha), radius = lampR * 0.4f, center = c)
    }
}

// --- fruits ---

/** Draws [fruit] filling its drawn box on a canvas of the current size. */
internal fun DrawScope.drawFruit(fruit: Fruit) {
    val center = Offset(fruit.x * size.width, fruit.y * size.height)
    val rx = fruit.type.halfWidth * size.width
    val ry = fruit.type.halfHeight * size.height
    inUnitBox(center, rx, ry) {
        when (fruit.type) {
            FruitType.APPLE -> drawApple()
            FruitType.CHERRY -> drawCherries()
            FruitType.BANANA -> drawBanana()
        }
    }
}

/** Top-down apple: glossy red sphere, stem cavity, stem and a leaf. */
private fun DrawScope.drawApple() {
    softShadow(Offset(0.18f, 0.24f), 1.2f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to AppleLight,
            0.55f to AppleMid,
            1f to AppleDark,
            center = Offset(-0.32f, -0.36f),
            radius = 1.55f,
        ),
        radius = 1f,
        center = Offset.Zero,
    )
    // Faint yellow blush on the lit side for a natural, non-plastic skin.
    drawCircle(
        brush = Brush.radialGradient(
            0f to AppleBlush.copy(alpha = 0.35f),
            1f to Color.Transparent,
            center = Offset(-0.45f, 0.35f),
            radius = 0.6f,
        ),
        radius = 0.6f,
        center = Offset(-0.45f, 0.35f),
    )
    // Stem cavity.
    drawCircle(
        brush = Brush.radialGradient(
            0f to AppleCavity.copy(alpha = 0.8f),
            1f to Color.Transparent,
            center = Offset(0.04f, 0.02f),
            radius = 0.3f,
        ),
        radius = 0.3f,
        center = Offset(0.04f, 0.02f),
    )
    drawLine(StemBrown, Offset(0.04f, 0.02f), Offset(0.22f, -0.3f), strokeWidth = 0.1f, cap = StrokeCap.Round)
    val leaf = Path().apply {
        moveTo(0.2f, -0.26f)
        quadraticTo(0.42f, -0.78f, 0.86f, -0.6f)
        quadraticTo(0.62f, -0.16f, 0.2f, -0.26f)
        close()
    }
    drawPath(leaf, Brush.linearGradient(listOf(LeafLight, LeafDark), Offset(0.2f, -0.6f), Offset(0.8f, -0.3f)))
    drawLine(LeafDark.copy(alpha = 0.8f), Offset(0.22f, -0.27f), Offset(0.8f, -0.58f), strokeWidth = 0.035f)
    glint(Offset(-0.42f, -0.46f), 0.36f)
}

/** A pair of glossy cherries joined by their stems. */
private fun DrawScope.drawCherries() {
    val left = Offset(-0.44f, 0.36f)
    val right = Offset(0.46f, 0.44f)
    val r = 0.5f
    softShadow(left + Offset(0.12f, 0.16f), r * 1.25f)
    softShadow(right + Offset(0.12f, 0.16f), r * 1.25f)

    val joint = Offset(0.12f, -0.8f)
    val stems = Path().apply {
        moveTo(left.x + 0.05f, left.y - r * 0.8f)
        quadraticTo(-0.35f, -0.5f, joint.x, joint.y)
        moveTo(right.x - 0.02f, right.y - r * 0.8f)
        quadraticTo(0.42f, -0.35f, joint.x, joint.y)
    }
    drawPath(stems, StemGreen, style = Stroke(width = 0.09f, cap = StrokeCap.Round))
    val leaf = Path().apply {
        moveTo(joint.x, joint.y)
        quadraticTo(0.45f, -1.05f, 0.85f, -0.82f)
        quadraticTo(0.5f, -0.6f, joint.x, joint.y)
        close()
    }
    drawPath(leaf, Brush.linearGradient(listOf(LeafLight, LeafDark), Offset(0.1f, -0.9f), Offset(0.8f, -0.75f)))

    for (c in listOf(left, right)) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to CherryLight,
                0.6f to CherryMid,
                1f to CherryDark,
                center = c + Offset(-0.18f, -0.2f),
                radius = r * 1.5f,
            ),
            radius = r,
            center = c,
        )
        glint(c + Offset(-0.2f, -0.22f), r * 0.38f, alpha = 0.75f)
        drawCircle(CherryDark, radius = 0.06f, center = c + Offset(0.02f, -r * 0.82f)) // stem dimple
    }
}

/** Top-down banana: curved crescent with shading, a lit ridge and brown tips. */
private fun DrawScope.drawBanana() {
    fun crescent(dx: Float = 0f, dy: Float = 0f) = Path().apply {
        moveTo(-0.95f + dx, -0.38f + dy)
        quadraticTo(0f + dx, 2.2f + dy, 0.95f + dx, -0.38f + dy)
        quadraticTo(0.86f + dx, -0.62f + dy, 0.8f + dx, -0.58f + dy)
        quadraticTo(0f + dx, 0.85f + dy, -0.8f + dx, -0.58f + dy)
        quadraticTo(-0.86f + dx, -0.62f + dy, -0.95f + dx, -0.38f + dy)
        close()
    }
    drawPath(crescent(0.03f, 0.3f), Color.Black.copy(alpha = 0.3f))
    drawPath(
        crescent(),
        Brush.verticalGradient(
            0f to BananaLight,
            0.45f to BananaMid,
            1f to BananaDark,
            startY = -0.6f,
            endY = 0.95f,
        ),
    )
    // Lit ridge along the inner curve.
    val ridge = Path().apply {
        moveTo(-0.72f, -0.42f)
        quadraticTo(0f, 1.2f, 0.72f, -0.42f)
        quadraticTo(0f, 1.0f, -0.72f, -0.42f)
        close()
    }
    drawPath(ridge, Color.White.copy(alpha = 0.35f))
    // Brown tips: stalk on the left, flower end on the right.
    drawOval(BananaTip, topLeft = Offset(-1f, -0.66f), size = Size(0.12f, 0.42f))
    drawOval(BananaTip, topLeft = Offset(0.9f, -0.6f), size = Size(0.08f, 0.3f))
    // A few freckles.
    drawOval(BananaTip.copy(alpha = 0.5f), topLeft = Offset(-0.3f, 0.45f), size = Size(0.03f, 0.12f))
    drawOval(BananaTip.copy(alpha = 0.5f), topLeft = Offset(0.25f, 0.5f), size = Size(0.025f, 0.1f))
}

// --- onion ---

/**
 * The red-onion hero with its face, centred at [center] with drawn half-extents
 * [rx]×[ry], rotated by [tiltDeg]. [shadowAlpha] fades the contact shadow as it falls.
 */
internal fun DrawScope.drawOnion(center: Offset, rx: Float, ry: Float, tiltDeg: Float, shadowAlpha: Float) {
    if (shadowAlpha > 0f) {
        inUnitBox(center, rx, ry) { softShadow(Offset(0.16f, 0.3f), 1.25f, alpha = 0.5f * shadowAlpha) }
    }
    rotate(degrees = tiltDeg, pivot = center) {
        inUnitBox(center, rx, ry) {
            // Bulb + pointed neck.
            val bulb = Path().apply {
                moveTo(0f, -1.02f)
                cubicTo(0.18f, -0.8f, 0.92f, -0.62f, 0.92f, 0.12f)
                cubicTo(0.92f, 0.7f, 0.5f, 0.98f, 0f, 0.98f)
                cubicTo(-0.5f, 0.98f, -0.92f, 0.7f, -0.92f, 0.12f)
                cubicTo(-0.92f, -0.62f, -0.18f, -0.8f, 0f, -1.02f)
                close()
            }
            drawPath(
                bulb,
                Brush.radialGradient(
                    0f to OnionLight,
                    0.55f to OnionMid,
                    1f to OnionDark,
                    center = Offset(-0.3f, -0.25f),
                    radius = 1.45f,
                ),
            )
            // Skin veins running from the neck to the root.
            for (k in listOf(-0.62f, -0.3f, 0.3f, 0.62f)) {
                val vein = Path().apply {
                    moveTo(0f, -0.9f)
                    quadraticTo(k * 1.55f, 0.05f, k * 0.3f, 0.95f)
                }
                drawPath(vein, OnionVein, style = Stroke(width = 0.035f, cap = StrokeCap.Round))
            }
            // Papery sprout tip and root tuft.
            drawLine(OnionPaper, Offset(0f, -0.98f), Offset(-0.08f, -1.16f), strokeWidth = 0.07f, cap = StrokeCap.Round)
            drawLine(OnionPaper, Offset(0f, -0.98f), Offset(0.07f, -1.13f), strokeWidth = 0.05f, cap = StrokeCap.Round)
            for (dx in listOf(-0.14f, -0.05f, 0.05f, 0.14f)) {
                drawLine(OnionRoot, Offset(dx * 0.4f, 0.96f), Offset(dx, 1.12f), strokeWidth = 0.035f, cap = StrokeCap.Round)
            }
            glint(Offset(-0.42f, -0.32f), 0.38f, alpha = 0.5f)

            // Face (same proportions as faceLayout, unit radius).
            val face = faceLayout(radius = 1f)
            for (cheekX in listOf(-0.58f, 0.58f)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to Cheek.copy(alpha = 0.55f),
                        1f to Color.Transparent,
                        center = Offset(cheekX, 0.18f),
                        radius = 0.22f,
                    ),
                    radius = 0.22f,
                    center = Offset(cheekX, 0.18f),
                )
            }
            for (eye in listOf(Offset(face.leftEyeX, face.leftEyeY), Offset(face.rightEyeX, face.rightEyeY))) {
                drawCircle(FaceColor, radius = face.eyeRadius * 1.15f, center = eye)
                drawCircle(Color.White, radius = face.eyeRadius * 0.4f, center = eye + Offset(-0.04f, -0.05f))
            }
            drawArc(
                color = FaceColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(face.smileCx - face.smileRadius, face.smileCy - face.smileRadius),
                size = Size(face.smileRadius * 2f, face.smileRadius * 2f),
                style = Stroke(width = 0.09f, cap = StrokeCap.Round),
            )
        }
    }
}

// --- palette ---

private val BeltEdge = Color(0xFF1E2024)
private val BeltMid = Color(0xFF2C2F34)
private val BeltCentre = Color(0xFF383C42)
private val RibShadow = Color(0x8C000000)
private val RibLight = Color(0x1FFFFFFF)
private val SteelLight = Color(0xFFEDF0F2)
private val SteelMid = Color(0xFFA9B0B6)
private val SteelDark = Color(0xFF6B7278)
private val SteelShadow = Color(0xFF3E4348)
private val DropTop = Color(0xFF15171A)
private val DropBottom = Color(0xFF07080A)
private val DangerRed = Color(0xFFFF1744)
private val LampCore = Color(0xFFFF5252)

private val AppleLight = Color(0xFFFF7A6B)
private val AppleMid = Color(0xFFD42420)
private val AppleDark = Color(0xFF7E0C0C)
private val AppleBlush = Color(0xFFFFD25A)
private val AppleCavity = Color(0xFF4A0808)
private val StemBrown = Color(0xFF5B3A1E)
private val LeafLight = Color(0xFF8FD14F)
private val LeafDark = Color(0xFF3B8A2A)
private val StemGreen = Color(0xFF5E8C2A)

private val CherryLight = Color(0xFFFF4D63)
private val CherryMid = Color(0xFFB0001E)
private val CherryDark = Color(0xFF45000E)

private val BananaLight = Color(0xFFFFF59A)
private val BananaMid = Color(0xFFF7D02C)
private val BananaDark = Color(0xFFC9920C)
private val BananaTip = Color(0xFF5A3A12)

private val OnionLight = Color(0xFFF0B5E2)
private val OnionMid = Color(0xFFA6418F)
private val OnionDark = Color(0xFF561650)
private val OnionVein = Color(0x2EFFFFFF)
private val OnionPaper = Color(0xFFE2CFA6)
private val OnionRoot = Color(0xFFD9C29A)
private val Cheek = Color(0xFFFF7FA8)
private val FaceColor = Color(0xFF2B0A2A)
