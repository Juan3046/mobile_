package com.rmgames.feature.game.casino

import android.Manifest
import androidx.compose.runtime.Composable
import com.rmgames.core.common.Game
import com.rmgames.core.common.GameEntry
import com.rmgames.core.common.GameScope
import com.rmgames.core.vision.HeadGesture
import com.rmgames.core.vision.HeadGestureDetector
import com.rmgames.core.vision.HeadGestureObservation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class CameraStatus { WAITING_FOR_FACE, READY, TOUCH_FALLBACK }

data class CasinoScreenState(
    val game: CasinoState = CasinoState(),
    val cameraStatus: CameraStatus = CameraStatus.WAITING_FOR_FACE,
    val presentationMessage: String? = null,
)

class PorubaCasinoGame(
    private val detector: HeadGestureDetector,
    private val engine: BlackjackEngine = BlackjackEngine(),
    private val soundPlayerFactory: () -> CasinoSoundPlayer = { SilentCasinoSoundPlayer },
) : Game {
    override val id = "01-poruba-casino"
    override val title = "Poruba Casino"
    override val description = "Play blackjack with head gestures. Nod to hit; shake to stand."
    override val durationMs = Long.MAX_VALUE
    override val higherWins = true

    private val _state = MutableStateFlow(CasinoScreenState(game = engine.state.value))
    val state: StateFlow<CasinoScreenState> = _state.asStateFlow()
    private var started = false
    private var completed = false
    private var runScope: GameScope? = null
    private var runLoopStarted = false
    private var nextHandScheduled = false
    private var lastReportedBalance: Int? = null
    private var dealScheduled = false
    private var dealerTurnScheduled = false
    private var actionPending = false
    private var soundPlayer: CasinoSoundPlayer? = null
    private var observedPlayerCards = 0
    private var observedDealerCards = 0
    private var observedPhase = CasinoPhase.WAITING
    private var observedResult: HandResult? = null

    override fun start(scope: GameScope) {
        if (started) return
        engine.reset()
        _state.value = CasinoScreenState(game = engine.state.value)
        completed = false
        runLoopStarted = false
        nextHandScheduled = false
        lastReportedBalance = null
        dealScheduled = false
        dealerTurnScheduled = false
        actionPending = false
        soundPlayer?.release()
        soundPlayer = soundPlayerFactory()
        observedPlayerCards = 0
        observedDealerCards = 0
        observedPhase = CasinoPhase.WAITING
        observedResult = null
        started = true
        runScope = scope
        scope.hostScope.launch {
            detector.observations()
                .catch { error ->
                    if (error is CancellationException) throw error
                    switchToTouchFallback()
                }
                .collect(::onObservation)
        }
        scope.hostScope.launch {
            delay(CAMERA_STARTUP_TIMEOUT_MS)
            if (!runLoopStarted) switchToTouchFallback()
        }
    }

    override fun stop() {
        started = false
        runScope = null
        soundPlayer?.release()
        soundPlayer = null
    }

    private fun onObservation(observation: HeadGestureObservation) {
        when (observation) {
            is HeadGestureObservation.FaceTracked -> {
                val firstFace = !runLoopStarted
                _state.value = _state.value.copy(cameraStatus = CameraStatus.READY)
                if (firstFace) beginRun()
            }
            HeadGestureObservation.FaceLost -> {
                switchToTouchFallback()
            }
            HeadGestureObservation.TrackingError -> {
                switchToTouchFallback()
            }
            is HeadGestureObservation.Gesture -> {
                requestAction(observation.value)
            }
        }
    }

    fun onTouchHit() = requestAction(HeadGesture.NOD)

    fun onTouchStand() = requestAction(HeadGesture.SHAKE)

    private fun requestAction(gesture: HeadGesture) {
        if (!runLoopStarted || completed || actionPending || engine.state.value.phase != CasinoPhase.PLAYING) return
        actionPending = true
        runScope?.hostScope?.launch {
            val prompt = when (gesture) {
                HeadGesture.NOD -> "You nod — taking another card…"
                HeadGesture.SHAKE -> "You shake your head — standing on ${engine.state.value.player.total}."
            }
            _state.value = _state.value.copy(presentationMessage = prompt)
            delay(ACTION_PAUSE_MS)
            _state.value = _state.value.copy(presentationMessage = null)
            when (gesture) {
                HeadGesture.NOD -> engine.hit()
                HeadGesture.SHAKE -> engine.stand()
            }
            publishState()
            actionPending = false
        }
    }

    private fun switchToTouchFallback() {
        _state.value = _state.value.copy(cameraStatus = CameraStatus.TOUCH_FALLBACK)
        if (!runLoopStarted) beginRun()
    }

    private fun beginRun() {
        if (runLoopStarted) return
        runLoopStarted = true
        engine.start()
        publishState()
    }

    private fun publishState() {
        val current = engine.state.value
        _state.value = _state.value.copy(game = current)
        if (current.player.cards.size > observedPlayerCards) {
            soundPlayer?.play(
                if (current.phase == CasinoPhase.DEALING) CasinoSound.CARD_DEALT else CasinoSound.PLAYER_HIT,
            )
        }
        if (current.dealer.cards.size > observedDealerCards) {
            soundPlayer?.play(
                if (current.phase == CasinoPhase.DEALING) CasinoSound.CARD_DEALT else CasinoSound.DEALER_DRAWS,
            )
        }
        if (current.phase == CasinoPhase.DEALER_TURN && observedPhase != CasinoPhase.DEALER_TURN) {
            soundPlayer?.play(CasinoSound.PLAYER_STANDS)
        }
        if (current.lastResult != null && current.lastResult != observedResult) {
            soundPlayer?.play(
                when (current.lastResult) {
                    HandResult.WIN -> CasinoSound.WIN
                    HandResult.LOSS -> CasinoSound.LOSS
                    HandResult.PUSH -> CasinoSound.PUSH
                    HandResult.BLACKJACK -> CasinoSound.BLACKJACK
                },
            )
        }
        observedPlayerCards = current.player.cards.size
        observedDealerCards = current.dealer.cards.size
        observedPhase = current.phase
        observedResult = current.lastResult
        if (lastReportedBalance != current.balance) {
            lastReportedBalance = current.balance
            runScope?.onScore(current.balance.toLong())
        }

        if (current.phase == CasinoPhase.BETWEEN_HANDS && current.balance >= BlackjackEngine.WAGER && !nextHandScheduled) {
            nextHandScheduled = true
            runScope?.hostScope?.launch {
                delay(RESULT_DISPLAY_MS)
                if (!completed && engine.state.value.phase == CasinoPhase.BETWEEN_HANDS) {
                    nextHandScheduled = false
                    engine.beginNextHand()
                    publishState()
                }
            }
        }

        if (current.phase == CasinoPhase.DEALING && !dealScheduled) {
            dealScheduled = true
            runScope?.hostScope?.launch {
                repeat(INITIAL_DEAL_CARD_COUNT) {
                    if (engine.state.value.phase != CasinoPhase.DEALING || completed) return@repeat
                    delay(DEAL_CARD_PAUSE_MS)
                    engine.dealNextInitialCard()
                    publishState()
                }
                if (engine.state.value.phase == CasinoPhase.DEALING && !completed) {
                    engine.finishInitialDeal()
                    publishState()
                }
                dealScheduled = false
            }
        }

        if (current.phase == CasinoPhase.DEALER_TURN && !dealerTurnScheduled) {
            dealerTurnScheduled = true
            runScope?.hostScope?.launch {
                delay(DEALER_REVEAL_PAUSE_MS)
                while (engine.state.value.phase == CasinoPhase.DEALER_TURN && !completed) {
                    val drawnCard = engine.dealerStep()
                    publishState()
                    if (drawnCard == null) break
                    delay(DEALER_CARD_PAUSE_MS)
                }
                dealerTurnScheduled = false
                publishState()
            }
        }

        if (current.phase == CasinoPhase.FINISHED && !completed) {
            completed = true
            runScope?.onFinished(current.balance >= BlackjackEngine.STARTING_BALANCE)
        }
    }

    companion object {
        private const val CAMERA_STARTUP_TIMEOUT_MS = 2_500L
        private const val DEAL_CARD_PAUSE_MS = 650L
        private const val INITIAL_DEAL_CARD_COUNT = 4
        private const val ACTION_PAUSE_MS = 700L
        private const val DEALER_REVEAL_PAUSE_MS = 1_000L
        private const val DEALER_CARD_PAUSE_MS = 1_100L
        private const val RESULT_DISPLAY_MS = 2_500L
    }
}

@Composable
fun PorubaCasinoScreen(game: PorubaCasinoGame) {
    PorubaCasinoScene(game.state, game::onTouchHit, game::onTouchStand)
}

fun porubaCasinoEntry(detector: HeadGestureDetector, cameraAvailable: Boolean = true): GameEntry {
    val game = PorubaCasinoGame(detector, soundPlayerFactory = ::ToneCasinoSoundPlayer)
    val permissions = if (cameraAvailable) listOf(Manifest.permission.CAMERA) else emptyList()
    return GameEntry(game, permissions) { PorubaCasinoScreen(game) }
}
