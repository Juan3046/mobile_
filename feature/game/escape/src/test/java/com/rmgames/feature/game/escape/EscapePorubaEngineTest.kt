package com.rmgames.feature.game.escape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EscapePorubaEngineTest {

    @Test
    fun `player follows horizontal input without leaving road`() {
        val engine = EscapePorubaEngine(random = Random(1))
        engine.start()
        engine.setTargetX(1f)

        repeat(20) { engine.tick(50L) }

        assertTrue(engine.state.value.playerX in 0.0f..1.0f)
        assertTrue(engine.state.value.playerX > 0.5f)
    }

    @Test
    fun `target input is clamped to road bounds`() {
        val engine = EscapePorubaEngine(random = Random(2))
        engine.start()
        engine.setTargetX(4f)
        repeat(60) { engine.tick(50L) }
        assertTrue(engine.state.value.playerX <= EscapePorubaEngine.PLAYER_X_MAX)

        engine.setTargetX(-4f)
        repeat(60) { engine.tick(50L) }
        assertTrue(engine.state.value.playerX >= EscapePorubaEngine.PLAYER_X_MIN)
    }

    @Test
    fun `collision ends run and score is elapsed centiseconds`() {
        val engine = EscapePorubaEngine(random = Random(3))
        engine.start()
        engine.debugSpawnHazard(
            lane = nearestLane(engine.state.value.playerX),
            y = EscapePorubaEngine.PLAYER_Y,
        )
        engine.tick(16L)

        assertEquals(EscapePhase.CRASHED, engine.state.value.phase)
        assertEquals(engine.state.value.elapsedMs / 10L, engine.state.value.scoreCentis)
    }

    @Test
    fun `surviving to ninety seconds finishes as escaped`() {
        val engine = EscapePorubaEngine(random = Random(4))
        engine.start()
        engine.disableSpawnsForTest()

        repeat(1800) { engine.tick(50L) }

        assertEquals(EscapePhase.ESCAPED, engine.state.value.phase)
        assertEquals(90_000L, engine.state.value.elapsedMs)
        assertEquals(9_000L, engine.state.value.scoreCentis)
    }

    @Test
    fun `difficulty grows continuously and reaches hell mode`() {
        val engine = EscapePorubaEngine(random = Random(5))
        engine.start()
        engine.disableSpawnsForTest()
        repeat(300) { engine.tick(50L) }
        val at15 = engine.state.value.difficulty
        repeat(600) { engine.tick(50L) }
        val at45 = engine.state.value.difficulty
        repeat(900) { engine.tick(50L) }

        assertTrue(at15 > 0f)
        assertTrue(at45 > at15)
        assertEquals(1f, engine.state.value.difficulty, 0.0001f)
    }

    @Test
    fun `every generated pattern has an open lane reachable before impact`() {
        val generator = EscapePatternGenerator(Random(42))

        repeat(500) { index ->
            val difficulty = index / 499f
            val playerX = (index % 100) / 99f
            val pattern = generator.create(
                difficulty = difficulty,
                playerX = playerX,
                worldSpeed = EscapePorubaEngine.worldSpeedAt(difficulty),
            )
            val secondsToImpact = (EscapePorubaEngine.PLAYER_Y - EscapePorubaEngine.PATTERN_SPAWN_Y) /
                EscapePorubaEngine.worldSpeedAt(difficulty)
            val reachable = pattern.safeLanes.any { lane ->
                kotlin.math.abs(EscapePorubaEngine.laneCenter(lane) - playerX) <=
                    EscapePorubaEngine.PLAYER_MAX_SPEED * secondsToImpact + EscapePorubaEngine.SAFE_LANE_HALF_WIDTH
            }
            assertTrue("pattern $index has no reachable gap", reachable)
            assertTrue(pattern.safeLanes.isNotEmpty())
            val physicallyOpen = pattern.safeLanes.any { safeLane ->
                val safeX = EscapePorubaEngine.laneCenter(safeLane)
                pattern.hazards.none { hazard ->
                    kotlin.math.abs(safeX - EscapePorubaEngine.laneCenter(hazard.lane)) <=
                        EscapePorubaEngine.PLAYER_HALF_WIDTH + EscapePorubaEngine.HAZARD_HALF_WIDTH
                }
            }
            assertTrue("pattern $index has no collision-free gap", physicallyOpen)
        }
    }

    @Test
    fun `escaped run cannot advance beyond ninety seconds`() {
        val engine = EscapePorubaEngine(random = Random(6))
        engine.start()
        engine.disableSpawnsForTest()
        repeat(1800) { engine.tick(50L) }
        engine.tick(500L)
        assertEquals(90_000L, engine.state.value.elapsedMs)
        assertFalse(engine.state.value.phase == EscapePhase.RUNNING)
    }

    private fun nearestLane(x: Float): Int =
        (x * EscapePorubaEngine.LANE_COUNT).toInt().coerceIn(0, EscapePorubaEngine.LANE_COUNT - 1)
}
