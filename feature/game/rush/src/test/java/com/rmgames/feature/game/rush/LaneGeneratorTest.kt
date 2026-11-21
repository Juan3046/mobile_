package com.rmgames.feature.game.rush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaneGeneratorTest {

    @Test
    fun `the first rows are safe pavement`() {
        val g = LaneGenerator(seed = 7)
        (0 until SAFE_START_ROWS).forEach { assertEquals(LaneKind.PAVEMENT, g.lane(it).kind) }
    }

    @Test
    fun `the same seed builds the same street`() {
        val a = LaneGenerator(42)
        val b = LaneGenerator(42)
        (0 until 80).forEach { assertEquals(a.lane(it), b.lane(it)) }
    }

    @Test
    fun `there is a mix of every lane kind`() {
        val g = LaneGenerator(3)
        val kinds = (0 until 200).map { g.lane(it).kind }.toSet()
        assertEquals(LaneKind.entries.toSet(), kinds)
    }

    @Test
    fun `lanes get faster further up`() {
        val g = LaneGenerator(5)
        fun avg(rows: IntRange) = rows.map { g.lane(it) }.filter { it.kind == LaneKind.ROAD }.map { it.speed }.average()
        assertTrue(avg(60..140) > avg(3..60))
    }

    @Test
    fun `every pond can be crossed — gaps between floes stay short`() {
        val g = LaneGenerator(11)
        (0 until 300).map { g.lane(it) }.filter { it.kind == LaneKind.POND }.forEach { lane ->
            val starts = lane.movers.map { it.start }.sorted()
            val longestGap = starts.zipWithNext { a, b -> b - a }.maxOrNull() ?: 0f
            assertTrue("gap $longestGap too long", longestGap <= 5f)
        }
    }
}
