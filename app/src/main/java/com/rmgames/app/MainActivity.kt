package com.rmgames.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rmgames.core.common.GameRegistry
import com.rmgames.core.common.theme.RmTheme
import com.rmgames.core.common.ui.RmTransitions
import android.content.pm.ApplicationInfo
import com.rmgames.core.sensors.AndroidAccelerometerSensor
import com.rmgames.core.sensors.AndroidCompassSensor
import com.rmgames.core.sensors.AndroidGyroscopeSensor
import com.rmgames.core.vision.MlKitBlinkDetector
import com.rmgames.feature.game.blink.blinkSniperEntry
import com.rmgames.feature.game.compass.compassChaosEntry
import com.rmgames.feature.game.escape.escapePorubaEntry
import com.rmgames.feature.game.onion.onionRollEntry
import com.rmgames.feature.game.soda.sodaShakeEntry
import com.rmgames.feature.home.HomeRoute
import com.rmgames.feature.session.SessionRoute
import com.rmgames.feature.settings.SettingsRoute
import com.rmgames.feature.ranking.SeasonsHistoryRoute
import com.rmgames.feature.onboarding.NickOnboardingRoute
import com.rmgames.core.data.DailyGameSelector
import com.rmgames.core.common.ui.LocalUiFeedback
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context

/** App shell: loads the persisted state, then the registry-driven nav (Main → session → Result). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val container = (application as RmApplication).container
        // Splash (plan 09 PR 14): must be installed before super.onCreate; stays until the
        // saved state is in memory. Loading starts here, not only from the composition.
        installSplashScreen().setKeepOnScreenCondition { AppChrome.shouldKeepSplash(container.isLoaded) }
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { container.load() }
        enableEdgeToEdge(systemBarStyle(), systemBarStyle())
        // Music pauses in the background and resumes in the foreground (plan 09 §2B).
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> container.audio.onForeground(true)
                Lifecycle.Event.ON_STOP -> container.audio.onForeground(false)
                else -> Unit
            }
        })
        setContent {
            // Direction A "Chunky Cabinet" (docs/09 §2A). Surface paints the themed background.
            RmTheme {
                // Edge-to-edge: screens stay inside the safe area; behind the bars the
                // window background and bar scrims show the same RmTheme background colour.
                Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
                    // Saved attempts/scores/settings load from disk (a few ms) before the first screen.
                    var ready by remember { mutableStateOf(container.isLoaded) }
                    LaunchedEffect(container) { container.load(); ready = true }
                    if (ready) {
                        val settings by container.settings.settings.collectAsStateWithLifecycle()
                        LaunchedEffect(settings) { container.audio.onSettings(settings.musicVolume, settings.sfxEnabled) }
                        val feedback = remember(settings) {
                            uiFeedbackFor(settings, play = container.audio::play, scene = container.audio::setScene)
                        }
                        CompositionLocalProvider(LocalUiFeedback provides feedback) {
                            RMGamesNav(container)
                        }
                    }
                }
            }
        }
    }

    /** Bars painted with the RmTheme background; icons follow the light/dark theme. */
    private fun systemBarStyle(): SystemBarStyle {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (AppChrome.lightBarIcons(darkTheme = night == Configuration.UI_MODE_NIGHT_YES)) {
            SystemBarStyle.dark(AppChrome.DARK_BAR_SCRIM)
        } else {
            SystemBarStyle.light(AppChrome.LIGHT_BAR_SCRIM, AppChrome.DARK_BAR_SCRIM)
        }
    }
}

/**
 * Composition root: the single place the five games are wired into the
 * [GameRegistry]. Game authors never edit this file — each module owns its frozen
 * `<game>Entry(...)` factory (docs/08-implementation-plan.md — Game Hosting Contract).
 */
@Composable
private fun rememberGameRegistry(): GameRegistry {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember(context, lifecycleOwner) {
        GameRegistry(
            listOf(
                escapePorubaEntry(),
                onionRollEntry(AndroidGyroscopeSensor(context)),
                blinkSniperEntry(MlKitBlinkDetector(context, lifecycleOwner)),
                compassChaosEntry(AndroidCompassSensor(context)),
                sodaShakeEntry(AndroidAccelerometerSensor(context)),
            ),
        )
    }
}

@Composable
fun RMGamesNav(container: AppContainer) {
    val nav = rememberNavController()
    val registry = rememberGameRegistry()
    // App-wide, persisted repositories (AppContainer chooses where they are stored).
    val attempts = container.attempts
    val scores = container.scores
    val clock = container.clock
    val context = LocalContext.current
    val todaysGame: (Long) -> String = { day -> DailyGameSelector.select(day, dailyRotation(registry.all.map { it.game }).map { it.id }) }
    val debuggable = remember { (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    NavHost(
        navController = nav,
        // First launch: choose a nick before anything else (docs/03 §3.2).
        startDestination = remember { if (container.players.nick.value == null) "onboarding" else "home" },
        enterTransition = { RmTransitions.enter() },
        exitTransition = { RmTransitions.exit() },
        popEnterTransition = { RmTransitions.enter() },
        popExitTransition = { RmTransitions.popExit() },
    ) {
        composable("onboarding") {
            NickOnboardingRoute(container.nickClaim) {
                nav.navigate("home") { popUpTo("onboarding") { inclusive = true } }
            }
        }
        composable("home") {
            HomeRoute(
                games = dailyRotation(registry.all.map { it.game }),
                debugGames = registry.all.map { it.game },
                thumbnails = registry.all.mapNotNull { e -> e.thumbnail?.let { e.game.id to it } }.toMap(),
                attempts = attempts,
                scores = scores,
                clock = clock,
                onPlay = { gameId -> nav.navigate("session/$gameId") },
                debugTools = debuggable,
                onSettings = { nav.navigate("settings") },
                onHistory = { nav.navigate("history") },
            )
        }
        composable("history") {
            SeasonsHistoryRoute(
                scores = scores,
                clock = clock,
                games = registry.all.map { it.game },
                onBack = { nav.popBackStack() },
                // Today's game from the current rotation; past days use what was recorded
                // (practice runs never record, so stored bests are always the daily game).
                dailyGameId = { day -> if (day == clock.dayIndex()) todaysGame(day) else null },
            )
        }
        composable("settings") {
            SettingsRoute(
                repository = container.settings,
                appVersion = remember { appVersion(context) },
                nick = container.players.nick.value,
                onBack = { nav.popBackStack() },
                onResetAttempts = if (debuggable) {
                    { attempts.reset(clock.dayIndex()) }
                } else null,
            )
        }
        composable("session/{gameId}") { backStack ->
            val gameId = backStack.arguments?.getString("gameId").orEmpty()
            val entry = registry.find(gameId)
            PermissionGate(entry) {
                SessionRoute(
                    entry = entry,
                    attempts = attempts,
                    scores = scores,
                    clock = clock,
                    onExit = { nav.popBackStack("home", inclusive = false) },
                    // Only today's game counts; any other game (debug launcher) is a practice run.
                    practice = gameId != todaysGame(clock.dayIndex()),
                )
            }
        }
    }
}

private fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
