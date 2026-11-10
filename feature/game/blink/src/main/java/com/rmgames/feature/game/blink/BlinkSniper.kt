package com.rmgames.feature.game.blink

import android.Manifest
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import com.rmgames.core.vision.BlinkDetector
import com.rmgames.core.vision.EyeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Game 03 — Blink Sniper (docs/games/03-blink-sniper.md).
 *
 * Owns two loops inside [GameScope.hostScope] — the host cancels that scope when
 * the run ends, so nothing here can leak:
 *  - a 16ms ticker that advances [BlinkSniperEngine] and publishes UI state;
 *  - a collector over [BlinkDetector] that turns an OPEN -> CLOSED edge into a shot
 *    (the engine enforces the 300ms cooldown).
 *
 * Reporting order matters: state and score are published first, and [GameScope.onFinished]
 * is always the last call — the host cancels `hostScope` there, so the game must not
 * suspend afterwards.
 *
 * @param detector injected by `:app` (`MlKitBlinkDetector` in production, a fake in tests).
 * @param timeSource milliseconds source; injected so tests can drive a virtual clock.
 * @param engineFactory creates a fresh engine per run — the [Game] instance is shared
 *   across runs by the registry, so no run state may live on the instance.
 */
class BlinkSniperGame(
    private val detector: BlinkDetector,
    private val timeSource: () -> Long = { System.currentTimeMillis() },
    private val engineFactory: () -> BlinkSniperEngine = { BlinkSniperEngine() },
) : Game {

    override val id = "03-blink-sniper"
    override val title = "Blink Sniper"
    override val description = "Blink to shoot targets. 60 seconds."
    override val durationMs = 60_000L
    override val higherWins = true

    private val _state = MutableStateFlow(BlinkSniperUiState())
    val state: StateFlow<BlinkSniperUiState> = _state.asStateFlow()

    private var reportedScore = 0
    private var eyesOpen = true

    override fun start(scope: GameScope) {
        val engine = engineFactory()
        val startedAt = timeSource()
        reportedScore = 0
        eyesOpen = true
        _state.value = BlinkSniperUiState()

        scope.hostScope.launch {
            while (isActive) {
                delay(TICK_MS)
                engine.tick(timeSource() - startedAt)
                publish(engine)
                if (engine.isFinished) {
                    reportScore(engine, scope)
                    scope.onFinished(engine.endReason == EndReason.TIME_UP)
                    return@launch
                }
            }
        }

        scope.hostScope.launch {
            var previous = EyeState.OPEN
            detector.eyeState().collect { current ->
                val isBlink = previous == EyeState.OPEN && current == EyeState.CLOSED
                previous = current
                eyesOpen = current == EyeState.OPEN

                var bombHit = false
                if (isBlink) {
                    when (engine.shoot(timeSource() - startedAt)) {
                        ShotOutcome.BULLSEYE, ShotOutcome.OUTER -> reportScore(engine, scope)
                        ShotOutcome.BOMB -> {
                            reportScore(engine, scope)
                            bombHit = true
                        }
                        ShotOutcome.MISS, ShotOutcome.IGNORED -> Unit
                    }
                }
                publish(engine)
                if (bombHit) scope.onFinished(false)
            }
        }
    }

    /**
     * The host owns the lifecycle: [com.rmgames.feature.session.SessionGameScope]
     * cancels `hostScope` right after this, which stops both loops.
     */
    override fun stop() = Unit

    /** Pushes the score to the host only when it actually moved. */
    private fun reportScore(engine: BlinkSniperEngine, scope: GameScope) {
        if (engine.score == reportedScore) return
        reportedScore = engine.score
        scope.onScore(reportedScore.toLong())
    }

    private fun publish(engine: BlinkSniperEngine) {
        _state.value = BlinkSniperUiState(
            score = engine.score,
            elapsedMs = engine.elapsedMs,
            remainingMs = (engine.durationMs - engine.elapsedMs).coerceAtLeast(0L),
            targets = engine.snapshot(),
            eyesOpen = eyesOpen,
            shotsFired = engine.shotsFired,
            finished = engine.isFinished,
            endReason = engine.endReason,
        )
    }

    private companion object {
        const val TICK_MS = 16L
    }
}

/**
 * Frozen Phase 1b entry signature (docs/08-implementation-plan.md — Game Hosting
 * Contract): `:app` wires this once into the `GameRegistry`, so gameplay work never
 * touches shared navigation.
 *
 * @param detector injected by `:app` (`MlKitBlinkDetector` in production,
 * `FakeBlinkDetector` in tests).
 */
fun blinkSniperEntry(detector: BlinkDetector): GameEntry {
    val game = BlinkSniperGame(detector)
    return GameEntry(
        game = game,
        requiredPermissions = listOf(Manifest.permission.CAMERA),
    ) { BlinkSniperScreen(game) }
}
