package com.rmgames.feature.session

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.theme.RmTheme
import com.rmgames.core.common.ui.AttemptsPips
import com.rmgames.core.common.ui.AudioScene
import com.rmgames.core.common.ui.UiCue
import com.rmgames.core.common.ui.Confetti
import com.rmgames.core.common.ui.CountdownChip
import com.rmgames.core.common.ui.GameCard
import com.rmgames.core.common.ui.LocalUiFeedback
import com.rmgames.core.common.ui.NewBestStamp
import com.rmgames.core.common.ui.RmButton
import com.rmgames.core.common.ui.RmButtonSize
import com.rmgames.core.common.ui.RmButtonVariant
import com.rmgames.core.common.ui.RmCard
import com.rmgames.core.common.ui.RmIcons
import com.rmgames.core.common.ui.RmTopBar
import com.rmgames.core.common.ui.ScoreCounter
import com.rmgames.core.common.ui.StatTile
import com.rmgames.core.common.ui.gameDurationLabel
import com.rmgames.core.common.ui.gameHowTo
import com.rmgames.core.common.ui.HowTo
import com.rmgames.core.common.ui.gameScoreText
import com.rmgames.core.common.ui.gameSensor
import com.rmgames.core.common.ui.rememberRemaining
import com.rmgames.core.data.AttemptRepository
import com.rmgames.core.data.DayClock
import com.rmgames.core.data.ScoreRepository
import kotlinx.coroutines.delay

/** How long the game's own end state stays on screen before the Result screen. */
private const val END_HOLD_MS = 1_000L

/**
 * Session host (plan 09 PR 9): how-to intro → 3-2-1 → the game → Result, with
 * "Play again" looping back to the countdown while attempts remain (docs/03 §3.3).
 * The run itself still goes through [SessionGameScope], so the frozen game contract
 * is unchanged. Leaving during the countdown or the run asks first, then forfeits
 * the attempt (docs/02 "No pause").
 *
 * @param entry resolved from the `GameRegistry` by `:app`; null = unknown game id.
 * @param onExit back to the Main screen.
 */
@Composable
fun SessionRoute(
    entry: GameEntry?,
    attempts: AttemptRepository,
    scores: ScoreRepository,
    clock: DayClock,
    onExit: () -> Unit,
    practice: Boolean = false,
) {
    if (entry == null) {
        NotFound(onExit)
        return
    }
    val flow = remember(entry) { SessionFlow(entry.game, attempts, scores, clock, practice = practice) }
    var run by remember(entry) { mutableStateOf<SessionGameScope?>(null) }
    var showLeave by remember { mutableStateOf(false) }
    val phase = flow.phase
    val view = LocalView.current
    val feedback = LocalUiFeedback.current

    fun startRun() {
        if (!flow.start()) return
        run?.close()
        run = SessionGameScope(entry.game, reportScore = flow::onScore, reportFinished = flow::onFinished)
    }

    LaunchedEffect(phase) {
        when (phase) {
            SessionPhase.Intro, SessionPhase.Locked -> feedback.scene(AudioScene.Menu)
            is SessionPhase.Countdown -> {
                feedback.scene(AudioScene.Game)
                feedback.cue(UiCue.Tick)
                if (feedback.hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                delay(1_000)
                flow.tick()
            }
            SessionPhase.Playing -> {
                feedback.cue(UiCue.Go)
                if (feedback.hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                run?.startRun()
            }
            SessionPhase.Abandoned -> { feedback.scene(AudioScene.Menu); onExit() }
            else -> Unit
        }
    }
    DisposableEffect(entry) {
        onDispose {
            flow.abandon()
            run?.close()
        }
    }
    BackHandler(enabled = phase is SessionPhase.Countdown || phase == SessionPhase.Playing) { showLeave = true }

    when (phase) {
        SessionPhase.Intro, SessionPhase.Locked ->
            IntroScreen(entry.game, entry.thumbnail, flow.attemptsLeft(), locked = phase == SessionPhase.Locked, practice = practice, onStart = ::startRun, onClose = onExit)
        is SessionPhase.Finished -> {
            var showResult by remember(phase) { mutableStateOf(false) }
            LaunchedEffect(phase) {
                delay(END_HOLD_MS)
                showResult = true
                feedback.scene(AudioScene.Menu)
                if (phase.summary.isNewBest || phase.summary.isPersonalBest) feedback.cue(UiCue.NewBest)
            }
            if (showResult) ResultScreen(entry.game, phase.summary, onPlayAgain = ::startRun, onHome = onExit)
            else RunStage(entry, phase, flow.liveScore)
        }
        else -> RunStage(entry, phase, flow.liveScore)
    }

    if (showLeave) {
        AlertDialog(
            onDismissRequest = { showLeave = false },
            title = { Text("Leave this run?") },
            text = { Text("Runs can't be paused. If you leave now, this attempt is used and nothing is recorded.") },
            confirmButton = { RmButton("Leave", { showLeave = false; flow.abandon() }, variant = RmButtonVariant.Text) },
            dismissButton = { RmButton("Keep playing", { showLeave = false }, variant = RmButtonVariant.Text) },
        )
    }
}

/** Whether the host draws its own score chip over [entry]'s screen. */
internal fun hostHudVisible(entry: GameEntry): Boolean = !entry.drawsOwnHud

@Composable
private fun NotFound(onExit: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Game not found", style = MaterialTheme.typography.titleLarge, color = RmTheme.colors.onSurface)
        RmButton("Back to home", onExit, variant = RmButtonVariant.Text)
    }
}

/** How-to + Start (plan 09 §2D.2). Start spends the attempt. */
@Composable
private fun IntroScreen(game: Game, thumbnail: (@Composable () -> Unit)?, attemptsLeft: Int, locked: Boolean, practice: Boolean, onStart: () -> Unit, onClose: () -> Unit) {
    val c = RmTheme.colors
    val d = RmTheme.dimens
    Column(Modifier.fillMaxSize().background(c.background)) {
        RmTopBar("How to play", navigationIcon = RmIcons.Close, navigationLabel = "Close", onNavigate = onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = d.gutter)) {
            val sensor = gameSensor(game.id)
            GameCard(
                gameId = game.id, title = game.title, howToPlay = game.description,
                sensorLabel = sensor?.label.orEmpty(), sensorIcon = sensor?.icon,
                durationLabel = gameDurationLabel(game.id, game.durationMs),
                art = thumbnail,
            )
            gameHowTo(game.id)?.let { HowToCard(it, Modifier.padding(top = d.gapSection)) }
        }
        Column(Modifier.fillMaxWidth().padding(d.gutter), verticalArrangement = Arrangement.spacedBy(d.gapRelated)) {
            AttemptsPips(attemptsLeft)
            Text(
                if (locked) "No attempts left today. A new game starts at 12:00."
                else if (practice) "Practice run (debug): this is not today's game, so no attempt is used and nothing is recorded."
                else "Starting uses 1 attempt. Runs can't be paused.",
                style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
            )
            if (locked) RmButton("Back at 12:00", {}, Modifier.fillMaxWidth(), size = RmButtonSize.Xl, icon = RmIcons.Lock, enabled = false)
            else RmButton("Start", onStart, Modifier.fillMaxWidth(), size = RmButtonSize.Xl, icon = RmIcons.Play)
        }
    }
}

/** The game itself, with the 3-2-1 overlay, the host score chip and the end hold. */
@Composable
private fun RunStage(entry: GameEntry, phase: SessionPhase, liveScore: Long?) {
    val c = RmTheme.colors
    Box(Modifier.fillMaxSize().background(c.background)) {
        entry.screen()
        if (phase == SessionPhase.Playing && hostHudVisible(entry) && liveScore != null) {
            val s = gameScoreText(entry.game.id, liveScore)
            Text(
                "${s.value} ${s.unit}",
                Modifier.align(Alignment.TopCenter).padding(top = 16.dp).clip(CircleShape)
                    .background(c.inverseSurface).padding(horizontal = 16.dp, vertical = 8.dp),
                style = RmTheme.numbers(16.sp), color = c.inverseOnSurface,
            )
        }
        when (phase) {
            is SessionPhase.Countdown -> CountdownOverlay(phase.secondsLeft)
            is SessionPhase.Finished -> Box(Modifier.fillMaxSize().background(c.scrim), contentAlignment = Alignment.Center) {
                Text("RUN OVER", style = MaterialTheme.typography.displaySmall, color = c.inverseOnSurface)
            }
            else -> Unit
        }
    }
}

@Composable
private fun CountdownOverlay(secondsLeft: Int) {
    val c = RmTheme.colors
    val m = RmTheme.motion
    val pop = remember(secondsLeft) { Animatable(1.5f) }
    LaunchedEffect(secondsLeft) { pop.animateTo(1f, tween(m.durationBase, easing = m.spring)) }
    Column(
        Modifier.fillMaxSize().background(c.scrim),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("GET READY", style = MaterialTheme.typography.headlineSmall, color = c.inverseOnSurface)
        Text(secondsLeft.toString(), Modifier.scale(pop.value), style = RmTheme.score(160.sp), color = c.tertiary)
    }
}

/** Numbered steps + the goal, under the GameCard on the intro (docs/03 §3.3). */
@Composable
private fun HowToCard(howTo: HowTo, modifier: Modifier = Modifier) {
    val c = RmTheme.colors
    RmCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("HOW TO PLAY", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
            howTo.steps.forEachIndexed { i, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = c.primary)
                    Text(step, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                }
            }
            Text("Goal: ${howTo.goal}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = c.onSurface)
        }
    }
}

/** Result (docs/03 §3.3): score, daily best, attempts, ranking note, Play again or lock. */
@Composable
private fun ResultScreen(game: Game, s: RunSummary, onPlayAgain: () -> Unit, onHome: () -> Unit) {
    val c = RmTheme.colors
    val d = RmTheme.dimens
    val runNumber = (s.attemptsTotal - s.attemptsLeft).coerceAtLeast(1)
    val locked = !s.practice && s.attemptsLeft <= 0
    val remaining by rememberRemaining(s.nextResetMillis)
    val unit = gameScoreText(game.id, s.score ?: 0).unit
    Column(Modifier.fillMaxSize().background(c.background)) {
        RmTopBar(if (s.practice) "${game.title} · practice" else "${game.title} · run $runNumber of ${s.attemptsTotal}", navigationIcon = RmIcons.Close, navigationLabel = "Back to home", onNavigate = onHome)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = d.gutter),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                if (s.isNewBest || s.isPersonalBest) Confetti(Modifier.fillMaxWidth().height(160.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (s.isPersonalBest) NewBestStamp(text = "Personal best") else if (s.isNewBest) NewBestStamp()
                    if (s.score != null) {
                        ScoreCounter(s.score, unit, isBest = s.isNewBest, format = { gameScoreText(game.id, it).value })
                    } else {
                        Text("—", style = RmTheme.score(120.sp), color = c.onSurfaceVariant)
                        Text("No score this run", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant)
                    }
                    if (!s.isNewBest && s.dailyBest != null && s.score != null) {
                        Text("Your best today stays at ${gameScoreText(game.id, s.dailyBest).value}",
                            style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    if (!s.isPersonalBest && s.personalBest != null && s.score != null) {
                        val pb = gameScoreText(game.id, s.personalBest)
                        Text("Personal best: ${pb.value} ${pb.unit}",
                            style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val best = s.dailyBest?.let { gameScoreText(game.id, it) }
                StatTile("Daily best", best?.value ?: "—", Modifier.weight(1f), caption = best?.unit ?: "no score yet")
                StatTile("Attempts", "${s.attemptsLeft} / ${s.attemptsTotal}", Modifier.weight(1f)) {
                    AttemptsPips(s.attemptsLeft, total = s.attemptsTotal, showLabel = false)
                }
            }
            RmCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (s.practice) "PRACTICE RUN" else "PROVISIONAL RANKING", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    Text(if (s.practice) "Debug practice: this score is not saved and no attempt was used."
                        else "Your best of the day decides your position. Position and season points show here once online ranking arrives.",
                        style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(d.gutter), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (locked) {
                CountdownChip("Next game in", remaining, Modifier.align(Alignment.CenterHorizontally), locked = true)
                RmButton("Back at 12:00", {}, Modifier.fillMaxWidth(), size = RmButtonSize.Xl, icon = RmIcons.Lock, enabled = false)
            } else {
                RmButton("Play again", onPlayAgain, Modifier.fillMaxWidth(), size = RmButtonSize.Xl, icon = RmIcons.Replay)
            }
            RmButton("Back to home", onHome, Modifier.fillMaxWidth(), variant = RmButtonVariant.Text)
        }
    }
}
