package com.rmgames.core.common.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmgames.core.common.theme.RmFonts
import com.rmgames.core.common.theme.RmTheme
import com.rmgames.core.common.theme.rmNumberStyle
import androidx.compose.ui.text.rememberTextMeasurer

/**
 * Hero result number (03 §3.3). Counts up from 0 over `RmMotion.durationCountUp`, then
 * pops with the spring easing. [isBest] tints it with the primary ink.
 */
@Composable
fun ScoreCounter(
    score: Long,
    unit: String,
    modifier: Modifier = Modifier,
    isBest: Boolean = false,
    animate: Boolean = true,
    format: (Long) -> String = { it.toString() },
) {
    val c = RmTheme.colors
    val m = RmTheme.motion
    val progress = remember(score) { Animatable(if (animate) 0f else 1f) }
    val pop = remember(score) { Animatable(1f) }
    LaunchedEffect(score, animate) {
        if (!animate) return@LaunchedEffect
        progress.animateTo(1f, tween(m.durationCountUp, easing = m.standard))
        pop.snapTo(0.92f)
        pop.animateTo(1f, tween(m.durationBase, easing = m.spring))
    }
    val maxSize = 120.sp
    val measurer = rememberTextMeasurer()
    Column(
        modifier.clearAndSetSemantics { contentDescription = "${format(score)} $unit" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            // Size for the final value (the widest the count-up gets) so the digits never
            // wrap and the size doesn't jump while counting.
            val fitted = remember(score, constraints.maxWidth) {
                val width = measurer.measure(format(score), rmNumberStyle(RmFonts.Display, maxSize), maxLines = 1, softWrap = false).size.width
                fitScoreFontSize(maxSize.value, width.toFloat(), constraints.maxWidth.toFloat()).sp
            }
            Text(
                format(countUpValue(score, progress.value)),
                style = RmTheme.score(fitted),
                color = if (isBest) c.primaryInk else c.onSurface,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.digitBox(fitted).scale(pop.value),
            )
        }
        Text(unit.uppercase(), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
    }
}

/** The tilted "New best" stamp — the only place the celebration colour appears. */
@Composable
fun NewBestStamp(modifier: Modifier = Modifier, text: String = "New best") {
    val c = RmTheme.colors
    LedgeSurface(c.tertiary, CircleShape, modifier.rotate(-3f)) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(RmIcons.Star, contentDescription = null, tint = c.onTertiary, modifier = Modifier.size(18.dp))
            Text(text.uppercase(), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.9.sp), color = c.onTertiary)
        }
    }
}

/**
 * Celebration burst for a new best: pieces fall and fade over ~1.3s. Decorative only.
 * Deterministic per [seed] so recomposition doesn't reshuffle it.
 */
@Composable
fun Confetti(modifier: Modifier = Modifier, seed: Int = 1, count: Int = 36, play: Boolean = true) {
    val c = RmTheme.colors
    val slots = listOf(c.tertiary, c.primary, c.secondary)
    val pieces = remember(seed, count) { confettiPieces(count, seed) }
    val duration = RmTheme.motion.durationSlow * 3
    val t = remember(seed) { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(seed, play) { if (play) t.animateTo(1f, tween(duration, easing = LinearEasing)) }
    Canvas(modifier.clearAndSetSemantics { }) {
        val p = t.value
        if (p >= 1f) return@Canvas
        val unit = 8.dp.toPx()
        pieces.forEach { piece ->
            val x = (piece.x + piece.drift * 0.08f * p) * size.width
            val y = (piece.y * 0.45f + p * 0.75f) * size.height
            val s = unit * piece.size
            val color = slots[piece.colorSlot].copy(alpha = 1f - p)
            if (piece.isCircle) drawCircle(color, s / 2, Offset(x, y))
            else rotate(piece.rotation + p * 540f * piece.drift, Offset(x, y)) {
                drawRect(color, Offset(x - s / 2, y - s / 2), Size(s, s * 0.6f))
            }
        }
    }
}

/**
 * Bungee's line box is ~1.3× the font size (tall accents), so a huge score sits in a lot of
 * empty space. Lays the text out at [DIGIT_BOX] × size and centres the glyphs in it —
 * digits have no descenders and Bungee centres them in its line box (measured on device).
 */
internal fun Modifier.digitBox(size: TextUnit): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val height = (size * DIGIT_BOX).roundToPx()
    layout(placeable.width, height) { placeable.place(0, (height - placeable.height) / 2) }
}

/** Visual height of Bungee digits relative to the font size (cap height + a little air). */
internal const val DIGIT_BOX = 0.82f
