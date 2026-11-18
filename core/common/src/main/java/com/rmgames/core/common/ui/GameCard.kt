package com.rmgames.core.common.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmgames.core.common.theme.RmColors
import com.rmgames.core.common.theme.RmTheme

/** Accent colour for a game id (thumbnail fallback, game chip). Unknown ids get a neutral container. */
fun gameAccent(gameId: String, colors: RmColors): Color = when (gameKey(gameId)) {
    "escape" -> colors.gameEscape
    "onion" -> colors.gameOnion
    "blink" -> colors.gameBlink
    "compass" -> colors.gameCompass
    "soda" -> colors.gameSoda
    else -> colors.surfaceContainerHighest
}

/** Glyph for a game's thumbnail fallback. Unknown ids get the play glyph. */
fun gameGlyph(gameId: String): ImageVector = when (gameKey(gameId)) {
    "escape" -> RmIcons.Run
    "onion" -> RmIcons.Onion
    "blink" -> RmIcons.Eye
    "compass" -> RmIcons.Compass
    "soda" -> RmIcons.Can
    else -> RmIcons.Play
}

/** Ids look like `03-blink-sniper`; the second token names the game. */
private fun gameKey(gameId: String) = gameId.split('-').getOrNull(1).orEmpty()

/**
 * 16:9 game thumbnail. Until a game ships its own art ([art]), shows the generated
 * fallback from Decision 5: game accent + glyph + rings (docs/09 §1).
 */
@Composable
fun GameThumbnail(
    gameId: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
    art: (@Composable () -> Unit)? = null,
) {
    val c = RmTheme.colors
    val shape = MaterialTheme.shapes.large
    Box(
        modifier.fillMaxWidth().aspectRatio(aspectRatio).clip(shape)
            .background(gameAccent(gameId, c))
            .border(RmTheme.dimens.strokeWidth, c.ledge, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) art() else {
            Canvas(Modifier.fillMaxSize()) {
                val ring = c.onGame.copy(alpha = 0.22f)
                drawCircle(ring, radius = 66.dp.toPx(), style = Stroke(2.dp.toPx()))
                drawCircle(ring, radius = 98.dp.toPx(), style = Stroke(2.dp.toPx()))
            }
            Icon(gameGlyph(gameId), contentDescription = null, tint = c.onGame, modifier = Modifier.size(72.dp))
        }
    }
}

/** Small chip in the game's accent colour, e.g. the sensor a game uses. */
@Composable
fun GameChip(gameId: String, label: String, icon: ImageVector?, modifier: Modifier = Modifier) {
    val c = RmTheme.colors
    Row(
        modifier.height(28.dp).clip(CircleShape).background(gameAccent(gameId, c))
            .padding(start = if (icon != null) 8.dp else 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.onGame, modifier = Modifier.size(16.dp))
        Text(label, color = c.onGame, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp))
    }
}

/**
 * Today's game hero (03 §3.1): thumbnail, sensor chip + duration, title, how-to.
 * The Play button and attempts live outside the card, so the card stays reusable.
 */
@Composable
fun GameCard(
    gameId: String,
    title: String,
    howToPlay: String,
    sensorLabel: String,
    sensorIcon: ImageVector?,
    durationLabel: String,
    modifier: Modifier = Modifier,
    thumbnailAspectRatio: Float = 16f / 9f,
    art: (@Composable () -> Unit)? = null,
) {
    val c = RmTheme.colors
    RmCard(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GameThumbnail(gameId, aspectRatio = thumbnailAspectRatio, art = art)
            Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(RmTheme.dimens.gapRelated)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (sensorLabel.isNotEmpty()) GameChip(gameId, sensorLabel, sensorIcon) else Spacer(Modifier)
                    Text(durationLabel.uppercase(), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                }
                Text(title.uppercase(), style = MaterialTheme.typography.headlineLarge.copy(lineHeight = 38.sp), color = c.onSurface)
                Text(howToPlay, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp), color = c.onSurfaceVariant)
            }
        }
    }
}
