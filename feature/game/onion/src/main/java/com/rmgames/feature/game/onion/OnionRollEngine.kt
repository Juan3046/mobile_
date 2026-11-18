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

/**
 * Belt obstacles; sizes are the drawn half-extents in screen-normalized units
 * (tuned to look round on a typical ~1:1.9 play area). Collisions use the
 * smaller [hitHalfWidth]/[hitHalfHeight].
 */
enum class FruitType(val halfWidth: Float, val halfHeight: Float) {
    CHERRY(0.045f, 0.025f),
    APPLE(0.075f, 0.042f),
    /** Spans one full lane (lanes are a third of the belt wide). */
    BANANA(0.16f, 0.035f),
}

// Belt + character geometry, screen-normalized — shared by the engine, the screen and the tests.
internal const val LANES = 3
internal const val ONION_Y = 0.68f
internal const val ONION_HOME_X = 0.5f
internal const val ONION_HALF_W = 0.08f
internal const val ONION_HALF_H = 0.045f
internal val ONION_X_MIN = ONION_HALF_W
internal val ONION_X_MAX = 1f - ONION_HALF_W

/** Hitboxes are this share of the drawn size on each axis, so grazing sprites don't collide. */
internal const val HITBOX_SCALE = 0.8f
internal const val ONION_HIT_HALF_W = ONION_HALF_W * HITBOX_SCALE
internal const val ONION_HIT_HALF_H = ONION_HALF_H * HITBOX_SCALE
internal val FruitType.hitHalfWidth: Float get() = halfWidth * HITBOX_SCALE
internal val FruitType.hitHalfHeight: Float get() = halfHeight * HITBOX_SCALE

/** Conveyor speed at t=0 in screen-heights per second; the engine ramps it over the run. */
internal const val BASE_BELT_SPEED = 0.429f // 0.22 → 0.286 (+30 %) → 0.429 (+50 %) after playtests (docs/games/02)

internal fun laneCenter(lane: Int): Float = (lane + 0.5f) / LANES

/** Fixed end roller of the endless belt: an onion shoved past it falls. */
internal const val END_OF_BELT_Y = 0.95f

/** Everything [OnionRollScreen] renders for one frame. */
data class OnionRollState(
    val phase: Phase = Phase.RUNNING,
    val elapsedMs: Long = 0L,
    /** Whole seconds survived; frozen at the moment of the fall. */
    val scoreSeconds: Long = 0L,
    val onionX: Float = ONION_HOME_X,
    /** Onion centre on the belt; [ONION_Y] at home, larger after knockback (closer to the end). */
    val onionY: Float = ONION_Y,
    /** Traffic ramp 0..1 (spawn rate and group size); saturates at 82 s. */
    val intensity: Float = 0f,
    val beltSpeed: Float = BASE_BELT_SPEED,
    val fallProgress: Float = 0f,
    /** Time since the fall started (drives the "YOU LOST!" banner); 0 while running. */
    val fallElapsedMs: Long = 0L,
    val fruits: List<Fruit> = emptyList(),
    /** Distinct fruit impacts this run (a new contact after a tick without one); drives hit sounds. */
    val hits: Int = 0,
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
 * **The belt is endless**: the run falls only when fruits shove the onion past
 * the fixed end roller ([END_OF_BELT_Y]); the ever-faster belt makes that
 * inevitable. Fruits are solid (hitboxes never overlap): sideways they block,
 * head-on they shove the onion back. Once clear, the onion walks back home.
 * [timeLimitMs] (default null = endless) exists only for deterministic tests.
 */
class OnionRollEngine(
    initialState: OnionRollState = OnionRollState.INITIAL,
    private val random: Random = Random.Default,
    private val timeLimitMs: Long? = null,
    /** Tests switch traffic off to check timing without knockback. */
    private val spawnFruits: Boolean = true,
) {
    private val initial = initialState
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<OnionRollState> = _state.asStateFlow()

    private var velocity = 0f
    private var knockVelocity = 0f
    private var nextSpawnAtMs = FIRST_SPAWN_AT_MS
    private var fallStartElapsedMs = 0L
    private var inContact = false

    /**
     * Advances the simulation by [rawDtMs] (clamped to [MAX_TICK_MS] so a stalled
     * frame cannot tunnel the onion through a fruit).
     *
     * @param tiltRate latest left/right tilt rate in rad/s, positive = lean left
     *   (see [tiltRate] in OnionRoll.kt for how gyro axes map to it).
     * @param keySteer debug keyboard override in -1..1; null = gyro-only.
     */
    fun tick(rawDtMs: Long, tiltRate: Float, keySteer: Float?) {
        if (rawDtMs <= 0L) return
        val dtMs = rawDtMs.coerceAtMost(MAX_TICK_MS)
        val current = _state.value
        if (current.phase == Phase.DONE) return

        val elapsed = current.elapsedMs + dtMs
        val intensity = ((elapsed / INTENSITY_FULL_MS).pow(INTENSITY_EXPONENT)).coerceIn(0f, 1f)
        val beltSpeed = BASE_BELT_SPEED * (1f + elapsed / RAMP_TIME_MS.toFloat()).pow(BELT_EXPONENT)

        val live = current.phase == Phase.RUNNING
        val dtS = dtMs / 1000f

        // Steering + lateral movement only while the run is live.
        val onionX = if (live) {
            val target = targetVelocity(tiltRate, keySteer)
            velocity += (target - velocity) * (1f - exp(-dtMs / SMOOTHING_TAU_MS))
            val wanted = current.onionX + velocity * dtS
            clampAgainstFruits(current.onionX, wanted, current.onionY, current.fruits)
                .coerceIn(ONION_X_MIN, ONION_X_MAX)
        } else {
            current.onionX
        }

        // Knockback glide + walking back home, never into a fruit ahead of the onion.
        var onionY = current.onionY
        var hitThisTick = false
        if (live) {
            knockVelocity *= exp(-dtMs / KNOCK_TAU_MS)
            val moved = (onionY + (knockVelocity - RECOVERY_SPEED) * dtS).coerceAtLeast(ONION_Y)
            onionY = maxOf(moved, minOf(current.onionY, recoveryLimit(onionX, current.fruits)))
        }

        // Belt traffic: spawns and rides while the run is live; head-on hits shove the onion back.
        // Frozen during the fall.
        val fruits = if (live) {
            var riding = current.fruits
            if (spawnFruits && elapsed >= nextSpawnAtMs) {
                riding = riding + spawnGroup(intensity)
                nextSpawnAtMs = elapsed + spawnIntervalMs(intensity)
            }
            val step = beltSpeed * dtS
            riding.mapNotNull { fruit ->
                val y = fruit.y + step
                val hit = overlapsInX(onionX, fruit) && fruit.y <= onionY &&
                    abs(onionY - y) < ONION_HIT_HALF_H + fruit.type.hitHalfHeight
                if (hit) {
                    hitThisTick = true
                    onionY = y + fruit.type.hitHalfHeight + ONION_HIT_HALF_H + CONTACT_GAP
                    knockVelocity = maxOf(knockVelocity, beltSpeed + KNOCK_EXTRA_SPEED)
                }
                if (y > END_OF_BELT_Y && !hit) null else fruit.copy(y = y) // rode off the end
            }
        } else {
            current.fruits
        }

        // A hit counts once per contact: consecutive ticks of pushing are one impact.
        val hits = if (hitThisTick && !inContact) current.hits + 1 else current.hits
        inContact = hitThisTick

        // Phase transitions; the score freezes at the moment of the fall.
        var phase = current.phase
        var fallProgress = current.fallProgress
        var fallElapsedMs = current.fallElapsedMs
        var scoreSeconds = current.scoreSeconds
        when (phase) {
            Phase.RUNNING -> {
                scoreSeconds = elapsed / MS_PER_SECOND
                val timeUp = timeLimitMs != null && elapsed >= timeLimitMs
                if (onionY >= END_OF_BELT_Y || timeUp) {
                    phase = Phase.FALLING
                    fallStartElapsedMs = elapsed
                }
            }

            Phase.FALLING -> {
                fallElapsedMs = elapsed - fallStartElapsedMs
                fallProgress = (fallElapsedMs / FALL_DURATION_MS).coerceIn(0f, 1f)
                // The "YOU LOST!" banner stays up a moment after the fall animation.
                if (fallElapsedMs >= FALL_DURATION_MS + LOSS_HOLD_MS) phase = Phase.DONE
            }

            Phase.DONE -> Unit
        }

        _state.value = OnionRollState(
            phase = phase,
            elapsedMs = elapsed,
            scoreSeconds = scoreSeconds,
            onionX = onionX,
            onionY = onionY,
            intensity = intensity,
            beltSpeed = beltSpeed,
            fallProgress = fallProgress,
            fallElapsedMs = fallElapsedMs,
            fruits = fruits,
            hits = hits,
        )
    }

    /** Restores the run to its initial state; a retry starts clean. */
    fun reset() {
        _state.value = initial
        velocity = 0f
        knockVelocity = 0f
        nextSpawnAtMs = FIRST_SPAWN_AT_MS
        fallStartElapsedMs = 0L
        inContact = false
    }

    private fun targetVelocity(tiltRate: Float, keySteer: Float?): Float =
        if (keySteer != null) {
            keySteer * STEERING_MAX
        } else {
            val rate = if (abs(tiltRate) < DEADBAND_RAD_S) 0f else tiltRate * STEERING_SIGN
            (rate * STEERING_GAIN).coerceIn(-STEERING_MAX, STEERING_MAX)
        }

    /**
     * Keeps the onion out of fruit rectangles: computes the allowed x interval from
     * every alongside fruit (fruits on the right cap it, fruits on the left floor
     * it) and holds position if squeezed between two of them.
     */
    private fun clampAgainstFruits(from: Float, wanted: Float, onionY: Float, fruits: List<Fruit>): Float {
        var ceiling = Float.MAX_VALUE
        var floor = -Float.MAX_VALUE
        for (fruit in fruits) {
            if (!overlapsInY(onionY, fruit)) continue
            if (!overlapsInX(wanted, fruit)) continue
            if (from <= fruit.x) {
                ceiling = minOf(ceiling, fruit.x - fruit.type.hitHalfWidth - ONION_HIT_HALF_W)
            } else {
                floor = maxOf(floor, fruit.x + fruit.type.hitHalfWidth + ONION_HIT_HALF_W)
            }
        }
        if (floor > ceiling) return from
        return wanted.coerceAtMost(ceiling).coerceAtLeast(floor)
    }

    private fun overlapsInY(onionY: Float, fruit: Fruit): Boolean =
        abs(onionY - fruit.y) < ONION_HIT_HALF_H + fruit.type.hitHalfHeight

    /** Highest the onion may walk back to: just behind any fruit in its column. */
    private fun recoveryLimit(onionX: Float, fruits: List<Fruit>): Float {
        var limit = ONION_Y
        for (fruit in fruits) {
            if (!overlapsInX(onionX, fruit)) continue
            limit = maxOf(limit, fruit.y + fruit.type.hitHalfHeight + ONION_HIT_HALF_H + CONTACT_GAP)
        }
        return limit
    }

    private fun overlapsInX(x: Float, fruit: Fruit): Boolean {
        val left = fruit.x - fruit.type.hitHalfWidth
        val right = fruit.x + fruit.type.hitHalfWidth
        return x + ONION_HIT_HALF_W > left && x - ONION_HIT_HALF_W < right
    }

    private fun spawnGroup(intensity: Float): List<Fruit> =
        chooseSpawnLanes(intensity, random).map { lane ->
            Fruit(pickType(), x = laneCenter(lane), y = SPAWN_Y)
        }

    private fun spawnIntervalMs(intensity: Float): Long {
        val seconds = SPAWN_INTERVAL_START_S - (SPAWN_INTERVAL_START_S - SPAWN_INTERVAL_END_S) * intensity
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
        private const val MAX_TICK_MS = 50L // anti-tunneling guard for stalled frames
        private const val INTENSITY_FULL_MS = 82_000f // traffic reaches full density here
        private const val INTENSITY_EXPONENT = 1.6f // slow start, accelerating ramp
        private const val RAMP_TIME_MS = 45_000f / 1.1f // 10 % faster acceleration after playtest (≈ 40.9 s)
        private const val BELT_EXPONENT = 1.3f
        private const val STEERING_SIGN = -1f // tiltRate > 0 = lean left → negative velocity
        private const val STEERING_GAIN = 1.4f // (screen-widths/s) per rad/s: ~15 deg of tilt = one lane
        private const val STEERING_MAX = 2.5f // comfort cap: crosses the belt in ~0.35 s
        private const val DEADBAND_RAD_S = 0.05f // swallows typical sensor bias
        private const val SMOOTHING_TAU_MS = 60f // short glide, then the onion parks
        private const val CONTACT_GAP = 0.01f // a shoved onion sits just clear of the fruit
        private const val KNOCK_EXTRA_SPEED = 0.6f // backward kick above belt speed, screen-heights/s
        private const val KNOCK_TAU_MS = 80f // the kick fades in ~80 ms
        private const val RECOVERY_SPEED = 0.04f // walk back home, screen-heights/s
        private const val FIRST_SPAWN_AT_MS = 600L
        private const val SPAWN_INTERVAL_START_S = 1.2f
        private const val SPAWN_INTERVAL_END_S = 0.4f
        private const val SPAWN_Y = -0.1f
        private const val MS_PER_SECOND = 1_000L
        private const val FALL_DURATION_MS = 700f
        private const val LOSS_HOLD_MS = 1_300f // banner time after the fall: 2 s in total
        private const val SINGLE_FRUIT_UNTIL_INTENSITY = 0.45f
        private const val MAX_GROUP_SIZE = 2
        private const val CHERRY_WEIGHT = 0.45f
        private const val APPLE_WEIGHT = 0.35f

        /**
         * Lanes for one spawn group: one fruit early, up to two later — never a
         * full wall, so one lane always stays open.
         */
        internal fun chooseSpawnLanes(intensity: Float, random: Random): List<Int> {
            val groupSize = if (intensity < SINGLE_FRUIT_UNTIL_INTENSITY) {
                1
            } else {
                random.nextInt(1, MAX_GROUP_SIZE + 1)
            }
            return (0 until LANES).shuffled(random).take(groupSize)
        }
    }
}
