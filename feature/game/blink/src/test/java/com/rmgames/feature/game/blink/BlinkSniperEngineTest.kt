package com.rmgames.feature.game.blink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Pure JVM tests for the Blink Sniper run logic (AGENTS.md §5 — TDD).
 * Everything here is time-injected and seeded, so the suite is deterministic.
 *
 * Spec: docs/games/03-blink-sniper.md — 60s run, crosshair fixed at the centre,
 * only the centre lane can be hit (decided: other lanes are decoys), +2 bullseye,
 * +1 outer ring, bomb = instant Game Over keeping the score, 300ms cooldown.
 */
class BlinkSniperEngineTest {

    /**
     * Deterministic config: no speed jitter, one lane (the crosshair lane), no
     * small targets, and — unless overridden — a single spawn at t=0 so a test can
     * place the target exactly under the crosshair.
     */
    private fun testConfig(
        durationMs: Long = 60_000,
        startSpeed: Double = 0.5,
        endSpeed: Double = 0.5,
        startSpawnIntervalMs: Long = 60_000,
        endSpawnIntervalMs: Long = 60_000,
        bombChanceWarmup: Double = 0.0,
        bombChanceIntermediate: Double = 0.0,
        bombChanceHigh: Double = 0.0,
        centerLaneChance: Double = 1.0,
        smallChance: Double = 0.0,
    ) = BlinkSniperConfig(
        durationMs = durationMs,
        startSpeed = startSpeed,
        endSpeed = endSpeed,
        speedJitterMin = 1.0,
        speedJitterMax = 1.0,
        startSpawnIntervalMs = startSpawnIntervalMs,
        endSpawnIntervalMs = endSpawnIntervalMs,
        bombChanceWarmup = bombChanceWarmup,
        bombChanceIntermediate = bombChanceIntermediate,
        bombChanceHigh = bombChanceHigh,
        centerLaneChance = centerLaneChance,
        smallChance = smallChance,
    )

    private fun engine(config: BlinkSniperConfig = testConfig()) =
        BlinkSniperEngine(config, Random(seed = 42))

    // --- Spawning & movement -------------------------------------------------

    @Test
    fun `first tick spawns a target on the crosshair lane`() {
        val engine = engine()

        engine.tick(0)

        val target = engine.snapshot().single()
        assertEquals(1, engine.spawnedCount)
        assertEquals(CROSSHAIR_Y, target.y, 0.0)
        assertEquals(TargetKind.STANDARD, target.kind)
    }

    @Test
    fun `targets travel to the right over time`() {
        val engine = engine()
        engine.tick(0)
        val startX = engine.snapshot().single().x

        engine.tick(1_000)

        val movedX = engine.snapshot().single().x
        assertTrue("expected $movedX > $startX", movedX > startX)
    }

    @Test
    fun `targets that leave the screen are removed`() {
        val engine = engine(testConfig(startSpeed = 2.0, endSpeed = 2.0))
        engine.tick(0)
        assertEquals(1, engine.snapshot().size)

        engine.tick(2_000)

        assertEquals(0, engine.snapshot().size)
    }

    @Test
    fun `the spawn rate increases over the run`() {
        val config = BlinkSniperConfig(
            startSpawnIntervalMs = 1_400,
            endSpawnIntervalMs = 520,
            speedJitterMin = 1.0,
            speedJitterMax = 1.0,
            centerLaneChance = 1.0,
            smallChance = 0.0,
        )
        val engine = BlinkSniperEngine(config, Random(7))

        var t = 0L
        while (t < 10_000) {
            engine.tick(t)
            t += 50
        }
        val early = engine.spawnedCount

        while (t < 59_900) {
            engine.tick(t)
            t += 50
        }
        val later = engine.spawnedCount - early

        assertTrue("early=$early later=$later", later > early)
    }

    // --- Difficulty phases ---------------------------------------------------

    @Test
    fun `no bombs spawn during the warmup phase`() {
        val engine = engine(
            testConfig(
                bombChanceIntermediate = 1.0,
                startSpawnIntervalMs = 500,
                endSpawnIntervalMs = 500,
            ),
        )

        var t = 0L
        while (t < 20_000) {
            engine.tick(t)
            t += 100
        }

        assertTrue(engine.snapshot().none { it.kind == TargetKind.BOMB })
    }

    @Test
    fun `bombs spawn from the intermediate phase onwards`() {
        val engine = engine(
            testConfig(
                bombChanceIntermediate = 1.0,
                startSpawnIntervalMs = 500,
                endSpawnIntervalMs = 500,
            ),
        )

        var t = 0L
        while (t < 24_000) {
            engine.tick(t)
            t += 100
        }

        assertTrue(engine.snapshot().any { it.kind == TargetKind.BOMB })
    }

    // --- Shooting ------------------------------------------------------------

    @Test
    fun `a blink at the crosshair centre scores the bullseye`() {
        val engine = engine()
        engine.tick(0)

        // speed 0.5 u/s from x=-0.12 reaches x=0.5 after 1.24s.
        engine.tick(1_240)

        assertEquals(ShotOutcome.BULLSEYE, engine.shoot(1_240))
        assertEquals(2, engine.score)
    }

    @Test
    fun `a blink on the outer ring scores one point`() {
        val engine = engine()
        engine.tick(0)

        // x=0.55 -> distance 0.05, between the bullseye (0.030) and outer (0.070).
        engine.tick(1_340)

        assertEquals(ShotOutcome.OUTER, engine.shoot(1_340))
        assertEquals(1, engine.score)
    }

    @Test
    fun `a blink with nothing in range misses and wastes the arrow`() {
        val engine = engine()
        engine.tick(0)

        assertEquals(ShotOutcome.MISS, engine.shoot(0))
        assertEquals(0, engine.score)
        assertEquals(1, engine.shotsFired)
    }

    @Test
    fun `a second blink during the cooldown is ignored`() {
        val engine = engine()
        engine.tick(0)

        assertEquals(ShotOutcome.MISS, engine.shoot(0))
        assertEquals(ShotOutcome.IGNORED, engine.shoot(100))
        assertEquals(ShotOutcome.IGNORED, engine.shoot(299))
        assertEquals(ShotOutcome.MISS, engine.shoot(300))
        assertEquals(2, engine.shotsFired)
    }

    @Test
    fun `shooting a bomb ends the run and keeps the score`() {
        val engine = engine(
            testConfig(
                startSpawnIntervalMs = 500,
                endSpawnIntervalMs = 500,
                bombChanceIntermediate = 1.0,
            ),
        )
        engine.tick(0)
        engine.tick(1_240)
        assertEquals(ShotOutcome.BULLSEYE, engine.shoot(1_240))
        assertEquals(2, engine.score)

        // Advance to 21_740: the target spawned at 20_500 is exactly on the
        // crosshair then, and 20_500 is already inside the intermediate phase.
        var t = 1_240L
        while (t < 21_740) {
            t += 100
            engine.tick(t)
        }

        assertEquals(ShotOutcome.BOMB, engine.shoot(21_740))
        assertTrue(engine.isFinished)
        assertEquals(EndReason.BOMB, engine.endReason)
        assertEquals(2, engine.score)
        assertEquals(ShotOutcome.IGNORED, engine.shoot(22_000))
    }

    // --- Run end -------------------------------------------------------------

    @Test
    fun `the timer ends the run as a win`() {
        val engine = engine()
        engine.tick(59_900)

        assertFalse(engine.isFinished)

        engine.tick(60_000)

        assertTrue(engine.isFinished)
        assertEquals(EndReason.TIME_UP, engine.endReason)
    }

    @Test
    fun `ticking after the run ended freezes the world`() {
        val engine = engine()
        engine.tick(0)
        engine.tick(60_000)
        val frozen = engine.snapshot()

        engine.tick(61_000)

        assertEquals(frozen, engine.snapshot())
        assertEquals(60_000L, engine.elapsedMs)
    }
}
