package com.rmgames.feature.game.escape

import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Game 01 — Escape Poruba, a touch-controlled 90-second arcade runner. */
class EscapePorubaGame(
    private val engine: EscapePorubaEngine = EscapePorubaEngine(),
) : Game {
    override val id = "01-escape-poruba"
    override val title = "Escape Poruba"
    override val description = "Drag left and right to dodge. Survive 90 seconds and escape."
    override val durationMs = EscapePorubaEngine.RUN_DURATION_MS
    override val higherWins = true

    val state = engine.state
    private val _resultVisible = MutableStateFlow(false)
    val resultVisible = _resultVisible.asStateFlow()
    private var loopJob: Job? = null

    fun dragTo(normalizedX: Float) = engine.setTargetX(normalizedX)

    /** Desktop/emulator debug controls; the screen only exposes them in debug builds. */
    fun debugStep(direction: Int) {
        val current = state.value.targetX
        engine.setTargetX(current + direction * DEBUG_STEP)
    }

    override fun start(scope: GameScope) {
        loopJob?.cancel()
        engine.start()
        _resultVisible.value = false
        loopJob = scope.hostScope.launch {
            var lastScore = -1L
            while (isActive) {
                delay(FRAME_DELAY_MS)
                engine.tick(FRAME_DELAY_MS)
                val frame = state.value
                if (frame.scoreCentis != lastScore) {
                    lastScore = frame.scoreCentis
                    scope.onScore(frame.scoreCentis)
                }
                when (frame.phase) {
                    EscapePhase.CRASHED -> {
                        delay(IMPACT_FREEZE_MS)
                        _resultVisible.value = true
                        scope.onFinished(false)
                        break
                    }
                    EscapePhase.ESCAPED -> {
                        delay(ESCAPE_ANIMATION_MS)
                        _resultVisible.value = true
                        scope.onFinished(true)
                        break
                    }
                    EscapePhase.READY, EscapePhase.RUNNING -> Unit
                }
            }
        }
    }

    override fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    private companion object {
        const val FRAME_DELAY_MS = 16L
        const val IMPACT_FREEZE_MS = 100L
        const val ESCAPE_ANIMATION_MS = 850L
        const val DEBUG_STEP = 0.2f
    }
}

/** Phase 1b registry adapter; Escape Poruba has no required hardware permissions. */
fun escapePorubaEntry(): GameEntry {
    val game = EscapePorubaGame()
    return GameEntry(game) { EscapePorubaScreen(game) }
}

@Composable
fun EscapePorubaScreen(game: EscapePorubaGame) {
    EscapePorubaScene(game)
}
