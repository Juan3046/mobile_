package com.rmgames.feature.game.soda

import org.junit.Assert.assertEquals
import org.junit.Test

class SodaShakeHapticsTest {

    @Test
    fun `every counted shake buzzes and the pop buzzes harder`() {
        assertEquals(ShakeBuzz.Shake, shakeBuzz(previous = 0, now = 1))
        assertEquals(ShakeBuzz.Shake, shakeBuzz(previous = 18, now = 19))
        assertEquals(ShakeBuzz.Pop, shakeBuzz(previous = 19, now = SodaShakeEngine.SHAKES_TO_POP))
    }

    @Test
    fun `nothing buzzes without a new shake or when a run resets`() {
        assertEquals(null, shakeBuzz(previous = 3, now = 3))
        assertEquals(null, shakeBuzz(previous = 20, now = 0))
    }
}
