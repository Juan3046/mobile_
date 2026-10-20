package com.rmgames.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rmgames.core.common.Game

/**
 * Phase 1b launcher: lists every registered game so each developer can run their own
 * game on a device (home → `session/{gameId}` → session host). The real daily home
 * (title, thumbnail, how-to-play, single Play, History — docs/03-screens.md) replaces
 * this list in Phase 2; the data-down / event-up wiring (`games`, `onPlay`) stays.
 *
 * @param games metadata of the registered games, passed down by `:app`.
 * @param onPlay navigation callback — home never sees the NavHost itself.
 */
@Composable
fun HomeScreen(
    games: List<Game>,
    onPlay: (gameId: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("RM Games")
        Spacer(modifier = Modifier.height(8.dp))
        games.forEach { game ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Text(game.title)
                Text(game.description)
                Button(onClick = { onPlay(game.id) }) {
                    Text("PLAY")
                }
            }
        }
    }
}
