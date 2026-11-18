package com.rmgames.feature.game.onion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Top-down view of the checkout belt (docs/games/02-onion-roll.md): ribbed belt,
 * the fixed end roller, the groceries and the onion (sprites in OnionRollArt.kt),
 * the red danger glow, the minimal HUD (score counter in seconds) and the
 * "YOU LOST!" banner once the onion falls. Sound effects and vibration follow
 * the state through [CueDetector] → [OnionRollFeedback].
 *
 * Renders [OnionRollState] only — the [Game] object stays the sole gateway to the
 * host (docs/08). Debug keys (A/D, arrows) feed [onDebugSteer] and the RESTART
 * button calls [onDebugRestart]; both are gated on BuildConfig.DEBUG, so release
 * builds ignore them entirely.
 */
@Composable
fun OnionRollScreen(
    state: StateFlow<OnionRollState>,
    onDebugSteer: (Float?) -> Unit,
    practice: StateFlow<Boolean>,
    onDebugRestart: (practiceScope: CoroutineScope) -> Unit,
) {
    val s by state.collectAsState()
    val isPractice by practice.collectAsState()
    // Practice runs live on this scope, so they stop when the screen leaves.
    val screenScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Sound effects + vibration (docs/games/02 — Audio & Haptics); no music in the MVP.
    val appContext = LocalContext.current.applicationContext
    val feedback = remember { OnionRollFeedback(appContext) }
    DisposableEffect(feedback) { onDispose { feedback.release() } }
    LaunchedEffect(state) {
        val detector = CueDetector()
        state.collect { frame -> detector.next(frame).forEach(feedback::play) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event -> handleDebugKey(event, onDebugSteer) },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val travel = (s.elapsedMs / 1000f) * s.beltSpeed * size.height
            val endY = END_OF_BELT_Y * size.height
            drawBelt(scrollPx = travel, endY = endY)
            val danger = dangerLevel(s.onionY)
            drawDangerGlow(endY = endY, alpha = dangerGlowAlpha(s.elapsedMs, danger), danger = danger)

            s.fruits.forEach { drawFruit(it) }

            // The onion: an accelerating, tumbling drop once the belt runs out.
            val onionCenter = Offset(
                s.onionX * size.width,
                s.onionY * size.height + fallDropFraction(s.fallProgress) * size.height * FALL_DROP_SCREENS,
            )
            drawOnion(
                center = onionCenter,
                rx = ONION_HALF_W * size.width,
                ry = ONION_HALF_H * size.height,
                tiltDeg = fallTiltDeg(s.fallProgress),
                shadowAlpha = 1f - s.fallProgress,
            )
        }

        ScoreCounter(
            seconds = s.scoreSeconds,
            // The host hides its own "score: N" text for this game (drawsOwnHud = true).
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp),
        )

        if (s.phase != Phase.RUNNING) {
            LossBanner(fallElapsedMs = s.fallElapsedMs, survivedSeconds = s.scoreSeconds)
        }

        if (isPractice) {
            Text(
                text = "PRACTICE · not scored",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(PracticePill, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        // Playtest helper only: release builds never show it (2-attempts/day rule).
        if (BuildConfig.DEBUG) {
            Button(
                onClick = { onDebugRestart(screenScope) },
                colors = ButtonDefaults.buttonColors(containerColor = RestartColor, contentColor = Color.White),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Text("RESTART", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** The only HUD element: whole seconds survived (= the score), big and bold, popping on each tick. */
@Composable
private fun ScoreCounter(seconds: Long, modifier: Modifier = Modifier) {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(seconds) {
        if (seconds > 0L) {
            pop.snapTo(COUNTER_POP_SCALE)
            pop.animateTo(1f, animationSpec = tween(durationMillis = 220))
        }
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .background(CounterPill, RoundedCornerShape(50))
            .padding(horizontal = 28.dp, vertical = 6.dp),
    ) {
        Text(
            text = "$seconds",
            style = TextStyle(
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), offset = Offset(0f, 4f), blurRadius = 8f),
            ),
        )
        Text(
            text = "s",
            color = CounterUnit,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 9.dp),
        )
    }
}

/** "YOU LOST!" overlay: dims the belt, pops the banner in, shows the survived time. */
@Composable
private fun LossBanner(fallElapsedMs: Long, survivedSeconds: Long) {
    val alpha = lossBannerAlpha(fallElapsedMs)
    val scale = lossBannerScale(fallElapsedMs)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * alpha)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }
                .background(BannerBackground, RoundedCornerShape(24.dp))
                .padding(horizontal = 36.dp, vertical = 24.dp),
        ) {
            Text(
                text = "YOU LOST!",
                style = TextStyle(
                    color = BannerTitle,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 6f), blurRadius = 10f),
                ),
            )
            Text(
                text = "Survived $survivedSeconds s",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private fun handleDebugKey(event: KeyEvent, onDebugSteer: (Float?) -> Unit): Boolean {
    if (!BuildConfig.DEBUG) return false
    val steer = when (event.key) {
        Key.A, Key.DirectionLeft -> -1f
        Key.D, Key.DirectionRight -> 1f
        else -> return false
    }
    return when (event.type) {
        KeyEventType.KeyDown -> {
            onDebugSteer(steer)
            true
        }

        KeyEventType.KeyUp -> {
            onDebugSteer(null)
            true
        }

        else -> false
    }
}

private val CounterPill = Color(0xB3000000)
private val CounterUnit = Color(0xFFFFD54F)
private val BannerBackground = Color(0xE6261326)
private val BannerTitle = Color(0xFFFF5252)
private val RestartColor = Color(0xCC37474F)
private val PracticePill = Color(0xCC1565C0)

private const val COUNTER_POP_SCALE = 1.18f

/** Full fall drop distance, in screen heights. */
private const val FALL_DROP_SCREENS = 0.6f
