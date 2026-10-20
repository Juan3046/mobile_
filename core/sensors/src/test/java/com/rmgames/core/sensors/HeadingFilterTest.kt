package com.rmgames.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadingFilterTest {

    private val epsilon = 0.001f

    @Test
    fun `first sample passes through unchanged`() {
        val filter = HeadingFilter(alpha = 0.5f)

        assertEquals(90f, filter.filter(90f), epsilon)
    }

    @Test
    fun `moves halfway toward target with alpha 0_5`() {
        val filter = HeadingFilter(alpha = 0.5f)
        filter.filter(100f)

        // 100 → 80 is -20°, half-step lands on 90°.
        assertEquals(90f, filter.filter(80f), epsilon)
    }

    @Test
    fun `wraps through zero instead of through 180`() {
        val filter = HeadingFilter(alpha = 0.5f)
        filter.filter(350f)

        // Shortest path 350 → 10 is +20° through 0°, so half-step lands on 0°.
        assertEquals(0f, filter.filter(10f), epsilon)
    }

    @Test
    fun `wraps backwards through 359`() {
        val filter = HeadingFilter(alpha = 0.5f)
        filter.filter(10f)

        // Shortest path 10 → 350 is -20° through 0°, so half-step lands on 360 → normalized 0°.
        assertEquals(0f, filter.filter(350f), epsilon)
    }

    @Test
    fun `output always stays in 0 until 360`() {
        val filter = HeadingFilter(alpha = 0.3f)

        var heading = 180f
        repeat(100) { heading = filter.filter((heading + 173f) % 360f) }

        assertTrue(heading >= 0f && heading < 360f)
    }
}
