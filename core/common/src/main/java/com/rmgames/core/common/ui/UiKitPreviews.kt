package com.rmgames.core.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmgames.core.common.theme.RmTheme
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Static previews mirroring design-system/concept.html, built only from the UI kit.
// Illustrative content (Blink Sniper as today's game). Not used at runtime.

@Composable
private fun MainSample() {
    val c = RmTheme.colors
    Column(Modifier.fillMaxSize().background(c.background)) {
        RmTopBar("RM Games", brand = true, actions = {
            RmIconButton(RmIcons.Settings, "Settings", onClick = {})
        })
        Column(
            Modifier.weight(1f).padding(horizontal = RmTheme.dimens.gutter).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(RmTheme.dimens.gapSection),
        ) {
            SeasonStrip(season = 12, todayIndex = 3, points = 3150)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Today's game") {
                    CountdownChip("Ends in", 21.hours + 14.minutes + 8.seconds)
                }
                GameCard(
                    gameId = "03-blink-sniper", title = "Blink Sniper",
                    howToPlay = "Targets slide through the crosshair. Blink to fire — bullseye scores +2. Never shoot a bomb.",
                    sensorLabel = "Front camera", sensorIcon = RmIcons.Camera, durationLabel = "60 seconds",
                )
            }
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    AttemptsPips(remaining = 2)
                    Text("Best today: —", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                RmButton("Play", {}, Modifier.fillMaxWidth(), size = RmButtonSize.Xl, icon = RmIcons.Play)
                RmButton("Seasons history", {}, Modifier.fillMaxWidth(), variant = RmButtonVariant.Tonal, icon = RmIcons.Podium)
                RmButton("Reset attempts (debug)", {}, Modifier.align(Alignment.CenterHorizontally),
                    variant = RmButtonVariant.Text, icon = RmIcons.Bug)
            }
        }
    }
}

@Composable
private fun ResultSample() {
    val c = RmTheme.colors
    Column(Modifier.fillMaxSize().background(c.background)) {
        RmTopBar("Blink Sniper · run 1 of 2", navigationIcon = RmIcons.Close, navigationLabel = "Close")
        Column(
            Modifier.weight(1f).padding(horizontal = RmTheme.dimens.gutter).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Confetti(Modifier.fillMaxWidth().height(140.dp), play = false)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NewBestStamp()
                    ScoreCounter(27, "points", isBest = true, animate = false)
                    Text("21 hits · 3 bullseyes · ended on time", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Daily best", "27", Modifier.weight(1f), caption = "this run")
                StatTile("Attempts", "1 / 2", Modifier.weight(1f)) { AttemptsPips(1, showLabel = false) }
            }
            StatTile("Provisional · #14 of 212", formatPoints(4975, signed = true), caption = "Season points if the day ended now · final at 12:00", accent = true)
            Spacer(Modifier.weight(1f))
            RmButton("Play again", {}, Modifier.fillMaxWidth(), size = RmButtonSize.Xl, icon = RmIcons.Replay)
            RmButton("Back to home", {}, Modifier.fillMaxWidth(), variant = RmButtonVariant.Text)
        }
    }
}

@Preview(name = "Main · dark", widthDp = 360, heightDp = 800)
@Composable
private fun MainDarkPreview() = RmTheme(darkTheme = true) { MainSample() }

@Preview(name = "Main · light", widthDp = 360, heightDp = 800)
@Composable
private fun MainLightPreview() = RmTheme(darkTheme = false) { MainSample() }

@Preview(name = "Result · dark", widthDp = 360, heightDp = 800)
@Composable
private fun ResultDarkPreview() = RmTheme(darkTheme = true) { ResultSample() }

@Preview(name = "Result · light", widthDp = 360, heightDp = 800)
@Composable
private fun ResultLightPreview() = RmTheme(darkTheme = false) { ResultSample() }

@Preview(name = "Buttons", widthDp = 360)
@Composable
private fun ButtonsPreview() = RmTheme(darkTheme = true) {
    Column(Modifier.background(RmTheme.colors.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RmButton("Filled", {})
        RmButton("Tonal", {}, variant = RmButtonVariant.Tonal)
        RmButton("Outlined", {}, variant = RmButtonVariant.Outlined)
        RmButton("Text", {}, variant = RmButtonVariant.Text)
        RmButton("Next game 12:00", {}, Modifier.fillMaxWidth(), size = RmButtonSize.Xl, icon = RmIcons.Lock, enabled = false)
    }
}
