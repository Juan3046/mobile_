package com.rmgames.feature.game.blink

import kotlin.math.hypot
import kotlin.random.Random

/** Horizontal position of the fixed crosshair, normalized to the gallery box. */
const val CROSSHAIR_X = 0.5

/** Vertical position of the fixed crosshair, normalized to the gallery box. */
const val CROSSHAIR_Y = 0.5

/** Visual kind of a target on the gallery. */
enum class TargetKind { STANDARD, FAST_SMALL, BOMB }

/** What one blink did. [IGNORED] means no shot was taken (cooldown or run over). */
enum class ShotOutcome { BULLSEYE, OUTER, MISS, BOMB, IGNORED }

/** Why the run ended. */
enum class EndReason { TIME_UP, BOMB }

/** Target as the UI renders it: normalized to the gallery box (0..1 on both axes). */
data class TargetUi(
    val id: Int,
    val x: Double,
    val y: Double,
    val kind: TargetKind,
    val outerRadius: Double,
    val bullseyeRadius: Double,
)

/** Immutable snapshot of one run, consumed by the Compose screen. */
data class BlinkSniperUiState(
    val score: Int = 0,
    val elapsedMs: Long = 0,
    val remainingMs: Long = 60_000,
    val targets: List<TargetUi> = emptyList(),
    val eyesOpen: Boolean = true,
    val shotsFired: Int = 0,
    val finished: Boolean = false,
    val endReason: EndReason? = null,
)

/**
 * Tunables for one run. Defaults follow docs/games/03-blink-sniper.md:
 * a fixed 60s timer, three difficulty phases (0-20 / 20-40 / 40-60s) and a
 * crosshair fixed at the centre of the gallery.
 *
 * Only [centerLane] can be scored: the crosshair does not move, so targets on the
 * other lanes are decoys (decided — see the game doc).
 */
data class BlinkSniperConfig(
    val durationMs: Long = 60_000,
    /** Minimum gap between two shots; blocks one blink from firing twice. */
    val cooldownMs: Long = 300,
    /** Vertical centre of every lane, normalized to the gallery box. */
    val lanesY: List<Double> = listOf(0.14, 0.32, 0.50, 0.68, 0.86),
    /** Lane that sits exactly under the crosshair — the only hittable one. */
    val centerLane: Int = 2,
    /** Share of spawns placed on the crosshair lane; the rest are decoys. */
    val centerLaneChance: Double = 0.5,
    val standardOuterRadius: Double = 0.070,
    val standardBullseyeRadius: Double = 0.030,
    /** Small targets are both smaller and harder to time. */
    val smallOuterRadius: Double = 0.050,
    val smallBullseyeRadius: Double = 0.020,
    /** Horizontal speed in gallery-widths per second, at the start/end of the run. */
    val startSpeed: Double = 0.28,
    val endSpeed: Double = 0.55,
    val smallSpeedMultiplier: Double = 1.35,
    /** Per-target speed spread so runs are not metronomic. */
    val speedJitterMin: Double = 0.85,
    val speedJitterMax: Double = 1.15,
    /** Gap between spawns, in ms, at the start/end of the run. */
    val startSpawnIntervalMs: Long = 1_400,
    val endSpawnIntervalMs: Long = 520,
    val smallChance: Double = 0.25,
    /** Bomb share per phase: warmup / intermediate / high intensity. */
    val bombChanceWarmup: Double = 0.0,
    val bombChanceIntermediate: Double = 0.12,
    val bombChanceHigh: Double = 0.30,
    /** Spawn point, just off the left edge. */
    val startX: Double = -0.12,
    /** Point past the right edge where a target is dropped. */
    val despawnX: Double = 1.12,
    /** Safety cap so a big clock jump cannot flood the gallery. */
    val maxSimultaneous: Int = 14,
)

/** One target while it is alive. Speed is fixed at spawn; position is derived. */
private data class LiveTarget(
    val id: Int,
    val lane: Int,
    val kind: TargetKind,
    val spawnedAtMs: Long,
    val startX: Double,
    val speed: Double,
    val outerRadius: Double,
    val bullseyeRadius: Double,
)

/**
 * Pure run logic for Blink Sniper (Strategy): spawning, movement, hit resolution
 * and the timer. Free of Android, Compose and coroutines, so it runs on plain JUnit
 * with an injected clock ([tick]/[shoot] take the elapsed ms explicitly).
 *
 * Rules: docs/games/03-blink-sniper.md — +2 on the bullseye, +1 on the outer ring,
 * a bomb ends the run instantly keeping the score, and the run is a win when the
 * 60s timer expires.
 */
class BlinkSniperEngine(
    private val config: BlinkSniperConfig = BlinkSniperConfig(),
    private val random: Random = Random.Default,
) {

    init {
        require(config.centerLane in config.lanesY.indices) {
            "centerLane ${config.centerLane} is outside ${config.lanesY.size} lanes"
        }
        require(config.lanesY[config.centerLane] == CROSSHAIR_Y) {
            "centerLane must sit on the crosshair (y=$CROSSHAIR_Y)"
        }
    }

    var elapsedMs: Long = 0L
        private set
    var score: Int = 0
        private set
    var shotsFired: Int = 0
        private set
    var spawnedCount: Int = 0
        private set
    var isFinished: Boolean = false
        private set
    var endReason: EndReason? = null
        private set

    val durationMs: Long get() = config.durationMs

    private var targets: List<LiveTarget> = emptyList()
    private var nextId = 0
    private var nextSpawnAtMs = 0L
    private var lastShotAtMs: Long? = null

    /** Targets as the UI should draw them right now. */
    fun snapshot(): List<TargetUi> = targets.map { target ->
        TargetUi(
            id = target.id,
            x = xOf(target, elapsedMs),
            y = config.lanesY[target.lane],
            kind = target.kind,
            outerRadius = target.outerRadius,
            bullseyeRadius = target.bullseyeRadius,
        )
    }

    /**
     * Advances the world to [nowMs]: spawns on schedule, moves targets, drops the
     * ones that left the screen and ends the run when the timer expires.
     * Ignored once the run is over.
     */
    fun tick(nowMs: Long) {
        if (isFinished) return
        if (nowMs > elapsedMs) elapsedMs = nowMs
        if (elapsedMs >= config.durationMs) {
            end(EndReason.TIME_UP)
            return
        }

        while (elapsedMs >= nextSpawnAtMs) {
            if (targets.size < config.maxSimultaneous) spawn(nextSpawnAtMs)
            else nextSpawnAtMs += spawnIntervalMs(nextSpawnAtMs)
        }
        targets = targets.filter { xOf(it, elapsedMs) <= config.despawnX }
    }

    /**
     * Resolves one blink at [nowMs]. Returns [ShotOutcome.IGNORED] when the run is
     * over or while the firing cooldown is still running.
     */
    fun shoot(nowMs: Long): ShotOutcome {
        if (isFinished) return ShotOutcome.IGNORED
        val lastShot = lastShotAtMs
        if (lastShot != null && nowMs - lastShot < config.cooldownMs) return ShotOutcome.IGNORED
        lastShotAtMs = nowMs
        shotsFired++

        val candidates = targets.mapNotNull { target ->
            val distance = hypot(
                xOf(target, nowMs) - CROSSHAIR_X,
                config.lanesY[target.lane] - CROSSHAIR_Y,
            )
            if (distance <= target.outerRadius) target to distance else null
        }
        if (candidates.isEmpty()) return ShotOutcome.MISS

        // Nearest target wins; a dead heat goes to the bomb (harsher outcome).
        val (hit, distance) = candidates.minWith(
            compareBy<Pair<LiveTarget, Double>>(
                { it.second },
                { if (it.first.kind == TargetKind.BOMB) 0 else 1 },
            ),
        )
        targets = targets.filterNot { it.id == hit.id }

        if (hit.kind == TargetKind.BOMB) {
            end(EndReason.BOMB)
            return ShotOutcome.BOMB
        }
        return if (distance <= hit.bullseyeRadius) {
            score += 2
            ShotOutcome.BULLSEYE
        } else {
            score += 1
            ShotOutcome.OUTER
        }
    }

    private fun end(reason: EndReason) {
        isFinished = true
        endReason = reason
    }

    private fun spawn(atMs: Long) {
        val progress = atMs.toDouble() / config.durationMs
        val kind = pickKind(progress)
        val speed = lerp(config.startSpeed, config.endSpeed, progress) * jitter() *
            if (kind == TargetKind.FAST_SMALL) config.smallSpeedMultiplier else 1.0
        val small = kind == TargetKind.FAST_SMALL

        targets = targets + LiveTarget(
            id = nextId++,
            lane = pickLane(),
            kind = kind,
            spawnedAtMs = atMs,
            startX = config.startX,
            speed = speed,
            outerRadius = if (small) config.smallOuterRadius else config.standardOuterRadius,
            bullseyeRadius = if (small) config.smallBullseyeRadius else config.standardBullseyeRadius,
        )
        spawnedCount++
        nextSpawnAtMs = atMs + spawnIntervalMs(atMs)
    }

    /** Phase 1 (0-20s) never spawns a bomb; the two later phases ramp up. */
    private fun pickKind(progress: Double): TargetKind {
        val bombChance = when {
            progress < 1.0 / 3.0 -> config.bombChanceWarmup
            progress < 2.0 / 3.0 -> config.bombChanceIntermediate
            else -> config.bombChanceHigh
        }
        val roll = random.nextDouble()
        return when {
            roll < bombChance -> TargetKind.BOMB
            roll < bombChance + config.smallChance -> TargetKind.FAST_SMALL
            else -> TargetKind.STANDARD
        }
    }

    private fun pickLane(): Int {
        if (random.nextDouble() < config.centerLaneChance) return config.centerLane
        val decoys = config.lanesY.indices.filter { it != config.centerLane }
        return decoys[random.nextInt(decoys.size)]
    }

    private fun jitter(): Double =
        config.speedJitterMin + random.nextDouble() * (config.speedJitterMax - config.speedJitterMin)

    private fun spawnIntervalMs(nowMs: Long): Long {
        val progress = (nowMs.toDouble() / config.durationMs).coerceIn(0.0, 1.0)
        return (config.startSpawnIntervalMs +
            (config.endSpawnIntervalMs - config.startSpawnIntervalMs) * progress).toLong()
    }

    private fun xOf(target: LiveTarget, nowMs: Long): Double =
        target.startX + target.speed * (nowMs - target.spawnedAtMs) / 1000.0

    private fun lerp(start: Double, end: Double, t: Double): Double =
        start + (end - start) * t.coerceIn(0.0, 1.0)
}
