package com.rmgames.feature.game.sound

import kotlin.random.Random

/** Why the run ended. [LEFT_BEHIND] = a barrier finished passing outside its gap. */
enum class EndReason { TIME_UP, LEFT_BEHIND }

/** One barrier as the UI draws it, normalized to the play box (0..1 on both axes). */
data class BarrierUi(
    val id: Int,
    /** Left edge of the bar pair; greater than 1.0 while still off-screen right. */
    val x: Double,
    /** Horizontal width of each bar, in screen widths. */
    val width: Double,
    /** Vertical centre of the gap: 0 = floor, 1 = ceiling. */
    val gapCenter: Double,
    /** Half-height of the gap, so it spans `gapCenter ± gapHalf`. */
    val gapHalf: Double,
)

/** Immutable snapshot of one run, consumed by the Compose screen. */
data class SoundBarrierUiState(
    val score: Int = 0,
    val elapsedMs: Long = 0,
    val remainingMs: Long = 60_000,
    /** Ball centre height: 0 = floor, 1 = ceiling. */
    val ballY: Double = 0.5,
    /** Fixed horizontal position of the ball, 0..1 (mirrors the config). */
    val ballX: Double = 0.25,
    /** Ball radius, 0..1 (mirrors the config) so the screen can scale it. */
    val ballRadius: Double = 0.030,
    /** Latest microphone level, 0..1 (before the loud threshold is applied). */
    val soundLevel: Float = 0f,
    /** True while the input counts as sound (mic over threshold or the debug key). */
    val soundDetected: Boolean = false,
    val barriers: List<BarrierUi> = emptyList(),
    val finished: Boolean = false,
    val endReason: EndReason? = null,
)

/**
 * Tunables for one run. Defaults follow docs/games/06-sound-barrier.md:
 * a fixed 60s timer, the track advancing on its own, sound thrusting the ball up
 * and silence letting gravity pull it down, and gaps that tighten over the run.
 *
 * Everything is normalized to the play box (0..1 on both axes) so the physics does
 * not depend on the screen size.
 */
data class SoundBarrierConfig(
    val durationMs: Long = 60_000,
    /** Fixed horizontal position of the ball: 0 = left edge, 1 = right edge. */
    val playerX: Double = 0.25,
    /** Ball radius, also its floor/ceiling clearance. */
    val playerRadius: Double = 0.030,
    /** Downward acceleration while silent, in screen-heights / s². */
    val gravity: Double = 2.2,
    /** Upward acceleration while sound is detected, in screen-heights / s². */
    val thrust: Double = 3.4,
    /** Terminal speeds, in screen-heights per second. */
    val maxFallSpeed: Double = 1.6,
    val maxRiseSpeed: Double = 1.4,
    /** Track scroll speed at the start / end of the run, in widths per second. */
    val startSpeed: Double = 0.34,
    val endSpeed: Double = 0.62,
    /** Distance between two barriers along the track, in widths. */
    val spacing: Double = 0.55,
    /** Horizontal width of each bar, in widths. */
    val barWidth: Double = 0.10,
    /** Where a barrier is born, just off the right edge. */
    val spawnX: Double = 1.15,
    /** Half of the gap height at the start / end of the run. */
    val startGapHalf: Double = 0.17,
    val endGapHalf: Double = 0.09,
    /** Vertical band a gap centre is drawn from. */
    val gapCenterMin: Double = 0.22,
    val gapCenterMax: Double = 0.78,
    /** Microphone level at or above which the input counts as loud (0..1). */
    val loudThreshold: Float = 0.05f,
)

/** One barrier while it is alive. Its position is derived from the track scroll. */
private data class LiveBarrier(
    val id: Int,
    val spawnWorldX: Double,
    val gapCenter: Double,
    val gapHalf: Double,
)

/**
 * Pure run logic for Sound Barrier (Strategy): track scroll, ball physics,
 * gap placement and pass resolution. Free of Android, Compose and coroutines, so
 * it runs on plain JUnit with an injected clock ([tick] takes the elapsed ms and
 * the current sound state explicitly).
 *
 * Rules: docs/games/06-sound-barrier.md — +1 when the ball is inside the gap at
 * the instant a barrier finishes passing, Game Over keeping the score otherwise,
 * contact during the sweep is free, and the run is a win when the 60s timer expires.
 */
class SoundBarrierEngine(
    private val config: SoundBarrierConfig = SoundBarrierConfig(),
    private val random: Random = Random.Default,
) {

    var elapsedMs: Long = 0L
        private set
    var score: Int = 0
        private set
    /** Ball centre height, 0 = floor, 1 = ceiling. */
    var ballY: Double = 0.5
        private set
    /** Ball vertical speed in screen-heights per second; positive is up. */
    var ballVelocity: Double = 0.0
        private set
    var spawnedCount: Int = 0
        private set
    var isFinished: Boolean = false
        private set
    var endReason: EndReason? = null
        private set

    val durationMs: Long get() = config.durationMs

    private var barriers: List<LiveBarrier> = emptyList()
    private var nextId = 0
    private var lastSpawnWorldX: Double? = null
    private var lastStepMs = 0L

    init {
        require(config.spacing > 0.0) { "spacing must be positive" }
        require(config.durationMs > 0) { "durationMs must be positive" }
    }

    /** Barriers as the UI should draw them right now. */
    fun snapshot(): List<BarrierUi> {
        val worldX = worldXAt(elapsedMs)
        return barriers.map { barrier ->
            BarrierUi(
                id = barrier.id,
                x = xOf(barrier, worldX),
                width = config.barWidth,
                gapCenter = barrier.gapCenter,
                gapHalf = barrier.gapHalf,
            )
        }
    }

    /**
     * Advances the world to [nowMs]: moves the ball, scrolls the track, spawns and
     * resolves barriers, and ends the run when the timer expires or a barrier
     * finishes passing outside its gap. Ignored once the run is over.
     *
     * @param soundDetected true while the microphone counts as loud (or the debug
     * key is held); it is the only thing that pushes the ball up.
     */
    fun tick(nowMs: Long, soundDetected: Boolean) {
        if (isFinished) return
        if (nowMs > elapsedMs) elapsedMs = nowMs
        if (elapsedMs >= config.durationMs) {
            end(EndReason.TIME_UP)
            return
        }

        stepBall(elapsedMs, soundDetected)
        spawnBarriers()
        resolvePassedBarriers()
    }

    /** Integrates one vertical step. The floor and the ceiling clamp the ball. */
    private fun stepBall(nowMs: Long, soundDetected: Boolean) {
        val dt = (nowMs - lastStepMs) / 1000.0
        lastStepMs = nowMs
        if (dt <= 0.0) return

        val acceleration = if (soundDetected) config.thrust else -config.gravity
        ballVelocity = (ballVelocity + acceleration * dt)
            .coerceIn(-config.maxFallSpeed, config.maxRiseSpeed)
        val nextY = ballY + ballVelocity * dt
        val floor = config.playerRadius
        val ceiling = 1.0 - config.playerRadius
        ballY = nextY.coerceIn(floor, ceiling)
        if (ballY == floor || ballY == ceiling) ballVelocity = 0.0
    }

    /** Keeps exactly one barrier per [SoundBarrierConfig.spacing] of track. */
    private fun spawnBarriers() {
        val worldX = worldXAt(elapsedMs)
        val gate = lastSpawnWorldX
        if (gate == null) {
            spawn(atMs = elapsedMs, worldX = worldX)
            lastSpawnWorldX = worldX
            return
        }

        var next = gate
        while (worldX - next >= config.spacing) {
            next += config.spacing
            spawn(atMs = elapsedMs, worldX = next)
        }
        lastSpawnWorldX = next
    }

    private fun spawn(atMs: Long, worldX: Double) {
        val progress = (atMs.toDouble() / config.durationMs).coerceIn(0.0, 1.0)
        val gapHalf = config.startGapHalf +
            (config.endGapHalf - config.startGapHalf) * progress
        val requested = config.gapCenterMin +
            random.nextDouble() * (config.gapCenterMax - config.gapCenterMin)
        // Keep the whole gap on screen even if a very large gap was configured.
        val gapCenter = requested.coerceIn(
            gapHalf.coerceAtMost(0.5),
            (1.0 - gapHalf).coerceAtLeast(0.5),
        )

        barriers = barriers + LiveBarrier(
            id = nextId++,
            spawnWorldX = worldX,
            gapCenter = gapCenter,
            gapHalf = gapHalf,
        )
        spawnedCount++
    }

    /**
     * Resolves every barrier whose trailing edge has just crossed the ball:
     * inside the gap scores a point, outside it ends the run. Contact while the
     * barrier is still sweeping across the ball does nothing on its own.
     */
    private fun resolvePassedBarriers() {
        val worldX = worldXAt(elapsedMs)
        var index = 0
        while (index < barriers.size) {
            val barrier = barriers[index]
            if (xOf(barrier, worldX) + config.barWidth > config.playerX) break

            val insideGap = ballY >= barrier.gapCenter - barrier.gapHalf &&
                ballY <= barrier.gapCenter + barrier.gapHalf
            if (!insideGap) {
                end(EndReason.LEFT_BEHIND)
                return
            }
            score++
            index++
        }
        if (index > 0) barriers = barriers.drop(index)
    }

    private fun xOf(barrier: LiveBarrier, worldX: Double): Double =
        config.spawnX - (worldX - barrier.spawnWorldX)

    /**
     * Distance the track has scrolled, in widths: the closed-form integral of the
     * linear speed ramp, so a test can jump the clock without stepping through it
     * and so every barrier moves together (none can overtake another).
     */
    private fun worldXAt(nowMs: Long): Double {
        val t = nowMs.coerceIn(0L, config.durationMs).toDouble() / 1000.0
        val durationSeconds = config.durationMs / 1000.0
        val ramp = (config.endSpeed - config.startSpeed) * t * t / (2.0 * durationSeconds)
        return config.startSpeed * t + ramp
    }

    private fun end(reason: EndReason) {
        isFinished = true
        endReason = reason
    }
}
