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
        timeLimitMs: Long? = null,
        spawnFruits: Boolean = true,
    ) = OnionRollEngine(
        initialState = initial,
        random = Random(seed),
        timeLimitMs = timeLimitMs,
        spawnFruits = spawnFruits,
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
        assertEquals(0L, state.scoreSeconds)
        assertEquals(0f, state.intensity, 0.0001f)
    }

    @Test
    fun `score is whole seconds survived`() {
        val e = engine(spawnFruits = false)

        e.tickFor(1_500)
        assertEquals(1L, e.state.value.scoreSeconds)

        e.tickFor(1_000)
        assertEquals(2L, e.state.value.scoreSeconds)
    }

    @Test
    fun `the belt is endless - with no hits the run never ends`() {
        val e = engine(spawnFruits = false)

        e.tickFor(200_000) // well past the old 82 s fall and 90 s cap

        assertEquals(Phase.RUNNING, e.state.value.phase)
        assertEquals(200L, e.state.value.scoreSeconds)
        assertEquals(ONION_Y, e.state.value.onionY, 0.0001f)
    }

    @Test
    fun `being shoved past the end roller ends the run and freezes the score`() {
        val atEdge = OnionRollState.INITIAL.copy(elapsedMs = 30_000L, onionY = END_OF_BELT_Y + 0.005f)
        val e = engine(initial = atEdge, spawnFruits = false)

        e.tick(16, 0f, null)
        assertEquals(Phase.FALLING, e.state.value.phase)
        assertEquals(30L, e.state.value.scoreSeconds)

        e.tickFor(2_000)
        assertEquals(Phase.DONE, e.state.value.phase)
        assertEquals(30L, e.state.value.scoreSeconds)
    }

    @Test
    fun `the loss banner holds after the fall animation before the run is done`() {
        val e = engine(spawnFruits = false, timeLimitMs = 5_000L)
        e.tickFor(5_000)
        assertEquals(Phase.FALLING, e.state.value.phase)

        e.tickFor(800) // fall animation (700 ms) is over…
        assertEquals(1f, e.state.value.fallProgress, 0.0001f)
        assertEquals(Phase.FALLING, e.state.value.phase) // …but "YOU LOST!" is still up
        assertTrue(e.state.value.fallElapsedMs in 700L..900L)

        e.tickFor(1_300)
        assertEquals(Phase.DONE, e.state.value.phase)
    }

    @Test
    fun `an optional time limit ends the run (tests only - production is endless)`() {
        val e = engine(spawnFruits = false, timeLimitMs = 10_000L)

        e.tickFor(9_984)
        assertEquals(Phase.RUNNING, e.state.value.phase)

        e.tickFor(32)
        assertEquals(Phase.FALLING, e.state.value.phase)
        assertEquals(10L, e.state.value.scoreSeconds)
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
        assertTrue("moved $moved — expected a capped advance", moved < 0.2f)
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
    fun `belt starts 50 percent faster than the 0_286 tuning`() {
        val e = engine(spawnFruits = false)

        e.tick(16, 0f, null)

        assertEquals(0.286f * 1.5f, e.state.value.beltSpeed, 0.002f)
    }

    @Test
    fun `belt accelerates 10 percent faster than the 45 s ramp`() {
        val e = engine(spawnFruits = false)

        e.tickFor(60_000)

        // 0.429 * (1 + 60 / 40.9)^1.3 ≈ 1.388 (the 45 s ramp gave ≈ 1.291)
        assertEquals(1.388f, e.state.value.beltSpeed, 0.01f)
    }

    @Test
    fun `belt speed keeps ramping without limit`() {
        val e = engine(spawnFruits = false)
        e.tickFor(90_000)
        val at90 = e.state.value.beltSpeed

        e.tickFor(60_000)

        assertTrue("at90=$at90 at150=${e.state.value.beltSpeed}", e.state.value.beltSpeed > at90 * 1.3f)
    }

    @Test
    fun `traffic intensity ramps up and saturates at 82 s`() {
        val e = engine(spawnFruits = false)
        e.tickFor(41_000)
        val mid = e.state.value.intensity
        assertTrue("mid=$mid", mid > 0.2f && mid < 0.5f)

        e.tickFor(80_000)
        assertEquals(1f, e.state.value.intensity, 0.0001f)
    }

    @Test
    fun `belt speed ramps up as the run progresses`() {
        val e = engine(spawnFruits = false) // an idle onion would be knocked off early

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
            val lanes = OnionRollEngine.chooseSpawnLanes(intensity = 0.9f, random = random)
            assertTrue(lanes.isNotEmpty())
            assertTrue("filled every lane: $lanes", lanes.distinct().size < LANES)
        }
    }

    @Test
    fun `early game spawns exactly one fruit at a time`() {
        val random = Random(7)

        repeat(100) {
            assertEquals(1, OnionRollEngine.chooseSpawnLanes(intensity = 0.2f, random = random).size)
        }
    }

    // --- blocking (fruits occupy space; never touch the timer) ---

    @Test
    fun `a fruit in the way blocks the onion from entering its lane`() {
        val wall = Fruit(FruitType.APPLE, x = laneCenter(1), y = 0.60f)
        val start = OnionRollState.INITIAL.copy(onionX = laneCenter(0), fruits = listOf(wall))
        val e = engine(initial = start)

        e.tickFor(350, omegaZ = -5f) // hard right, while the fruit is still alongside

        // Hitboxes are 80 % of the sprites, so the onion stops past the drawn edge.
        val blockedAt = laneCenter(1) - FruitType.APPLE.hitHalfWidth - ONION_HIT_HALF_W
        assertEquals(blockedAt, e.state.value.onionX, 0.01f)
        assertTrue(blockedAt > laneCenter(1) - FruitType.APPLE.halfWidth - ONION_HALF_W)
    }

    @Test
    fun `steering is responsive — a moderate tilt crosses a full lane`() {
        val e = engine()

        // ~15° tilted over 0.25 s ≈ 1.05 rad/s
        e.tickFor(250, omegaZ = -1.05f)
        e.tickFor(200)

        val moved = e.state.value.onionX - 0.5f
        assertTrue("moved $moved — a 15° tilt should move about one lane (1/3)", moved > 0.3f)
    }

    // --- knockback (head-on hits cost ground) ---

    @Test
    fun `a fruit hitting the onion knocks it back toward the end of the belt`() {
        val approaching = Fruit(FruitType.APPLE, x = laneCenter(1), y = 0.55f)
        val e = engine(initial = OnionRollState.INITIAL.copy(fruits = listOf(approaching)), spawnFruits = false)

        e.tickFor(400)

        val s = e.state.value
        assertTrue("onion should be knocked back, was at ${s.onionY}", s.onionY > ONION_Y + 0.03f)
        assertTrue("the fruit keeps riding past where it used to stop", s.fruits.single().y > 0.58f)
    }

    @Test
    fun `a knocked-back onion never overlaps the fruit`() {
        val approaching = Fruit(FruitType.BANANA, x = laneCenter(1), y = 0.50f)
        val e = engine(initial = OnionRollState.INITIAL.copy(fruits = listOf(approaching)), spawnFruits = false)

        repeat(60) {
            e.tick(16, 0f, null)
            val s = e.state.value
            s.fruits.forEach { f ->
                val overlapY = kotlin.math.abs(s.onionY - f.y) < ONION_HIT_HALF_H + f.type.hitHalfHeight - 0.001f
                val overlapX = kotlin.math.abs(s.onionX - f.x) < ONION_HIT_HALF_W + f.type.hitHalfWidth - 0.001f
                assertTrue("onion overlaps fruit at tick $it: $s", !(overlapX && overlapY))
            }
        }
    }

    @Test
    fun `a fruit in another lane does not knock the onion`() {
        val passing = Fruit(FruitType.APPLE, x = laneCenter(0), y = 0.55f)
        val e = engine(initial = OnionRollState.INITIAL.copy(fruits = listOf(passing)), spawnFruits = false)

        e.tickFor(1_000)

        assertEquals(ONION_Y, e.state.value.onionY, 0.0001f)
    }

    @Test
    fun `once clear the onion walks back to its home row`() {
        val e = engine(initial = OnionRollState.INITIAL.copy(onionY = 0.78f), spawnFruits = false)

        e.tickFor(1_000)
        val y = e.state.value.onionY
        assertTrue("expected recovery, was at $y", y < 0.78f && y > ONION_Y)

        e.tickFor(5_000)
        assertEquals(ONION_Y, e.state.value.onionY, 0.0001f)
    }

    @Test
    fun `repeated hits shove the onion over the end roller`() {
        val chaser = Fruit(FruitType.APPLE, x = laneCenter(1), y = 0.70f)
        val start = OnionRollState.INITIAL.copy(onionY = 0.82f, fruits = listOf(chaser))
        val e = engine(initial = start, spawnFruits = false)

        e.tickFor(1_500) // never dodges

        assertEquals(Phase.FALLING, e.state.value.phase)
    }

    // --- hitboxes ---

    @Test
    fun `hitboxes are 80 percent of the drawn size`() {
        assertEquals(ONION_HALF_W * 0.8f, ONION_HIT_HALF_W, 0.0001f)
        assertEquals(ONION_HALF_H * 0.8f, ONION_HIT_HALF_H, 0.0001f)
        FruitType.entries.forEach {
            assertEquals(it.halfWidth * 0.8f, it.hitHalfWidth, 0.0001f)
            assertEquals(it.halfHeight * 0.8f, it.hitHalfHeight, 0.0001f)
        }
    }

    @Test
    fun `sprites that only graze each other do not collide`() {
        // Drawn boxes overlap by 0.01 sideways; hitboxes are still apart.
        val apple = Fruit(FruitType.APPLE, x = laneCenter(1), y = ONION_Y)
        val grazingX = laneCenter(1) - FruitType.APPLE.halfWidth - ONION_HALF_W + 0.01f
        val e = engine(
            initial = OnionRollState.INITIAL.copy(onionX = grazingX, fruits = listOf(apple)),
            spawnFruits = false,
        )

        e.tickFor(300, keySteer = 0.3f) // nudge right: still free to move

        assertTrue("onion should move past the drawn edge", e.state.value.onionX > grazingX)
        assertEquals(ONION_Y, e.state.value.onionY, 0.0001f) // and no knockback
    }
}
