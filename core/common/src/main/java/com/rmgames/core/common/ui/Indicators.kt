package com.rmgames.core.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmgames.core.common.theme.RmTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Remaining daily attempts as pips + text (never colour alone). */
@Composable
fun AttemptsPips(remaining: Int, modifier: Modifier = Modifier, total: Int = 2, showLabel: Boolean = true) {
    val c = RmTheme.colors
    val label = attemptsLabel(remaining, total)
    Row(
        modifier.clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pipStates(remaining, total).forEach { available ->
            Box(
                Modifier.size(14.dp).clip(CircleShape)
                    .background(if (available) c.primary else Color.Transparent)
                    .border(2.dp, if (available) c.ledge else c.outline, CircleShape),
            )
        }
        if (showLabel) {
            val r = remaining.coerceIn(0, total)
            Text(
                buildAnnotatedString {
                    if (r == 0) append(label) else {
                        withStyle(SpanStyle(color = c.onSurface, fontWeight = FontWeight.Bold)) { append("$r of $total") }
                        append(" attempts left")
                    }
                },
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = c.onSurfaceVariant,
            )
        }
    }
}

/** Ticks once a second until [targetEpochMs]; returns the time left. */
@Composable
fun rememberRemaining(targetEpochMs: Long, now: () -> Long = System::currentTimeMillis): State<Duration> {
    val state = remember(targetEpochMs) { mutableStateOf((targetEpochMs - now()).milliseconds) }
    LaunchedEffect(targetEpochMs) {
        while (true) {
            val left = targetEpochMs - now()
            state.value = left.milliseconds
            if (left <= 0) break
            delay(1000 - (left % 1000).coerceIn(0, 999))
        }
    }
    return state
}

/** "Ends in 21:14:08" / locked "Next game in 03:42:51" (03 §3.5). */
@Composable
fun CountdownChip(label: String, remaining: Duration, modifier: Modifier = Modifier, locked: Boolean = false) {
    val c = RmTheme.colors
    Row(
        modifier.height(36.dp).clip(CircleShape).background(c.surfaceContainerHighest).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(if (locked) RmIcons.Lock else RmIcons.Timer, contentDescription = null,
            tint = if (locked) c.primaryInk else c.onSurface, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = c.onSurface)
        Text(formatCountdown(remaining), style = RmTheme.score(15.sp), color = c.onSurface)
    }
}

/** Labelled number tile for the result summary and ranking. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    accent: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
) {
    val c = RmTheme.colors
    val fg = if (accent) c.onPrimaryContainer else c.onSurface
    val sub = if (accent) c.onPrimaryContainer else c.onSurfaceVariant
    LedgeSurface(if (accent) c.primaryContainer else c.surfaceContainer, MaterialTheme.shapes.medium, modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = sub)
            Text(value, style = RmTheme.score(26.sp).copy(lineHeight = 32.sp), color = fg)
            if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, color = sub)
            footer?.invoke()
        }
    }
}

/** 7-day season progress with today highlighted, plus the player's season points (null hides them). */
@Composable
fun SeasonStrip(season: Int, todayIndex: Int, points: Long?, modifier: Modifier = Modifier) {
    val c = RmTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = c.onSurface, fontWeight = FontWeight.Bold)) { append("Season $season") }
                append(" · day ${todayIndex + 1} of 7")
            }, style = style, color = c.onSurfaceVariant)
            if (points != null) Text(buildAnnotatedString {
                append("You: ")
                withStyle(SpanStyle(color = c.onSurface, fontWeight = FontWeight.Bold)) { append(formatPoints(points)) }
                append(" pts")
            }, style = style, color = c.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            seasonDays(todayIndex).forEach { day ->
                Box(
                    Modifier.weight(1f).height(8.dp).clip(CircleShape).background(
                        when (day) {
                            SeasonDay.Done -> c.onSurfaceVariant
                            SeasonDay.Today -> c.primary
                            SeasonDay.Upcoming -> c.surfaceContainerHighest
                        },
                    ),
                )
            }
        }
    }
}

/** Overline section label with an optional trailing element. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.3.sp), color = RmTheme.colors.onSurfaceVariant)
        trailing?.invoke()
    }
}
