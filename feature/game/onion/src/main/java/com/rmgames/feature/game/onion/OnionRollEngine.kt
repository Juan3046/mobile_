package com.rmgames.feature.game.onion

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/** Phases of one run (docs/games/02-onion-roll.md). */
enum class Phase {
    RUNNING,
    FALLING,
    DONE,
}

/** A grocery riding the belt; x/y are screen-normalized centers (0..1). */
data class Fruit(val type: FruitType, val x: Float, val y: Float)

/** Belt obstacles; sizes are half-extents in screen-normalized units. */
enum class FruitType(val halfWidth: Float, val halfHeight: Float) {
    CHERRY(0.045f, 0.035f),
    APPLE(0.075f, 0.045f),
    /** Spans one full lane (lanes are a third of the belt wide). */
    BANANA(0.16f, 0.035f),
}

// Belt + character geometry, screen-normalized — shared by the engine, the screen and the tests.
internal const val LANES = 3
internal const val ONION_Y = 0.68f
internal const val ONION_HOME_X = 0.5f
internal const val ONION_HALF_W = 0.08f
internal const val ONION_HALF_H = 0.05f
internal val ONION_X_MIN = ONION_HALF_W
internal val ONION_X_MAX = 1f - ONION_HALF_W

/** Conveyor speed at t=0 in screen-heights per second; the engine ramps it over the run. */
internal const val BASE_BELT_SPEED = 0.22f

internal fun laneCenter(lane: Int): Float = (lane + 0.5f) / LANES

/** Everything [OnionRollScreen] renders for one frame. */
data class OnionRollState(
    val phase: Phase = Phase.RUNNING,
    val elapsedMs: Long = 0L,
    val scoreCentis: Long = 0L,
    val onionX: Float = ONION_HOME_X,
    val progress: Float = 0f,
    val beltSpeed: Float = BASE_BELT_SPEED,
    val fallProgress: Float = 0f,
    val fruits: List<Fruit> = emptyList(),
) {
    companion object {
        /** A run that has not started yet. */
        val INITIAL = OnionRollState()
    }
}

/**
 * Pure game rules for Onion Roll (docs/games/02-onion-roll.md) — no Android, no
 * Compose: the host loop calls [tick] with the latest gyroscope rate and [state]
 * is what the screen renders.
 *
 * **Steering** is rate-based: angular velocity maps to strafing speed through a
 * deadband and a short smoothing window, so the onion parks where the player
 * leaves it. A gyroscope cannot sense a static lean, and this model has neither
 * drift nor rubber-banding — it is the comfortable option the gyro allows.
 *
 * **The belt ramp alone ends every run**: [progress] reaches 1 at `fallAtMs`
 * (target window 80–85 s) with `hardCapMs` as a safety net. Fruits only ever
 * occupy space — entity rectangles never overlap — and never touch the timer.
 */
class OnionRollEngine(
    initialState: OnionRollState = OnionRollState.INITIAL,
    private val random: Random = Random.Default,
    private val fallAtMs: Long = DEFAULT_FALL_AT_MS,
    private val hardCapMs: Long = DEFAULT_HARD_CAP_MS,
) {
    private val initial = initialState
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<OnionRollState> = _state.asStateFlow()

    private var velocity = 0f
    private var nextSpawnAtMs = FIRST_SPAWN_AT_MS
    private var fallStartElapsedMs = 0L

    /**
     * Advances the simulation by [rawDtMs] (clamped to [MAX_TICK_MS] so a stalled
     * frame cannot tunnel the onion through a fruit).
     *
     * @param omegaZ latest gyroscope reading on the screen-normal axis (rad/s).
     * @param keySteer debug keyboard override in -1..1; null = gyro-only.
     */
    fun tick(rawDtMs: Long, omegaZ: Float, keySteer: Float?) {
        if (rawDtMs <= 0L) return
        val dtMs = rawDtMs.coerceAtMost(MAX_TICK_MS)
        val current = _state.value
        if (current.phase == Phase.DONE) return

        val elapsed = current.elapsedMs + dtMs
        val progress = ((elapsed.toFloat() / fallAtMs).pow(PROGRESS_EXPONENT)).coerceIn(0f, 1f)
        val beltSpeed = BASE_BELT_SPEED * (1f + elapsed / RAMP_TIME_MS.toFloat()).pow(BELT_EXPONENT)

        // Steering + lateral movement only while the run is live.
        val onionX = if (current.phase == Phase.RUNNING) {
            val target = targetVelocity(omegaZ, keySteer)
            velocity += (target - velocity) * (1f - exp(-dtMs / SMOOTHING_TAU_MS))
            val wanted = current.onionX + velocity * (dtMs / 1000f)
            clampAgainstFruits(current.onionX, wanted, current.fruits).coerceIn(ONION_X_MIN, ONION_X_MAX)
        } else {
            current.onionX
        }

        // Phase transitions; the score freezes at the moment of the fall.
        var phase = current.phase
        var fallProgress = current.fallProgress
        var scoreCentis = current.scoreCentis
        when (phase) {
            Phase.RUNNING -> {
                scoreCentis = elapsed / CENTIS_PER_SECOND
                if (progress >= 1f || elapsed >= hardCapMs) {
                    phase = Phase.FALLING
                    fallStartElapsedMs = elapsed
                }
            }

            Phase.FALLING -> {
                fallProgress = ((elapsed - fallStartElapsedMs) / FALL_DURATION_MS).coerceIn(0f, 1f)
                if (fallProgress >= 1f) phase = Phase.DONE
            }

            Phase.DONE -> Unit
        }

        // Belt traffic: spawns and rides while the run is live; frozen during the fall.
        val fruits = if (current.phase == Phase.RUNNING) {
            var riding = current.fruits
            if (elapsed >= nextSpawnAtMs) {
                riding = riding + spawnGroup(progress)
                nextSpawnAtMs = elapsed + spawnIntervalMs(progress)
            }
            rideFruits(riding, onionX, beltSpeed, dtMs, progress)
        } else {
            current.fruits
        }

        _state.value = OnionRollState(
            phase = phase,
            elapsedMs = elapsed,
            scoreCentis = scoreCentis,
            onionX = onionX,
            progress = progress,
            beltSpeed = beltSpeed,
            fallProgress = fallProgress,
            fruits = fruits,
        )
    }

    /** Restores the run to its initial state; a retry starts clean. */
    fun reset() {
        _state.value = initial
        velocity = 0f
        nextSpawnAtMs = FIRST_SPAWN_AT_MS
        fallStartElapsedMs = 0L
    }

    private fun targetVelocity(omegaZ: Float, keySteer: Float?): Float =
        if (keySteer != null) {
            keySteer * STEERING_MAX
        } else {
            val rate = if (abs(omegaZ) < DEADBAND_RAD_S) 0f else omegaZ * STEERING_SIGN
            (rate * STEERING_GAIN).coerceIn(-STEERING_MAX, STEERING_MAX)
        }

    /**
     * Keeps the onion out of fruit rectangles: computes the allowed x interval from
     * every alongside fruit (fruits on the right cap it, fruits on the left floor
     * it) and holds position if squeezed between two of them.
     */
    private fun clampAgainstFruits(from: Float, wanted: Float, fruits: List<Fruit>): Float {
        var ceiling = Float.MAX_VALUE
        var floor = Float.MIN_VALUE
        for (fruit in fruits) {
            if (!overlapsInY(fruit)) continue
            if (!overlapsInX(wanted, fruit)) continue
            if (from <= fruit.x) {
                ceiling = minOf(ceiling, fruit.x - fruit.type.halfWidth - ONION_HALF_W)
            } else {
                floor = maxOf(floor, fruit.x + fruit.type.halfWidth + ONION_HALF_W)
            }
        }
        if (floor > ceiling) return from
        return wanted.coerceAtMost(ceiling).coerceAtLeast(floor)
    }

    private fun overlapsInY(fruit: Fruit): Boolean =
        abs(ONION_Y - fruit.y) < ONION_HALF_H + fruit.type.halfHeight

    private fun overlapsInX(x: Float, fruit: Fruit): Boolean {
        val left = fruit.x - fruit.type.halfWidth
        val right = fruit.x + fruit.type.halfWidth
        return x + ONION_HALF_W > left && x - ONION_HALF_W < right
    }

    /** Rides fruits down the belt; one that would enter the onion's space stops against it. */
    private fun rideFruits(
        fruits: List<Fruit>,
        onionX: Float,
        beltSpeed: Float,
        dtMs: Long,
        progress: Float,
    ): List<Fruit> {
        val step = beltSpeed * (dtMs / 1000f)
        val endOfBeltY = 1f - progress * (1f - ONION_Y)
        return fruits.mapNotNull { fruit ->
            val wantedY = fruit.y + step
            val bumpsOnion = overlapsInX(onionX, fruit) &&
                abs(ONION_Y - wantedY) < ONION_HALF_H + fruit.type.halfHeight + PAUSE_GAP
            when {
                bumpsOnion -> fruit
                wantedY > endOfBeltY -> null // rode off the end of the belt
                else -> fruit.copy(y = wantedY)
            }
        }
    }

    private fun spawnGroup(progress: Float): List<Fruit> =
        chooseSpawnLanes(progress, random).map { lane ->
            Fruit(pickType(), x = laneCenter(lane), y = SPAWN_Y)
        }

    private fun spawnIntervalMs(progress: Float): Long {
        val seconds = SPAWN_INTERVAL_START_S - (SPAWN_INTERVAL_START_S - SPAWN_INTERVAL_END_S) * progress
        return (seconds * 1000f).toLong()
    }

    private fun pickType(): FruitType {
        val roll = random.nextFloat()
        return when {
            roll < CHERRY_WEIGHT -> FruitType.CHERRY
            roll < CHERRY_WEIGHT + APPLE_WEIGHT -> FruitType.APPLE
            else -> FruitType.BANANA
        }
    }

    companion object {
        private const val DEFAULT_FALL_AT_MS = 82_000L // centre of the 80–85 s window (docs/games/02)
        private const val DEFAULT_HARD_CAP_MS = 90_000L // global ceiling (docs/02-gameplay-rules)
        private const val MAX_TICK_MS = 50L // anti-tunneling guard for stalled frames
        private const val PROGRESS_EXPONENT = 1.6f // slow start, accelerating approach to the end
        private const val RAMP_TIME_MS = 45_000f
        private const val BELT_EXPONENT = 1.3f
        private const val STEERING_SIGN = -1f // ω_z > 0 = counter-clockwise = lean left → negative velocity
        private const val STEERING_GAIN = 0.55f // (screen-widths/s) per rad/s
        private const val STEERING_MAX = 1.1f // comfort cap: crosses the belt in ~1 s
        private const val DEADBAND_RAD_S = 0.05f // swallows typical sensor bias
        private const val SMOOTHING_TAU_MS = 60f // short glide, then the onion parks
        private const val PAUSE_GAP = 0.01f // a fruit stops just short of touching
        private const val FIRST_SPAWN_AT_MS = 600L
        private const val SPAWN_INTERVAL_START_S = 1.2f
        private const val SPAWN_INTERVAL_END_S = 0.4f
        private const val SPAWN_Y = -0.1f
        private const val CENTIS_PER_SECOND = 10L
        private const val FALL_DURATION_MS = 700f
        private const val SINGLE_FRUIT_UNTIL_PROGRESS = 0.45f
        private const val MAX_GROUP_SIZE = 2
        private const val CHERRY_WEIGHT = 0.45f
        private const val APPLE_WEIGHT = 0.35f

        /**
         * Lanes for one spawn group: one fruit early, up to two later — never a
         * full wall, so one lane always stays open.
         */
        internal fun chooseSpawnLanes(progress: Float, random: Random): List<Int> {
            val groupSize = if (progress < SINGLE_FRUIT_UNTIL_PROGRESS) {
                1
            } else {
                random.nextInt(1, MAX_GROUP_SIZE + 1)
            }
            return (0 until LANES).shuffled(random).take(groupSize)
        }
    }
}
