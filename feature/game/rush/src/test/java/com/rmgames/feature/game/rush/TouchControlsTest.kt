package com.rmgames.feature.game.rush

import org.junit.Assert.assertEquals
import org.junit.Test

class TouchControlsTest {
    private val t = 40f // swipe threshold in px for these tests

    @Test
    fun `a tap anywhere hops forward`() {
        assertEquals(Hop.FORWARD, swipeHop(0f, 0f, t))
        assertEquals(Hop.FORWARD, swipeHop(12f, -20f, t)) // a wobbly tap is still a tap
    }

    @Test
    fun `swipes step left, right, back or forward`() {
        assertEquals(Hop.LEFT, swipeHop(-90f, 10f, t))
        assertEquals(Hop.RIGHT, swipeHop(90f, -15f, t))
        assertEquals(Hop.BACK, swipeHop(5f, 80f, t))
        assertEquals(Hop.FORWARD, swipeHop(-5f, -80f, t))
    }

    @Test
    fun `the larger direction of a diagonal swipe wins`() {
        assertEquals(Hop.RIGHT, swipeHop(70f, 50f, t))
        assertEquals(Hop.BACK, swipeHop(50f, 70f, t))
    }
}
