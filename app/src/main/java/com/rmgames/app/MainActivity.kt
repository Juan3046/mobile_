package com.rmgames.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rmgames.feature.game.blink.BlinkSniperScreen
import com.rmgames.feature.game.compass.CompassChaosScreen
import com.rmgames.feature.game.escape.EscapePorubaScreen
import com.rmgames.feature.game.onion.OnionRollScreen
import com.rmgames.feature.game.soda.SodaShakeScreen
import com.rmgames.feature.home.HomeScreen
import com.rmgames.feature.session.SessionScreen

/** Phase 0 shell. Full flows (onboarding/result/history) land in Phase 1 (owner: P2). */
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

@Composable
fun RMGamesNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen() }
        composable("session/{gameId}") { backStack ->
            SessionScreen(backStack.arguments?.getString("gameId").orEmpty())
        }
        composable("game/01-escape-poruba") { EscapePorubaScreen() }
        composable("game/02-onion-roll") { OnionRollScreen() }
        composable("game/03-blink-sniper") { BlinkSniperScreen() }
        composable("game/04-compass-chaos") { CompassChaosScreen() }
        composable("game/05-soda-shake") { SodaShakeScreen() }
    }
}
