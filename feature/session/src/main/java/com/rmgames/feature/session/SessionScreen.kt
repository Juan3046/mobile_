package com.rmgames.feature.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rmgames.core.common.GameEntry

/**
 * Session host: runs one [GameEntry] (frozen before Phase 1b). See
 * docs/08-implementation-plan.md.
 *
 * Creates a [SessionGameScope] per run, starts the game when the screen enters
 * composition and closes the run (stops the game, cancels `hostScope`) when the run
 * finishes or the player leaves. The score readout and end-of-run note are Phase 1b
 * placeholders — the full HUD, attempts and result navigation land in Phase 2.
 *
 * @param entry resolved from the `GameRegistry` by `:app`; null = unknown game id.
 */
@Composable
fun SessionScreen(entry: GameEntry?) {
    if (entry == null) {
        Text("Game not found")
        return
    }

    var score by remember(entry) { mutableStateOf(0L) }
    var finished by remember(entry) { mutableStateOf<Boolean?>(null) }
    val run = remember(entry) {
        SessionGameScope(
            game = entry.game,
            reportScore = { score = it },
            reportFinished = { finished = it },
        )
    }

    DisposableEffect(entry) {
        run.startRun()
        onDispose { run.close() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        entry.screen(run)
        if (finished == null) {
            Text(
                text = "score: $score",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
            )
        } else {
            Text(
                text = "Run finished (win=$finished) · score=$score",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
        }
    }
}
