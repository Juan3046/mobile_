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
import androidx.compose.runtime.mutableStateOf
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
import com.rmgames.core.data.RankRow
import com.rmgames.core.data.RankingSync
import com.rmgames.core.data.ScoreRepository
import com.rmgames.core.data.Standing

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
    /** Online ranking for points, positions and today's top 50; null = none. */
    ranking: RankingSync? = null,
) {
    val feedback = LocalUiFeedback.current
    LaunchedEffect(Unit) { feedback.scene(AudioScene.Menu) }
    val rules = remember(games) { games.associate { it.id to it.higherWins } }
    val today = remember(clock) { clock.dayIndex() }
    val history = remember(scores, clock, games) {
        SeasonHistory.build(today, scores.history(), games.associate { it.id to it.title }, dailyGameId) { rules[it] ?: true }
    }
    val todaysGame = dailyGameId(today)
    var online by remember {
        mutableStateOf(OnlineStandings(online = ranking?.isOnline == true, todayGameId = todaysGame, todayGameTitle = games.firstOrNull { it.id == todaysGame }?.title))
    }
    LaunchedEffect(history) {
        if (ranking == null || !ranking.isOnline) return@LaunchedEffect
        // Today's best is (re)uploaded first, so it shows in the top list below.
        val standings = ranking.season(history.current.playedDays { rules[it] ?: true }, today)
        online = online.copy(standings = standings)
        if (todaysGame != null) online = online.copy(todayTop = ranking.top(today, todaysGame, rules[todaysGame] ?: true))
    }
    SeasonsHistoryScreen(history, onBack, online)
}

/** What the online ranking adds to the history; nulls are "not loaded (yet)". */
data class OnlineStandings(
    val online: Boolean = false,
    val standings: Map<Long, Standing?>? = null,
    val todayTop: List<RankRow>? = null,
    val todayGameId: String? = null,
    val todayGameTitle: String? = null,
)

@Composable
fun SeasonsHistoryScreen(history: SeasonHistory, onBack: () -> Unit, online: OnlineStandings = OnlineStandings()) {
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
            if (tab == 0) SeasonTab(history.current, online) else HistoryTab(history, online.online)
        }
    }
}

@Composable
private fun SeasonTab(s: SeasonSummary, online: OnlineStandings) {
    val c = RmTheme.colors
    val todayIndex = s.days.indexOfFirst { it.state == DayState.Today }.coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SEASON ${s.season}", style = MaterialTheme.typography.headlineMedium, color = c.onSurface)
        Text("${s.dateRange} · days run noon to noon", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        SeasonStrip(s.season, todayIndex, points = null, showLabels = false)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Days played", "${s.daysPlayed} / ${s.days.size}", Modifier.weight(1f))
        val (points, caption) = seasonPointsTile(online.online, online.standings)
        StatTile("Season points", points, Modifier.weight(1f), caption = caption)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Day by day")
        RmCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                s.days.forEachIndexed { i, d ->
                    if (i > 0) RmListDivider(inset = false)
                    val standing = online.standings?.get(d.dayIndex)?.let { " · ${standingLabel(it)}" }.orEmpty()
                    RmListItem(headline = d.dateLabel, supporting = dayText(d) + standing)
                }
            }
        }
    }
    if (online.online && online.todayGameId != null) TodayTop(online.todayGameId, online.todayGameTitle ?: online.todayGameId, online.todayTop)
    OnlineNote(online.online)
}

/** Today's top 50 for the daily game (provisional until 12:00). */
@Composable
private fun TodayTop(gameId: String, gameTitle: String, rows: List<RankRow>?) {
    val c = RmTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Today · $gameTitle")
        when {
            rows == null -> Text("Loading today's ranking…", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            rows.isEmpty() -> Text("No one is ranked yet today (or no connection).", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            else -> RmCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    rows.forEachIndexed { i, r ->
                        if (i > 0) RmListDivider(inset = false)
                        val score = gameScoreText(gameId, r.best)
                        RmListItem(
                            headline = "#${r.position}  ${r.nick}" + if (r.isMe) " (you)" else "",
                            supporting = "${score.value} ${score.unit}",
                            icon = if (r.isMe) RmIcons.Trophy else null,
                        )
                    }
                }
            }
        }
    }
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
private fun HistoryTab(h: SeasonHistory, online: Boolean) {
    val c = RmTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Days played", "${h.allTime.daysPlayed}", Modifier.weight(1f), caption = "all time")
        StatTile("Seasons", "${h.allTime.seasonsPlayed}", Modifier.weight(1f), caption = "with at least one day")
    }
    if (h.personalBests.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader("Personal bests")
            RmCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    h.personalBests.forEachIndexed { i, pb ->
                        if (i > 0) RmListDivider(inset = false)
                        val score = gameScoreText(pb.gameId, pb.best)
                        RmListItem(pb.gameTitle, supporting = "${score.value} ${score.unit} · ${pb.dateLabel}", icon = RmIcons.Trophy)
                    }
                }
            }
        }
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
    OnlineNote(online)
}

@Composable
private fun OnlineNote(online: Boolean) {
    Text(
        if (online) "Positions and points are provisional until each day closes at 12:00. Your bests upload when you're online."
        else "This build has no online ranking: it shows your own results on this phone.",
        style = MaterialTheme.typography.bodySmall, color = RmTheme.colors.onSurfaceVariant,
    )
}
