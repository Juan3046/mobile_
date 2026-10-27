package com.rmgames.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rmgames.core.common.GameRegistry
import com.rmgames.core.sensors.AndroidAccelerometerSensor
import com.rmgames.core.sensors.AndroidCompassSensor
import com.rmgames.core.sensors.AndroidGyroscopeSensor
import com.rmgames.core.vision.MlKitBlinkDetector
import com.rmgames.feature.game.blink.blinkSniperEntry
import com.rmgames.feature.game.compass.compassChaosEntry
import com.rmgames.feature.game.escape.escapePorubaEntry
import com.rmgames.feature.game.onion.onionRollEntry
import com.rmgames.feature.game.soda.sodaShakeEntry
import com.rmgames.feature.home.HomeScreen
import com.rmgames.feature.session.SessionScreen

/** Phase 1b shell: registry-driven nav. Full flows (onboarding/result/history) land in Phase 2. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RMGamesNav()
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
fun RMGamesNav() {
    val nav = rememberNavController()
    val registry = rememberGameRegistry()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                games = registry.all.map { it.game },
                onPlay = { gameId -> nav.navigate("session/$gameId") },
            )
        }
        composable("session/{gameId}") { backStack ->
            val gameId = backStack.arguments?.getString("gameId").orEmpty()
            val entry = registry.find(gameId)
            PermissionGate(entry) {
                SessionScreen(entry)
            }
        }
    }
}
