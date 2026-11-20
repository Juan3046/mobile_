package com.rmgames.feature.game.casino

import com.rmgames.core.common.GameScope
import com.rmgames.core.vision.FakeHeadGestureDetector
import com.rmgames.core.vision.HeadGesture
import com.rmgames.core.vision.HeadGestureObservation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PorubaCasinoGameTest {
    @Test fun `device without front camera does not require camera permission`() {
        val entry = porubaCasinoEntry(FakeHeadGestureDetector(), cameraAvailable = false)
        assertTrue(entry.requiredPermissions.isEmpty())
    }

    @Test fun `game waits for face before dealing and routes nod to hit`() = runTest {
        val detector = FakeHeadGestureDetector()
        val sounds = RecordingSoundPlayer()
        val game = PorubaCasinoGame(
            detector,
            BlackjackEngine(deckFactory = { deckForBust() }),
            soundPlayerFactory = { sounds },
        )
        val scope = RecordingScope(this)
        game.start(scope)
        runCurrent()
        assertEquals(CameraStatus.WAITING_FOR_FACE, game.state.value.cameraStatus)

        detector.emit(HeadGestureObservation.FaceTracked(0f, 0f))
        runCurrent()
        assertEquals(CasinoPhase.DEALING, game.state.value.game.phase)
        assertEquals("Hand 1 of 5 · shuffling the deck…", game.state.value.game.message)
        advanceTimeBy(PorubaCasinoGame.DEAL_CARD_PAUSE_MS)
        runCurrent()
        assertEquals(1, game.state.value.game.player.cards.size)
        assertEquals("Your first card: 10 of clubs", game.state.value.game.message)
        assertEquals(CasinoPhase.DEALING, game.state.value.game.phase)
        advanceTimeBy(3 * PorubaCasinoGame.DEAL_CARD_PAUSE_MS)
        runCurrent()
        assertEquals(CasinoPhase.PLAYING, game.state.value.game.phase)
        assertEquals(4, sounds.events.count { it == CasinoSound.CARD_DEALT })
        assertEquals(listOf(100L), scope.scores)

        detector.emit(HeadGestureObservation.Gesture(HeadGesture.NOD))
        runCurrent()
        advanceTimeBy(PorubaCasinoGame.ACTION_PAUSE_MS)
        runCurrent()
        assertEquals(90, game.state.value.game.balance)
        assertEquals(CasinoPhase.BETWEEN_HANDS, game.state.value.game.phase)
        assertEquals(90L, scope.scores.last())
        assertTrue(sounds.events.contains(CasinoSound.PLAYER_HIT))
        assertTrue(sounds.events.contains(CasinoSound.LOSS))
        scope.cancelHost()
    }

    @Test fun `player turn and run remain open with no time limit`() = runTest {
        val detector = FakeHeadGestureDetector()
        val game = PorubaCasinoGame(detector, BlackjackEngine(deckFactory = { deckForBust() }))
        val scope = RecordingScope(this)
        game.start(scope)
        runCurrent()
        detector.emit(HeadGestureObservation.FaceTracked(0f, 0f))
        runCurrent()

        advanceTimeBy(4 * PorubaCasinoGame.DEAL_CARD_PAUSE_MS)
        runCurrent()
        assertEquals(CasinoPhase.PLAYING, game.state.value.game.phase)
        assertFalse(scope.finished)

        advanceTimeBy(120_000)
        runCurrent()
        assertEquals(CasinoPhase.PLAYING, game.state.value.game.phase)
        assertFalse(scope.finished)
        assertEquals(100L, scope.scores.last())
        assertEquals(Long.MAX_VALUE, game.durationMs)
        scope.cancelHost()
    }

    @Test fun `the run ends after five hands and reports the final balance`() = runTest {
        val game = PorubaCasinoGame(FakeHeadGestureDetector(), BlackjackEngine(deckFactory = { deckForBust() }))
        val scope = RecordingScope(this)
        game.start(scope)
        runCurrent()
        advanceTimeBy(PorubaCasinoGame.CAMERA_STARTUP_TIMEOUT_MS) // no face: touch controls
        runCurrent()
        repeat(BlackjackEngine.HANDS_PER_RUN) {
            advanceTimeBy(4 * PorubaCasinoGame.DEAL_CARD_PAUSE_MS)
            runCurrent()
            assertEquals(CasinoPhase.PLAYING, game.state.value.game.phase)
            game.onTouchHit() // 17 + 5 = bust
            runCurrent()
            advanceTimeBy(PorubaCasinoGame.ACTION_PAUSE_MS)
            runCurrent()
            assertFalse(scope.finished)
            advanceTimeBy(PorubaCasinoGame.RESULT_DISPLAY_MS)
            runCurrent()
        }
        assertEquals(CasinoPhase.FINISHED, game.state.value.game.phase)
        assertTrue(scope.finished)
        assertEquals(50L, scope.scores.last())
        scope.cancelHost()
    }

    @Test fun `touch buttons work even while the camera tracks a face`() = runTest {
        val detector = FakeHeadGestureDetector()
        val game = PorubaCasinoGame(detector, BlackjackEngine(deckFactory = { deckForBust() }))
        val scope = RecordingScope(this)
        game.start(scope)
        runCurrent()
        detector.emit(HeadGestureObservation.FaceTracked(0f, 0f))
        runCurrent()
        advanceTimeBy(4 * PorubaCasinoGame.DEAL_CARD_PAUSE_MS)
        runCurrent()
        assertEquals(CameraStatus.READY, game.state.value.cameraStatus)

        game.onTouchHit() // 17 + 5 = bust
        runCurrent()
        advanceTimeBy(PorubaCasinoGame.ACTION_PAUSE_MS)
        runCurrent()
        assertEquals(90, game.state.value.game.balance)
        scope.cancelHost()
    }

    @Test fun `camera tracking error enables touch controls and touch hit plays`() = runTest {
        val detector = FakeHeadGestureDetector()
        val game = PorubaCasinoGame(detector, BlackjackEngine(deckFactory = { deckForBust() }))
        val scope = RecordingScope(this)
        game.start(scope)
        runCurrent()
        detector.emit(HeadGestureObservation.TrackingError)
        runCurrent()
        assertEquals(CameraStatus.TOUCH_FALLBACK, game.state.value.cameraStatus)
        assertEquals(CasinoPhase.DEALING, game.state.value.game.phase)

        advanceTimeBy(4 * PorubaCasinoGame.DEAL_CARD_PAUSE_MS)
        runCurrent()
        assertEquals(CasinoPhase.PLAYING, game.state.value.game.phase)
        game.onTouchHit()
        runCurrent()
        assertEquals("You nod — taking another card…", game.state.value.presentationMessage)
        advanceTimeBy(PorubaCasinoGame.ACTION_PAUSE_MS)
        runCurrent()
        assertEquals(CasinoPhase.BETWEEN_HANDS, game.state.value.game.phase)
        assertEquals(90, game.state.value.game.balance)
        scope.cancelHost()
    }

    @Test fun `stand and dealer draw play distinct effects before hand result`() = runTest {
        val sounds = RecordingSoundPlayer()
        val deck = listOf(
            Card(Rank.TEN, Suit.CLUBS), Card(Rank.TEN, Suit.DIAMONDS),
            Card(Rank.SEVEN, Suit.CLUBS), Card(Rank.SIX, Suit.DIAMONDS),
            Card(Rank.FIVE, Suit.HEARTS),
        ).toMutableList()
        val game = PorubaCasinoGame(
            FakeHeadGestureDetector(),
            BlackjackEngine(deckFactory = { deck.toMutableList() }),
            soundPlayerFactory = { sounds },
        )
        val scope = RecordingScope(this)
        game.start(scope)
        runCurrent()
        advanceTimeBy(PorubaCasinoGame.CAMERA_STARTUP_TIMEOUT_MS)
        runCurrent()
        advanceTimeBy(4 * PorubaCasinoGame.DEAL_CARD_PAUSE_MS)
        runCurrent()

        game.onTouchStand()
        runCurrent()
        advanceTimeBy(PorubaCasinoGame.ACTION_PAUSE_MS + PorubaCasinoGame.DEALER_REVEAL_PAUSE_MS)
        runCurrent()
        assertTrue(sounds.events.contains(CasinoSound.PLAYER_STANDS))
        assertTrue(sounds.events.contains(CasinoSound.DEALER_DRAWS))

        advanceTimeBy(PorubaCasinoGame.DEALER_CARD_PAUSE_MS)
        runCurrent()
        assertEquals(CasinoPhase.BETWEEN_HANDS, game.state.value.game.phase)
        assertTrue(sounds.events.contains(CasinoSound.LOSS))
        scope.cancelHost()
    }

    @Test fun `missing camera face falls back to touch controls after startup timeout`() = runTest {
        val detector = FakeHeadGestureDetector()
        val game = PorubaCasinoGame(detector)
        val scope = RecordingScope(this)
        game.start(scope)
        runCurrent()

        advanceTimeBy(PorubaCasinoGame.CAMERA_STARTUP_TIMEOUT_MS)
        runCurrent()

        assertEquals(CameraStatus.TOUCH_FALLBACK, game.state.value.cameraStatus)
        assertEquals(CasinoPhase.DEALING, game.state.value.game.phase)
        scope.cancelHost()
    }

    private fun deckForBust() = listOf(
        Card(Rank.TEN, Suit.CLUBS), Card(Rank.TEN, Suit.DIAMONDS),
        Card(Rank.SEVEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS),
        Card(Rank.FIVE, Suit.HEARTS),
    ).toMutableList()

    private class RecordingSoundPlayer : CasinoSoundPlayer {
        val events = mutableListOf<CasinoSound>()
        override fun play(sound: CasinoSound) { events += sound }
        override fun release() = Unit
    }

    private class RecordingScope(parent: CoroutineScope) : GameScope {
        private val hostJob = SupervisorJob(parent.coroutineContext[Job])
        override val hostScope = CoroutineScope(parent.coroutineContext + hostJob)
        val scores = mutableListOf<Long>()
        var finished = false
        override fun onScore(value: Long) { scores += value }
        override fun onFinished(win: Boolean) { finished = true }
        fun cancelHost() { hostJob.cancel() }
    }
}
