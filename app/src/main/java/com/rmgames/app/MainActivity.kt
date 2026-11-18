package com.rmgames.app

import android.os.Bundle
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

/** App shell: loads the persisted state, then the registry-driven nav (Main → session → Result). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as RmApplication).container
        setContent {
            // Direction A "Chunky Cabinet" (docs/09 §2A). Surface paints the themed background.
            RmTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Saved attempts/scores/settings load from disk (a few ms) before the first screen.
                    var ready by remember { mutableStateOf(container.isLoaded) }
                    LaunchedEffect(container) { container.load(); ready = true }
                    if (ready) RMGamesNav(container)
                }
            }
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
    val debuggable = remember { (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = { RmTransitions.enter() },
        exitTransition = { RmTransitions.exit() },
        popEnterTransition = { RmTransitions.enter() },
        popExitTransition = { RmTransitions.popExit() },
    ) {
        composable("home") {
            HomeRoute(
                games = dailyRotation(registry.all.map { it.game }),
                debugGames = registry.all.map { it.game },
                attempts = attempts,
                scores = scores,
                clock = clock,
                onPlay = { gameId -> nav.navigate("session/$gameId") },
                debugTools = debuggable,
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
                )
            }
        }
    }
}
