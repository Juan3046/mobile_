package com.rmgames.feature.game.escape

import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EscapePhase { READY, RUNNING, CRASHED, ESCAPED }

enum class EscapeHazardKind { CONTAINER, ROADWORKS, POTHOLE, CAR, BUS, TRAM, BICYCLE, PEDESTRIAN, SCOOTER, ICE }

data class EscapeHazard(
    val lane: Int,
    val y: Float,
    val kind: EscapeHazardKind,
)

data class EscapePattern(
    val safeLanes: List<Int>,
    val hazards: List<EscapeHazard>,
)

data class EscapePorubaState(
    val phase: EscapePhase = EscapePhase.READY,
    val elapsedMs: Long = 0L,
    val playerX: Float = 0.5f,
    val targetX: Float = 0.5f,
    val scoreCentis: Long = 0L,
    val distanceMeters: Int = 0,
    val difficulty: Float = 0f,
    val worldSpeed: Float = 0.78f,
    val hazards: List<EscapeHazard> = emptyList(),
    val crashedKind: EscapeHazardKind? = null,
)

/** Pure Kotlin simulation. No Android or Compose dependencies; deterministic with an injected Random. */
class EscapePorubaEngine(
    random: Random = Random.Default,
) {
    private val patternGenerator = EscapePatternGenerator(random)
    private val _state = MutableStateFlow(EscapePorubaState())
    val state: StateFlow<EscapePorubaState> = _state.asStateFlow()

    private var nextPatternAtMs = FIRST_PATTERN_AT_MS
    private var inputTargetX = PLAYER_START_X
    private var spawnsEnabled = true

    fun start() {
        nextPatternAtMs = FIRST_PATTERN_AT_MS
        inputTargetX = PLAYER_START_X
        spawnsEnabled = true
        _state.value = EscapePorubaState(phase = EscapePhase.RUNNING)
    }

    fun setTargetX(x: Float) {
        inputTargetX = x.coerceIn(PLAYER_X_MIN, PLAYER_X_MAX)
    }

    fun tick(rawDtMs: Long) {
        if (rawDtMs <= 0L) return
        val current = _state.value
        if (current.phase != EscapePhase.RUNNING) return

        // Clamp frame gaps to avoid tunnelling when the app is briefly busy.
        val dtMs = rawDtMs.coerceAtMost(MAX_TICK_MS)
        val elapsed = (current.elapsedMs + dtMs).coerceAtMost(RUN_DURATION_MS)
        val difficulty = difficultyAt(elapsed)
        val worldSpeed = worldSpeedAt(difficulty)
        val dtSeconds = dtMs / 1000f
        val playerX = moveToward(current.playerX, inputTargetX, PLAYER_MAX_SPEED * dtSeconds)

        var hazards = current.hazards.map { it.copy(y = it.y + worldSpeed * dtSeconds) }
            .filter { it.y < HAZARD_CULL_Y }

        val previousPatternPassed = hazards.none {
            it.y < PLAYER_Y + PLAYER_HALF_HEIGHT + HAZARD_HALF_HEIGHT
        }
        if (spawnsEnabled && previousPatternPassed && elapsed >= nextPatternAtMs && elapsed < RUN_DURATION_MS) {
            val pattern = patternGenerator.create(difficulty, playerX, worldSpeed)
            hazards = hazards + pattern.hazards
            nextPatternAtMs = elapsed + patternIntervalMs(difficulty)
        }

        val collision = hazards.firstOrNull { collides(playerX, it) }
        val phase = when {
            collision != null -> EscapePhase.CRASHED
            elapsed >= RUN_DURATION_MS -> EscapePhase.ESCAPED
            else -> EscapePhase.RUNNING
        }
        val score = elapsed / 10L
        val distance = (elapsed / 1000f * distanceSpeedAt(difficulty)).toInt()
        _state.value = EscapePorubaState(
            phase = phase,
            elapsedMs = elapsed,
            playerX = playerX,
            targetX = inputTargetX,
            scoreCentis = score,
            distanceMeters = distance,
            difficulty = difficulty,
            worldSpeed = worldSpeed,
            hazards = hazards,
            crashedKind = collision?.kind,
        )
    }

    /** Test seam for controlled collision setup. */
    internal fun debugSpawnHazard(lane: Int, y: Float) {
        _state.value = _state.value.copy(
            hazards = _state.value.hazards + EscapeHazard(lane.coerceIn(0, LANE_COUNT - 1), y, EscapeHazardKind.CAR),
        )
    }

    /** Test seam: lets tests verify the 90-second finish without simulating traffic. */
    internal fun disableSpawnsForTest() {
        spawnsEnabled = false
    }

    private fun collides(playerX: Float, hazard: EscapeHazard): Boolean {
        val x = laneCenter(hazard.lane)
        return abs(hazard.y - PLAYER_Y) <= PLAYER_HALF_HEIGHT + HAZARD_HALF_HEIGHT &&
            abs(playerX - x) <= PLAYER_HALF_WIDTH + HAZARD_HALF_WIDTH
    }

    companion object {
        const val RUN_DURATION_MS = 90_000L
        const val LANE_COUNT = 5
        const val PLAYER_Y = 0.74f
        const val PLAYER_START_X = 0.5f
        const val ROAD_LEFT = 0.31f
        const val ROAD_WIDTH = 0.38f
        const val PLAYER_X_MIN = ROAD_LEFT + 0.02f
        const val PLAYER_X_MAX = ROAD_LEFT + ROAD_WIDTH - 0.02f
        const val PLAYER_MAX_SPEED = 1.8f // normalized screen widths per second
        const val PLAYER_HALF_WIDTH = 0.02f
        const val PLAYER_HALF_HEIGHT = 0.035f
        const val HAZARD_HALF_WIDTH = 0.032f
        const val HAZARD_HALF_HEIGHT = 0.045f
        const val SAFE_LANE_HALF_WIDTH = 0.023f
        const val PATTERN_SPAWN_Y = -0.18f
        private const val MIN_WORLD_SPEED = 0.78f
        private const val MAX_WORLD_SPEED = 1.3f
        private const val MAX_TICK_MS = 50L
        private const val FIRST_PATTERN_AT_MS = 700L
        private const val HAZARD_CULL_Y = 1.15f

        fun difficultyAt(elapsedMs: Long): Float =
            (elapsedMs.coerceIn(0L, RUN_DURATION_MS).toFloat() / RUN_DURATION_MS).pow(1.8f)

        fun worldSpeedAt(difficulty: Float): Float =
            MIN_WORLD_SPEED + (MAX_WORLD_SPEED - MIN_WORLD_SPEED) * difficulty.coerceIn(0f, 1f)

        fun laneCenter(lane: Int): Float = ROAD_LEFT + (lane + 0.5f) * ROAD_WIDTH / LANE_COUNT

        private fun moveToward(current: Float, target: Float, maxDelta: Float): Float = when {
            target > current -> minOf(current + maxDelta, target)
            target < current -> maxOf(current - maxDelta, target)
            else -> current
        }

        private fun collides(playerX: Float, hazard: EscapeHazard): Boolean {
            val x = laneCenter(hazard.lane)
            return abs(hazard.y - PLAYER_Y) <= PLAYER_HALF_HEIGHT + HAZARD_HALF_HEIGHT &&
                abs(playerX - x) <= PLAYER_HALF_WIDTH + HAZARD_HALF_WIDTH
        }

        private fun patternIntervalMs(difficulty: Float): Long {
            val d = difficulty.coerceIn(0f, 1f)
            return (1_350f - 1_080f * d.pow(1.8f)).toLong().coerceAtLeast(270L)
        }

        private fun distanceSpeedAt(difficulty: Float): Float = 6f + 9f * difficulty.coerceIn(0f, 1f)
    }

    private fun patternIntervalMs(difficulty: Float): Long = patternIntervalMsStatic(difficulty)

    private fun distanceSpeedAt(difficulty: Float): Float = distanceSpeedStatic(difficulty)

    private fun patternIntervalMsStatic(difficulty: Float): Long {
        val d = difficulty.coerceIn(0f, 1f)
        return (1_350f - 1_080f * d.pow(1.8f)).toLong().coerceAtLeast(270L)
    }

    private fun distanceSpeedStatic(difficulty: Float): Float = 6f + 9f * difficulty.coerceIn(0f, 1f)
}

/** Selects at least one collision-free lane reachable before the row reaches the player. */
class EscapePatternGenerator(private val random: Random = Random.Default) {
    fun create(difficulty: Float, playerX: Float, worldSpeed: Float): EscapePattern {
        val safeLanes = (0 until EscapePorubaEngine.LANE_COUNT).filter { lane ->
            val secondsToImpact =
                (EscapePorubaEngine.PLAYER_Y - EscapePorubaEngine.PATTERN_SPAWN_Y) / worldSpeed
            abs(EscapePorubaEngine.laneCenter(lane) - playerX) <=
                EscapePorubaEngine.PLAYER_MAX_SPEED * secondsToImpact + EscapePorubaEngine.SAFE_LANE_HALF_WIDTH
        }
        // At the configured spawn distance and speed cap at least one lane is always reachable.
        check(safeLanes.isNotEmpty()) { "No reachable safe lane; pattern must not spawn" }

        val openCount = if (difficulty < 0.22f) 2 else if (difficulty < 0.62f && random.nextFloat() < 0.25f) 2 else 1
        val chosenSafe = safeLanes.shuffled(random).take(openCount.coerceAtMost(safeLanes.size))
        val blocked = (0 until EscapePorubaEngine.LANE_COUNT).filterNot(chosenSafe::contains)
        val kinds = if (difficulty < 0.2f) EARLY_KINDS else ALL_KINDS
        val hazards = blocked.map { lane ->
            EscapeHazard(lane, EscapePorubaEngine.PATTERN_SPAWN_Y, kinds.random(random))
        }
        return EscapePattern(chosenSafe, hazards)
    }

    private companion object {
        val EARLY_KINDS = listOf(
            EscapeHazardKind.CONTAINER,
            EscapeHazardKind.POTHOLE,
            EscapeHazardKind.PEDESTRIAN,
            EscapeHazardKind.BICYCLE,
        )
        val ALL_KINDS = EscapeHazardKind.entries.toList()
    }
}
