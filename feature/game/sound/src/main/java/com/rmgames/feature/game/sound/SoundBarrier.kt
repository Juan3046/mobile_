package com.rmgames.feature.game.sound

import android.Manifest
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Game 06 — Sound Barrier (docs/games/06-sound-barrier.md).
 *
 * Owns two loops inside [GameScope.hostScope] — the host cancels that scope when
 * the run ends, so nothing here can leak:
 *  - a 16ms ticker that advances [SoundBarrierEngine] from the loudest-or-quiet
 *    decision and publishes UI state;
 *  - a collector over [SoundSensor] that keeps the latest input level.
 *
 * Reporting order matters: state and score are published first, and [GameScope.onFinished]
 * is always the last call — the host cancels `hostScope` there, so the game must not
 * suspend afterwards.
 *
 * @param sensor injected by `:app` (`AndroidSoundSensor` in production, a fake in tests).
 * @param config threshold and physics tunables; injected so tests can pin the gap.
 * @param timeSource milliseconds source; injected so tests can drive a virtual clock.
 * @param engineFactory creates a fresh engine per run — the [Game] instance is shared
 *   across runs by the registry, so no run state may live on the instance.
 */
class SoundBarrierGame(
    private val sensor: SoundSensor,
    private val config: SoundBarrierConfig = SoundBarrierConfig(),
    private val timeSource: () -> Long = { System.currentTimeMillis() },
    private val engineFactory: (SoundBarrierConfig) -> SoundBarrierEngine = { SoundBarrierEngine(it) },
) : Game {

    override val id = "06-sound-barrier"
    override val title = "Sound Barrier"
    override val description = "Make noise to rise, stay quiet to drop. Pass every gap. 60 seconds."
    override val durationMs: Long get() = config.durationMs
    override val higherWins = true

    private val _state = MutableStateFlow(SoundBarrierUiState())
    val state: StateFlow<SoundBarrierUiState> = _state.asStateFlow()

    private var reportedScore = 0
    private var soundLevel = 0f
    private var debugSoundHeld = false

    override fun start(scope: GameScope) {
        val engine = engineFactory(config)
        val startedAt = timeSource()
        reportedScore = 0
        soundLevel = 0f
        debugSoundHeld = false
        _state.value = SoundBarrierUiState()

        scope.hostScope.launch {
            while (isActive) {
                delay(TICK_MS)
                val nowMs = timeSource() - startedAt
                engine.tick(nowMs, soundDetected())
                publish(engine)
                reportScore(engine, scope)
                if (engine.isFinished) {
                    scope.onFinished(engine.endReason == EndReason.TIME_UP)
                    return@launch
                }
            }
        }

        scope.hostScope.launch {
            sensor.readings().collect { level -> soundLevel = level }
        }
    }

    /**
     * The host owns the lifecycle: [com.rmgames.feature.session.SessionGameScope]
     * cancels `hostScope` right after this, which stops both loops.
     */
    override fun stop() = Unit

    /**
     * Emulator / desktop debug input: holding Space reads as sound while the
     * microphone stays quiet (docs/games/06-sound-barrier.md — Controls).
     */
    fun setDebugSound(held: Boolean) {
        debugSoundHeld = held
    }

    /** Loud is the input over the configured threshold, or the held debug key. */
    private fun soundDetected(): Boolean =
        debugSoundHeld || soundLevel >= config.loudThreshold

    /** Pushes the score to the host only when it actually moved. */
    private fun reportScore(engine: SoundBarrierEngine, scope: GameScope) {
        if (engine.score == reportedScore) return
        reportedScore = engine.score
        scope.onScore(reportedScore.toLong())
    }

    private fun publish(engine: SoundBarrierEngine) {
        _state.value = SoundBarrierUiState(
            score = engine.score,
            elapsedMs = engine.elapsedMs,
            remainingMs = (engine.durationMs - engine.elapsedMs).coerceAtLeast(0L),
            ballY = engine.ballY,
            ballX = config.playerX,
            ballRadius = config.playerRadius,
            soundLevel = soundLevel,
            soundDetected = soundDetected(),
            barriers = engine.snapshot(),
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
 * @param sensor injected by `:app` (`AndroidSoundSensor` in production,
 * a fake in tests). The microphone gate is the runtime `RECORD_AUDIO` permission
 * this entry declares; a denied permission blocks the run with an explanation and
 * never records a fake score (docs/02-gameplay-rules.md).
 */
fun soundBarrierEntry(sensor: SoundSensor): GameEntry {
    val game = SoundBarrierGame(sensor)
    return GameEntry(
        game = game,
        requiredPermissions = listOf(Manifest.permission.RECORD_AUDIO),
    ) { SoundBarrierScreen(game) }
}
