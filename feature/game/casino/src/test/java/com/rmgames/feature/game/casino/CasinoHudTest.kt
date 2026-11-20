package com.rmgames.feature.game.casino

import org.junit.Assert.assertEquals
import org.junit.Test

class CasinoHudTest {

    @Test
    fun `the hand chip counts the five hands`() {
        assertEquals("5 HANDS", handChip(CasinoState()))
        assertEquals("HAND 2 / 5", handChip(CasinoState(hand = 2, phase = CasinoPhase.PLAYING)))
        assertEquals("LAST HAND", handChip(CasinoState(hand = 5, phase = CasinoPhase.PLAYING)))
        assertEquals("FINAL", handChip(CasinoState(hand = 5, phase = CasinoPhase.FINISHED)))
    }

    @Test
    fun `each result gets a light haptic, nothing else does`() {
        assertEquals(ResultHaptic.Good, resultHaptic(HandResult.WIN))
        assertEquals(ResultHaptic.Good, resultHaptic(HandResult.BLACKJACK))
        assertEquals(ResultHaptic.Bad, resultHaptic(HandResult.LOSS))
        assertEquals(ResultHaptic.Neutral, resultHaptic(HandResult.PUSH))
        assertEquals(null, resultHaptic(null))
    }

    @Test
    fun `touch buttons are always offered, with a note that fits the camera state`() {
        assertEquals("Camera unavailable · use the buttons", touchControlsNote(CameraStatus.TOUCH_FALLBACK))
        assertEquals("Camera acting up? Tap instead", touchControlsNote(CameraStatus.READY))
        assertEquals("Camera acting up? Tap instead", touchControlsNote(CameraStatus.WAITING_FOR_FACE))
    }
}
