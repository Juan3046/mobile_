package com.rmgames.feature.game.compass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Game rules of Compass Chaos (docs/games/04-compass-chaos.md) — pure JVM, no Android.
 *
 * Timing note: the engine advances the run clock by exactly the ticks it is
 * given, so [tickAligned] (heading pinned to the called direction) lands on
 * exact elapsed values and turn generation is deterministic under a fixed seed.
 */
class CompassChaosEngineTest {

    /** Pins the heading to the called direction, so every tick scores. */
    private fun CompassChaosEngine.tickAligned(dtMs: Long = 16L) {
        val target = state.value.targetDegrees ?: error("no direction called yet")
        tick(dtMs, target.toFloat())
    }

    private fun assertCardinal(degrees: Int?) =
        assertTrue("not a cardinal direction: $degrees", degrees == NORTH || degrees == EAST || degrees == SOUTH || degrees == WEST)

    private fun engine(
        initial: CompassChaosState = CompassChaosState.INITIAL,
        seed: Int = 42,
    ) = CompassChaosEngine(initialState = initial, random = Random(seed))

    // --- lifecycle, first call and windows ---

    @Test
    fun `fresh run starts running with a random direction and the first window`() {
        val s = engine().state.value

        assertEquals(CompassPhase.RUNNING, s.phase)
        assertEquals(0, s.score)
        assertEquals(0L, s.elapsedMs)
        assertCardinal(s.targetDegrees)
        assertEquals(CompassChaosEngine.WINDOW_EARLY_MS, s.windowTotalMs)
        assertEquals(CompassChaosEngine.WINDOW_EARLY_MS, s.windowRemainingMs)
        assertEquals(0, s.turnDegrees)
    }

    @Test
    fun `the first call is random across seeds`() {
        val targets = (0 until 20).map { CompassChaosEngine(random = Random(it)).state.value.targetDegrees }

        assertTrue(targets.all { it == NORTH || it == EAST || it == SOUTH || it == WEST })
        assertTrue("first calls must vary, got $targets", targets.toSet().size > 1)
    }

    @Test
    fun `a heading inside the window scores and calls the next direction`() {
        val e = engine(CompassChaosState(targetDegrees = EAST))

        e.tick(16, 90f) // facing east: dead on

        val s = e.state.value
        assertEquals(1, s.score)
        assertEquals(16L, s.elapsedMs)
        assertCardinal(s.targetDegrees)
        assertTrue("the next call must turn away, got ${s.targetDegrees}", s.targetDegrees != EAST)
        assertEquals(90, s.turnDegrees)
        assertEquals(CompassChaosEngine.WINDOW_EARLY_MS, s.windowTotalMs)
        assertEquals(CompassChaosEngine.WINDOW_EARLY_MS, s.windowRemainingMs)
    }

    @Test
    fun `a heading outside the window scores nothing and keeps counting down`() {
        val e = engine(CompassChaosState(targetDegrees = EAST))

        e.tick(16, 0f) // facing north: 90° away

        val s = e.state.value
        assertEquals(0, s.score)
        assertEquals(EAST, s.targetDegrees)
        assertEquals(2_984L, s.windowRemainingMs)
        assertEquals(CompassChaosEngine.WINDOW_EARLY_MS, s.windowTotalMs)
    }

    @Test
    fun `an expired window misses and calls the next direction`() {
        val e = engine(CompassChaosState(targetDegrees = EAST))

        e.tick(3_000, 0f) // never aligned, window runs out

        val s = e.state.value
        assertEquals(0, s.score)
        assertTrue("the next call must turn away, got ${s.targetDegrees}", s.targetDegrees != EAST)
        assertEquals(90, s.turnDegrees)
        assertEquals(3_000L, s.elapsedMs)
        assertEquals(CompassChaosEngine.WINDOW_EARLY_MS, s.windowTotalMs)
    }

    @Test
    fun `the window shrinks with the difficulty curve`() {
        val mid = engine(CompassChaosState(targetDegrees = NORTH, elapsedMs = 6_990L, windowRemainingMs = 10L))
        mid.tick(16, 180f) // misses; the call crosses into phase 2 (7s)

        assertEquals(0, mid.state.value.score)
        assertEquals(CompassChaosEngine.WINDOW_MID_MS, mid.state.value.windowTotalMs)
        assertEquals(CompassChaosEngine.WINDOW_MID_MS, mid.state.value.windowRemainingMs)

        val hell = engine(CompassChaosState(targetDegrees = NORTH, elapsedMs = 13_990L, windowRemainingMs = 10L))
        hell.tick(16, 180f) // crosses into hell mode (14s)

        assertEquals(0, hell.state.value.score)
        assertEquals(CompassChaosEngine.WINDOW_HELL_MS, hell.state.value.windowTotalMs)
        assertEquals(CompassChaosEngine.WINDOW_HELL_MS, hell.state.value.windowRemainingMs)
    }

    @Test
    fun `phase one only calls ninety degree turns`() {
        val e = engine(CompassChaosState(targetDegrees = NORTH))
        val turns = mutableListOf<Int>()

        repeat(20) { e.tickAligned(); turns += e.state.value.turnDegrees } // 320 ms: still phase 1

        assertEquals(320L, e.state.value.elapsedMs)
        assertTrue("phase 1 must only turn 90°, got $turns", turns.all { it == 90 })
    }

    @Test
    fun `a hundred-eighty degree turns appear from the second phase`() {
        val e = engine(CompassChaosState(targetDegrees = NORTH, elapsedMs = 7_000L))
        val turns = mutableListOf<Int>()

        repeat(20) { e.tickAligned(); turns += e.state.value.turnDegrees } // 320 ms: phase 2

        assertTrue("only 90/180° turns, got $turns", turns.all { it == 90 || it == 180 })
        assertTrue("180° turns must be introduced in phase 2, got $turns", 180 in turns)
    }

    // --- alignment tolerance ---

    @Test
    fun `the alignment window is strictly inside forty-five degrees`() {
        val e = engine(CompassChaosState(targetDegrees = EAST))

        e.tick(16, 45f) // exactly 45° away: the boundary itself never scores
        assertEquals(0, e.state.value.score)

        e.tick(16, 45.1f) // inside the window now
        assertEquals(1, e.state.value.score)
    }

    @Test
    fun `shortest distance wraps around zero`() {
        assertEquals(20f, CompassChaosEngine.shortestDistance(350f, 10f), 0.01f)
        assertEquals(20f, CompassChaosEngine.shortestDistance(10f, 350f), 0.01f)
        assertEquals(90f, CompassChaosEngine.shortestDistance(0f, 90f), 0.01f)
        assertEquals(180f, CompassChaosEngine.shortestDistance(0f, 180f), 0.01f)
    }

    @Test
    fun `the stored heading stays normalized`() {
        val e = engine(CompassChaosState(targetDegrees = SOUTH))

        e.tick(16, 400f)
        assertEquals(40f, e.state.value.heading, 0.01f)
        assertEquals(0, e.state.value.score) // 140° away from south

        e.tick(16, -30f)
        assertEquals(330f, e.state.value.heading, 0.01f)
        assertEquals(0, e.state.value.score) // 150° away from south
    }

    // --- round end, blocking, reset ---

    @Test
    fun `the round always ends at twenty seconds with the score frozen`() {
        val e = engine()
        var guard = 0
        while (e.state.value.phase == CompassPhase.RUNNING && guard++ < 2_000) e.tickAligned()

        val done = e.state.value
        assertEquals(CompassPhase.DONE, done.phase)
        assertEquals(CompassChaosEngine.ROUND_MS, done.elapsedMs)
        assertTrue("aligned play must score, got ${done.score}", done.score > 0)

        val frozen = done.score
        e.tick(5_000, 0f)
        assertEquals(frozen, e.state.value.score)
        assertEquals(CompassChaosEngine.ROUND_MS, e.state.value.elapsedMs)
    }

    @Test
    fun `blocking freezes the run without a score`() {
        val e = engine(CompassChaosState(targetDegrees = EAST))
        repeat(3) { e.tickAligned() }
        assertEquals(3, e.state.value.score)

        e.block()

        assertEquals(CompassPhase.BLOCKED, e.state.value.phase)
        assertEquals(0, e.state.value.score)

        e.tick(16, 90f) // a blocked run never resumes or scores
        assertEquals(CompassPhase.BLOCKED, e.state.value.phase)
        assertEquals(0, e.state.value.score)
    }

    @Test
    fun `reset restores a clean run with a fresh call`() {
        val e = engine()
        repeat(3) { e.tickAligned() }
        e.block()
        e.reset()

        val s = e.state.value
        assertEquals(CompassPhase.RUNNING, s.phase)
        assertEquals(0, s.score)
        assertEquals(0L, s.elapsedMs)
        assertEquals(CompassChaosEngine.WINDOW_EARLY_MS, s.windowTotalMs)
        assertEquals(0f, s.heading, 0.01f)
        assertCardinal(s.targetDegrees)
    }

    private companion object {
        const val NORTH = 0
        const val EAST = 90
        const val SOUTH = 180
        const val WEST = 270
    }
}
