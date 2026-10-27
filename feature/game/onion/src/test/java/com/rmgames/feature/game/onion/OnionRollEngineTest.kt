package com.rmgames.feature.game.onion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Game rules of Onion Roll (docs/games/02-onion-roll.md) — pure JVM, no Android.
 *
 * Timing note: the engine ticks in fixed 16 ms steps, so `tickFor(ms)` lands on
 * exact elapsed values (e.g. 1_500 ms → score 150 centiseconds) and spawn cadence
 * is deterministic. Collision tests run before the first spawn (600 ms) unless
 * stated otherwise.
 */
class OnionRollEngineTest {

    private fun engine(
        initial: OnionRollState = OnionRollState.INITIAL,
        seed: Int = 42,
        fallAtMs: Long = 82_000L,
        hardCapMs: Long = 90_000L,
    ) = OnionRollEngine(
        initialState = initial,
        random = Random(seed),
        fallAtMs = fallAtMs,
        hardCapMs = hardCapMs,
    )

    private fun OnionRollEngine.tickFor(totalMs: Long, omegaZ: Float = 0f, keySteer: Float? = null) {
        var remaining = totalMs
        while (remaining > 0) {
            val step = minOf(16L, remaining)
            tick(step, omegaZ, keySteer)
            remaining -= step
        }
    }

    // --- lifecycle & score ---

    @Test
    fun `fresh run starts centered with no fruits`() {
        val state = engine().state.value

        assertEquals(Phase.RUNNING, state.phase)
        assertEquals(0.5f, state.onionX, 0.001f)
        assertTrue(state.fruits.isEmpty())
        assertEquals(0L, state.scoreCentis)
        assertEquals(0f, state.progress, 0.0001f)
    }

    @Test
    fun `score is reported in centiseconds`() {
        val e = engine()

        e.tickFor(1_500)

        assertEquals(150L, e.state.value.scoreCentis)
    }

    @Test
    fun `the run falls at the target window and freezes the score`() {
        val e = engine()

        e.tickFor(82_500)
        assertEquals(Phase.FALLING, e.state.value.phase)
        assertEquals(8_200L, e.state.value.scoreCentis)
        assertTrue(e.state.value.progress >= 1f)

        e.tickFor(700)
        assertEquals(Phase.DONE, e.state.value.phase)
        assertEquals(8_200L, e.state.value.scoreCentis)
    }

    @Test
    fun `the hard cap forces a fall even if the target window is pushed out`() {
        val e = engine(fallAtMs = 95_000L)

        e.tickFor(89_000)
        assertEquals(Phase.RUNNING, e.state.value.phase)

        e.tickFor(1_500)
        assertEquals(Phase.FALLING, e.state.value.phase)
    }

    // --- steering (gyroscope rate model) ---

    @Test
    fun `tilting the phone left steers the onion left`() {
        val e = engine()

        e.tickFor(500, omegaZ = 1f) // positive ω_z = counter-clockwise = lean left

        assertTrue("expected leftward movement, got ${e.state.value.onionX}", e.state.value.onionX < 0.5f)
    }

    @Test
    fun `tilting the phone right steers the onion right`() {
        val e = engine()

        e.tickFor(500, omegaZ = -1f)

        assertTrue("expected rightward movement, got ${e.state.value.onionX}", e.state.value.onionX > 0.5f)
    }

    @Test
    fun `onion parks in place when rotation stops`() {
        val e = engine()
        e.tickFor(400, omegaZ = -1f)
        val parkedAt = e.state.value.onionX
        assertTrue(parkedAt > 0.5f)

        e.tickFor(3_000, omegaZ = 0f)

        assertEquals(parkedAt, e.state.value.onionX, 0.06f)
    }

    @Test
    fun `steering speed is capped for comfort`() {
        val e = engine()

        e.tickFor(100, omegaZ = -50f) // violent twist

        val moved = e.state.value.onionX - 0.5f
        assertTrue("moved $moved — expected a small, capped advance", moved < 0.12f)
        assertTrue("moved $moved — expected to actually move", moved > 0.03f)
    }

    @Test
    fun `steering is clamped to the belt edges`() {
        val right = engine()
        right.tickFor(5_000, omegaZ = -5f)
        assertEquals(ONION_X_MAX, right.state.value.onionX, 0.001f)

        val left = engine()
        left.tickFor(5_000, omegaZ = 5f)
        assertEquals(ONION_X_MIN, left.state.value.onionX, 0.001f)
    }

    @Test
    fun `gyro bias below the deadband never drifts the onion`() {
        val e = engine()

        e.tickFor(10_000, omegaZ = 0.02f) // typical sensor bias

        assertEquals(0.5f, e.state.value.onionX, 0.01f)
    }

    @Test
    fun `debug keys steer without a gyroscope and win over it`() {
        val keys = engine()
        keys.tickFor(500, keySteer = 1f)
        assertTrue(keys.state.value.onionX > 0.5f)

        val overridden = engine()
        overridden.tickFor(500, omegaZ = 1f, keySteer = 1f) // gyro says left, key says right
        assertTrue(overridden.state.value.onionX > 0.5f)
    }

    // --- belt ramp ---

    @Test
    fun `belt speed ramps up as the run progresses`() {
        val e = engine()

        e.tickFor(2_000)
        val early = e.state.value.beltSpeed

        e.tickFor(58_000) // t = 60 s
        assertTrue("early=$early late=${e.state.value.beltSpeed}", e.state.value.beltSpeed > early * 2f)
    }

    // --- fruit traffic ---

    @Test
    fun `fruits spawn and ride toward the onion`() {
        val e = engine()

        e.tickFor(4_000)
        val fruits = e.state.value.fruits

        assertTrue("expected fruits on the belt", fruits.isNotEmpty())
        assertTrue(fruits.all { it.y > -0.1f })
        assertTrue("expected a fruit well past the spawn line", fruits.any { it.y > 0.1f })
    }

    @Test
    fun `spawn decisions never fill every lane`() {
        val random = Random(7)

        repeat(300) {
            val lanes = OnionRollEngine.chooseSpawnLanes(progress = 0.9f, random = random)
            assertTrue(lanes.isNotEmpty())
            assertTrue("filled every lane: $lanes", lanes.distinct().size < LANES)
        }
    }

    @Test
    fun `early game spawns exactly one fruit at a time`() {
        val random = Random(7)

        repeat(100) {
            assertEquals(1, OnionRollEngine.chooseSpawnLanes(progress = 0.2f, random = random).size)
        }
    }

    // --- blocking (fruits occupy space; never touch the timer) ---

    @Test
    fun `a fruit in the way blocks the onion from entering its lane`() {
        val wall = Fruit(FruitType.APPLE, x = laneCenter(1), y = 0.60f)
        val start = OnionRollState.INITIAL.copy(onionX = laneCenter(0), fruits = listOf(wall))
        val e = engine(initial = start)

        e.tickFor(600, omegaZ = -5f) // hard right

        val blockedAt = laneCenter(1) - FruitType.APPLE.halfWidth - ONION_HALF_W
        assertEquals(blockedAt, e.state.value.onionX, 0.01f)
    }

    @Test
    fun `a fruit stops against the onion and rides on once the onion dodges`() {
        val approaching = Fruit(FruitType.APPLE, x = laneCenter(1), y = 0.55f)
        val e = engine(initial = OnionRollState.INITIAL.copy(fruits = listOf(approaching)))

        e.tickFor(300) // assertions run before the first spawn (600 ms)
        val pausedY = e.state.value.fruits.single().y
        assertTrue("fruit should stop just above the onion, was at $pausedY", pausedY < ONION_Y)
        assertTrue("…and not far above it", pausedY > 0.5f)

        e.tickFor(250)
        assertEquals(pausedY, e.state.value.fruits.single().y, 0.0001f)

        e.tickFor(1_500, omegaZ = -1f) // dodge right
        assertTrue(e.state.value.onionX > 0.6f)
        assertTrue("fruit should ride on after the dodge", e.state.value.fruits.maxBy { it.y }.y > pausedY)
    }
}
