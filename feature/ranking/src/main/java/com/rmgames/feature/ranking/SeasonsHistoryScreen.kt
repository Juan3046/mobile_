package com.rmgames.feature.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rmgames.core.common.Game
import com.rmgames.core.common.theme.RmTheme
import com.rmgames.core.common.ui.AudioScene
import com.rmgames.core.common.ui.LocalUiFeedback
import com.rmgames.core.common.ui.RmCard
import com.rmgames.core.common.ui.RmIcons
import com.rmgames.core.common.ui.RmListDivider
import com.rmgames.core.common.ui.RmListItem
import com.rmgames.core.common.ui.RmTabs
import com.rmgames.core.common.ui.RmTopBar
import com.rmgames.core.common.ui.SeasonStrip
import com.rmgames.core.common.ui.SectionHeader
import com.rmgames.core.common.ui.StatTile
import com.rmgames.core.common.ui.gameScoreText
import com.rmgames.core.data.DayClock
import com.rmgames.core.data.ScoreRepository

/**
 * Seasons history route (docs/03 §3.4), built from the player's own daily bests. Opened
 * from the Main screen's "Seasons history" button.
 */
@Composable
fun SeasonsHistoryRoute(
    scores: ScoreRepository,
    clock: DayClock,
    games: List<Game>,
    onBack: () -> Unit,
    dailyGameId: (Long) -> String? = { null },
) {
    val feedback = LocalUiFeedback.current
    LaunchedEffect(Unit) { feedback.scene(AudioScene.Menu) }
    val history = remember(scores, clock, games) {
        SeasonHistory.build(clock.dayIndex(), scores.history(), games.associate { it.id to it.title }, dailyGameId)
    }
    SeasonsHistoryScreen(history, onBack)
}

@Composable
fun SeasonsHistoryScreen(history: SeasonHistory, onBack: () -> Unit) {
    val c = RmTheme.colors
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(c.background)) {
        RmTopBar("Seasons history", navigationIcon = RmIcons.Back, onNavigate = onBack)
        RmTabs(listOf("Season", "History"), tab, { tab = it })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = RmTheme.dimens.gutter).padding(top = 16.dp, bottom = RmTheme.dimens.gapMajor),
            verticalArrangement = Arrangement.spacedBy(RmTheme.dimens.gapSection),
        ) {
            if (tab == 0) SeasonTab(history.current) else HistoryTab(history)
        }
    }
}

@Composable
private fun SeasonTab(s: SeasonSummary) {
    val c = RmTheme.colors
    val todayIndex = s.days.indexOfFirst { it.state == DayState.Today }.coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SEASON ${s.season}", style = MaterialTheme.typography.headlineMedium, color = c.onSurface)
        Text("${s.dateRange} · days run noon to noon", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        SeasonStrip(s.season, todayIndex, points = null, showLabels = false)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Days played", "${s.daysPlayed} / ${s.days.size}", Modifier.weight(1f))
        StatTile("Season points", "—", Modifier.weight(1f), caption = "with online ranking")
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Day by day")
        RmCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                s.days.forEachIndexed { i, d ->
                    if (i > 0) RmListDivider(inset = false)
                    RmListItem(headline = d.dateLabel, supporting = dayText(d))
                }
            }
        }
    }
    OnlineNote()
}

private fun dayText(d: SeasonDayRow): String {
    val score = if (d.gameId != null && d.best != null) gameScoreText(d.gameId, d.best).let { "${it.value} ${it.unit}" } else null
    return when (d.state) {
        DayState.Played -> "${d.gameTitle} · best ${score}"
        DayState.Missed -> "Not played"
        DayState.Today -> if (score != null) "Today · ${d.gameTitle} · best $score" else "Today · not played yet"
        DayState.Upcoming -> "Upcoming"
    }
}

@Composable
private fun HistoryTab(h: SeasonHistory) {
    val c = RmTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Days played", "${h.allTime.daysPlayed}", Modifier.weight(1f), caption = "all time")
        StatTile("Seasons", "${h.allTime.seasonsPlayed}", Modifier.weight(1f), caption = "with at least one day")
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Past seasons")
        if (h.past.isEmpty()) {
            Text(
                "Your finished seasons will show here. Season ${h.current.season} ends after ${h.current.dateRange.substringAfter("– ")}.",
                style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
            )
        } else {
            RmCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    h.past.forEachIndexed { i, p ->
                        if (i > 0) RmListDivider(inset = false)
                        RmListItem("Season ${p.season} · ${p.dateRange}", supporting = "${p.daysPlayed} of 7 days played", icon = RmIcons.Calendar)
                    }
                }
            }
        }
    }
    OnlineNote()
}

@Composable
private fun OnlineNote() {
    Text(
        "Positions, season points and other players' standings arrive with online ranking. For now this shows your own results on this phone.",
        style = MaterialTheme.typography.bodySmall, color = RmTheme.colors.onSurfaceVariant,
    )
}
