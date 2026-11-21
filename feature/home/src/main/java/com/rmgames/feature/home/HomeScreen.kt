package com.rmgames.feature.home

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rmgames.core.common.Game
import com.rmgames.core.common.theme.RmTheme
import com.rmgames.core.common.ui.AttemptsPips
import com.rmgames.core.common.ui.AudioScene
import com.rmgames.core.common.ui.LocalUiFeedback
import com.rmgames.core.common.ui.CountdownChip
import com.rmgames.core.common.ui.GameCard
import com.rmgames.core.common.ui.RmButton
import com.rmgames.core.common.ui.RmButtonSize
import com.rmgames.core.common.ui.RmButtonVariant
import com.rmgames.core.common.ui.RmIconButton
import com.rmgames.core.common.ui.RmIcons
import com.rmgames.core.common.ui.RmTopBar
import com.rmgames.core.common.ui.SeasonStrip
import com.rmgames.core.common.ui.SectionHeader
import com.rmgames.core.common.ui.gameDurationLabel
import com.rmgames.core.common.ui.gameScoreText
import com.rmgames.core.common.ui.gameSensor
import com.rmgames.core.common.ui.rememberRemaining
import com.rmgames.core.data.AttemptRepository
import com.rmgames.core.data.DayClock
import com.rmgames.core.data.ScoreRepository
import com.rmgames.core.data.SeasonStatus
import kotlinx.coroutines.delay

/**
 * Main screen route (docs/03 §3.1, §3.5): owns the [HomeViewModel] and refreshes its
 * state on resume and at the 12:00 boundary. `:app` passes the shared repositories
 * and navigation callbacks; null callbacks hide their controls.
 *
 * @param games the daily rotation (only playable games).
 * @param debugTools debug builds only: shows "Reset attempts" and a quick launcher for
 * [debugGames] (every registered game), so each developer can still run their own game any day.
 */
@Composable
fun HomeRoute(
    games: List<Game>,
    attempts: AttemptRepository,
    scores: ScoreRepository,
    clock: DayClock,
    onPlay: (gameId: String) -> Unit,
    debugTools: Boolean,
    debugGames: List<Game> = games,
    thumbnails: Map<String, @Composable () -> Unit> = emptyMap(),
    onHistory: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    /** Why a game can't run on this phone (missing sensor), or null — Play is then disabled. */
    unavailableReason: (gameId: String) -> String? = { null },
    /** Reads my season points + today's standing from the online ranking (null result = unknown); null = offline build. */
    seasonStatus: (suspend () -> SeasonStatus?)? = null,
) {
    val vm = remember(games, attempts, scores, clock) { HomeViewModel(attempts, scores, clock, games, unavailableReason) }
    val feedback = LocalUiFeedback.current
    LaunchedEffect(Unit) { feedback.scene(AudioScene.Menu) }
    var version by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { version++ }
    val state = remember(vm, version) { vm.state() }
    LaunchedEffect(state.nextResetMillis) {
        delay((state.nextResetMillis - System.currentTimeMillis()).coerceAtLeast(0) + 500)
        version++ // new day: new game + fresh attempts
    }
    // Season points arrive after the screen shows; refreshed on every resume / new day.
    // Keyed by season: a failed refresh after the weekly rollover never shows last season's points.
    var status by remember(state.season) { mutableStateOf<SeasonStatus?>(null) }
    LaunchedEffect(version) { status = seasonStatus?.invoke() ?: status }
    HomeScreen(
        // Today's standing only belongs to today's game: a new day (version) drops it until re-read.
        state = state.copy(seasonPoints = status?.points?.toLong(), todayStanding = status?.today.takeIf { state.bestToday != null }),
        thumbnail = thumbnails[state.gameId],
        onPlay = { onPlay(state.gameId) },
        onHistory = onHistory,
        onSettings = onSettings,
        debug = if (debugTools) HomeDebug(
            onResetAttempts = { vm.resetAttempts(); version++ },
            games = debugGames.map { it.id to it.title },
            onPlayGame = onPlay,
        ) else null,
    )
}

/** Debug-only controls (docs/02 "Reset attempts", plan 09 Decision 3). */
class HomeDebug(
    val onResetAttempts: () -> Unit,
    val games: List<Pair<String, String>>,
    val onPlayGame: (gameId: String) -> Unit,
)

/**
 * Stateless Main screen: today's game hero, season strip, attempts, and one big Play.
 * With no attempts left Play is disabled and the countdown points to the next 12:00.
 * The scrollable content sits above a fixed action area so Play is always reachable.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: (@Composable () -> Unit)? = null,
    onHistory: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    debug: HomeDebug? = null,
) {
    val c = RmTheme.colors
    val d = RmTheme.dimens
    val remaining by rememberRemaining(state.nextResetMillis)
    Column(modifier.fillMaxSize().background(c.background)) {
        RmTopBar("RM Games", brand = true, actions = {
            if (onSettings != null) RmIconButton(RmIcons.Settings, "Settings", onSettings)
        })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = d.gutter).padding(bottom = d.gapRelated),
            verticalArrangement = Arrangement.spacedBy(d.gapSection),
        ) {
            if (state.season >= 1) SeasonStrip(state.season, state.seasonDayIndex, state.seasonPoints)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Today's game") {
                    CountdownChip(if (state.locked) "Next game in" else "Ends in", remaining, locked = state.locked)
                }
                val sensor = gameSensor(state.gameId)
                GameCard(
                    gameId = state.gameId,
                    title = state.title,
                    howToPlay = state.howToPlay,
                    sensorLabel = sensor?.label.orEmpty(),
                    sensorIcon = sensor?.icon,
                    durationLabel = gameDurationLabel(state.gameId, state.durationMs),
                    thumbnailAspectRatio = 2f,
                    art = thumbnail,
                )
            }
            if (debug != null) DebugSection(debug)
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = d.gutter).padding(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(d.gapRelated),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                AttemptsPips(state.attemptsLeft, total = state.attemptsTotal)
                val best = state.bestToday?.let { gameScoreText(state.gameId, it) }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Best today: ${best?.let { "${it.value} ${it.unit}" } ?: "—"}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    // Today's position with the season points it earns (online ranking, provisional).
                    state.todayStanding?.let {
                        Text(it.label(), style = MaterialTheme.typography.labelLarge, color = c.primaryInk)
                    }
                }
            }
            val unavailable = state.unavailableReason
            if (unavailable != null) {
                Text(unavailable, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                RmButton("Not playable on this phone", {}, Modifier.fillMaxWidth(), size = RmButtonSize.Xl, icon = RmIcons.Lock, enabled = false)
            } else if (state.locked) {
                RmButton("Back at 12:00", {}, Modifier.fillMaxWidth(), size = RmButtonSize.Xl, icon = RmIcons.Lock, enabled = false)
            } else {
                RmButton("Play", onPlay, Modifier.fillMaxWidth(), size = RmButtonSize.Xl, icon = RmIcons.Play)
            }
            if (onHistory != null) {
                RmButton("Seasons history", onHistory, Modifier.fillMaxWidth(), variant = RmButtonVariant.Tonal, icon = RmIcons.Podium)
            }
        }
    }
}

@Composable
private fun DebugSection(debug: HomeDebug) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Debug")
        RmButton("Reset attempts (debug)", debug.onResetAttempts, variant = RmButtonVariant.Outlined, icon = RmIcons.Bug)
        debug.games.forEach { (id, title) ->
            RmButton("Play $title (debug)", { debug.onPlayGame(id) }, variant = RmButtonVariant.Text, icon = RmIcons.Play)
        }
    }
}
