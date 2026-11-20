package com.rmgames.feature.game.casino

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow

private val CasinoGreen = Color(0xFF0C422F)
private val CasinoGold = Color(0xFFFFD166)
private val CasinoCream = Color(0xFFFFF8E8)
private val CasinoMint = Color(0xFF8FE3B0)
private val CasinoCoral = Color(0xFFFF8D8D)

@Composable
fun PorubaCasinoScene(
    state: StateFlow<CasinoScreenState>,
    onHit: () -> Unit,
    onStand: () -> Unit,
) {
    val screen by state.collectAsState()
    val game = screen.game
    Column(
        modifier = Modifier.fillMaxSize().background(CasinoGreen).padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("PORUBA CASINO", color = CasinoGold, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(50)) {
                Text("♾  NO TIME LIMIT", modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = CasinoCream, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("🍌 ${game.balance}", color = CasinoGold, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(cameraLabel(screen.cameraStatus), color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(22.dp))
        Text("DEALER", color = CasinoCream.copy(alpha = 0.75f), fontSize = 13.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        CardsRow(game.dealer.cards, reveal = game.dealerCardsVisible)
        Text(
            if (game.dealerCardsVisible) "${game.dealer.total}" else "?",
            color = CasinoCream,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 5.dp),
        )
        Spacer(Modifier.weight(1f))
        CasinoNotice(screen)
        Spacer(Modifier.weight(1f))
        Text("YOU · ${game.player.total}", color = CasinoCream.copy(alpha = 0.75f), fontSize = 13.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        CardsRow(game.player.cards, reveal = true)
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            GestureHint("↓", "NOD", "HIT")
            Spacer(Modifier.width(28.dp))
            GestureHint("↔", "SHAKE", "STAND")
        }
        if (screen.cameraStatus == CameraStatus.TOUCH_FALLBACK) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Camera unavailable · use touch controls",
                color = CasinoGold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onHit,
                    enabled = game.phase == CasinoPhase.PLAYING && screen.presentationMessage == null,
                    modifier = Modifier.weight(1f),
                ) { Text("HIT · DRAW") }
                Button(
                    onClick = onStand,
                    enabled = game.phase == CasinoPhase.PLAYING && screen.presentationMessage == null,
                    modifier = Modifier.weight(1f),
                ) { Text("STAND") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Fictional bananas · no real money", color = CasinoCream.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
private fun CardsRow(cards: List<Card>, reveal: Boolean) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        cards.forEachIndexed { index, card ->
            if (index > 0) Spacer(Modifier.width(7.dp))
            val hidden = !reveal && index == 1
            Surface(
                color = if (hidden) Color(0xFFB48337) else CasinoCream,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.width(54.dp).height(76.dp),
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (hidden) "RM" else "${card.rank.label}\n${card.symbol}",
                        color = if (!hidden && card.suit in setOf(Suit.HEARTS, Suit.DIAMONDS)) Color(0xFFC73E45) else Color(0xFF18382D),
                        textAlign = TextAlign.Center,
                        fontSize = if (hidden) 14.sp else 19.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureHint(icon: String, gesture: String, action: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$icon  $gesture", color = CasinoGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(action, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun CasinoNotice(screen: CasinoScreenState) {
    val game = screen.game
    val result = game.lastResult
    val (icon, title, accent) = when {
        screen.presentationMessage != null -> Triple("👀", "ACTION CONFIRMED", CasinoGold)
        result == HandResult.BLACKJACK -> Triple("✨", "NATURAL BLACKJACK!", CasinoMint)
        result == HandResult.WIN -> Triple("🏆", "YOU WIN THIS HAND", CasinoMint)
        result == HandResult.PUSH -> Triple("🤝", "IT'S A PUSH", CasinoGold)
        result == HandResult.LOSS -> Triple("💥", "HAND LOST", CasinoCoral)
        game.phase == CasinoPhase.WAITING -> Triple("🎰", "WELCOME TO THE TABLE", CasinoGold)
        game.phase == CasinoPhase.DEALING -> Triple("🃏", "DEALING THE CARDS", CasinoGold)
        game.phase == CasinoPhase.PLAYING -> Triple("🎯", "YOUR TURN", CasinoGold)
        game.phase == CasinoPhase.DEALER_TURN -> Triple("🎩", "DEALER'S TURN", CasinoGold)
        game.phase == CasinoPhase.FINISHED -> Triple("🍌", "RUN OVER", CasinoCoral)
        else -> Triple("🪙", "NEXT HAND", CasinoGold)
    }
    val detail = screen.presentationMessage ?: game.message
    Surface(
        color = Color(0xFF092F23),
        contentColor = CasinoCream,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = accent.copy(alpha = 0.16f), shape = RoundedCornerShape(18.dp)) {
                Text(icon, modifier = Modifier.padding(12.dp), fontSize = 28.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = accent, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(5.dp))
                Text(detail, color = CasinoCream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start)
                if (game.phase == CasinoPhase.DEALING) {
                    Spacer(Modifier.height(5.dp))
                    Text("INITIAL DEAL · ${game.player.cards.size + game.dealer.cards.size} OF 4 CARDS", color = CasinoCream.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
                if (game.phase == CasinoPhase.DEALER_TURN) {
                    Spacer(Modifier.height(5.dp))
                    Text("DEALER DRAWS UNTIL 17", color = CasinoCream.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
            }
        }
    }
}

private fun cameraLabel(status: CameraStatus): String = when (status) {
    CameraStatus.WAITING_FOR_FACE -> "Center your face"
    CameraStatus.READY -> "Camera ready"
    CameraStatus.TOUCH_FALLBACK -> "Touch controls active"
}
