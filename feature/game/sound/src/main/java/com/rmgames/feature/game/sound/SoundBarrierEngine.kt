package com.rmgames.feature.game.sound

import kotlin.random.Random

/** Why the run ended. [LEFT_BEHIND] = a bar pushed the ball off the screen. */
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
    /**
     * Horizontal position of the ball, 0..1: the centre of the screen until a bar
     * touches it and carries it backwards (left).
     */
    val ballX: Double = 0.5,
    /** Ball half-size on each normalized axis, 0..1 (mirrors the config). */
    val ballRadius: Double = 0.015,
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
    /**
     * Starting horizontal position of the ball: the centre of the screen. A bar
     * that touches it pushes it backwards (left) from here.
     */
    val playerX: Double = 0.5,
    /**
     * Half-size of the ball's hitbox on each normalized axis (widths on X, heights
     * on Y), which is also its floor/ceiling clearance. The screen draws the ball
     * as a circle of `playerRadius × box height`, so the hitbox sits inside what
     * the player sees: vertical contact is exact and horizontal contact is
     * never wider than the drawn ball.
     */
    val playerRadius: Double = 0.015,
    /**
     * Share of the vertical speed the ball keeps when it rebounds off the floor or
     * the ceiling: 1.0 = a perfect bounce, 0.0 = it stops dead on contact.
     */
    val bounceRestitution: Double = 0.8,
    /** Downward acceleration while silent, in screen-heights / s². */
    val gravity: Double = 2.2,
    /** Upward acceleration while sound is detected, in screen-heights / s². */
    val thrust: Double = 3.4,
    /** Terminal speeds, in screen-heights per second. */
    val maxFallSpeed: Double = 1.6,
    val maxRiseSpeed: Double = 1.4,
    /**
     * Track scroll speed at the start / end of the run, in widths per second.
     * Paced so the first barrier reaches the ball at ~3.3s — slow enough to hear
     * the microphone and react (playtest feedback: 1.9s left no time at all).
     */
    val startSpeed: Double = 0.20,
    val endSpeed: Double = 0.37,
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
 * gap placement, hitboxes and pass resolution. Free of Android, Compose and
 * coroutines, so it runs on plain JUnit with an injected clock ([tick] takes the
 * elapsed ms and the current sound state explicitly).
 *
 * Rules: docs/games/06-sound-barrier.md — the ball's hitbox touching a bar carries
 * the ball backwards (left) instead of ending the run, +1 when the ball is inside
 * the gap at the instant a barrier finishes passing it, the run ends when a bar
 * pushes the ball off the screen, and it is a win when the 60s timer expires.
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
    /** Horizontal position, 0..1: the centre until a bar pushes it backwards (left). */
    var ballX: Double = config.playerX
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
        require(config.bounceRestitution in 0.0..1.0) {
            "bounceRestitution must be between 0 and 1"
        }
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
     * resolves barriers, carries the ball back when a bar touches it and ends the
     * run when the timer expires or the ball is pushed off the screen. Ignored
     * once the run is over.
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
        checkBarrierContact()
        checkBallOnScreen()
    }

    /**
     * Integrates one vertical step. The floor and the ceiling **bounce** the ball:
     * contact reverses its speed instead of pinning it, so a fall rebounds upward
     * and a shout rebounds downward (docs/games/06-sound-barrier.md — Boundaries).
     */
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
        // Only a boundary the ball tried to cross reverses it; reaching a boundary
        // exactly is not a collision. Restitution < 1 bleeds energy off, so a fall
        // settles after a couple of rebounds instead of ringing forever.
        if (ballY != nextY) ballVelocity = -ballVelocity * config.bounceRestitution
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
     * Resolves every barrier whose trailing edge has just crossed the ball: inside
     * the gap scores a point, outside it scores nothing — the ball is being carried
     * backwards by that same bar, so it never resolves while it is still touching.
     * Checked *before* [checkBarrierContact] so the instant the trailing edge
     * clears the ball the pass is scored instead of being pushed again; with the
     * default geometry a contact fires long before that, so this branch is only a
     * guard that keeps a miss from ever scoring.
     */
    private fun resolvePassedBarriers() {
        val worldX = worldXAt(elapsedMs)
        var index = 0
        while (index < barriers.size) {
            val barrier = barriers[index]
            if (xOf(barrier, worldX) + config.barWidth > ballX) break

            val insideGap = ballY >= barrier.gapCenter - barrier.gapHalf &&
                ballY <= barrier.gapCenter + barrier.gapHalf
            if (insideGap) score++
            index++
        }
        if (index > 0) barriers = barriers.drop(index)
    }

    /**
     * Ball ↔ bar hitboxes. The ball occupies [SoundBarrierConfig.playerRadius]
     * around its centre on both normalized axes — the same convention the floor
     * and the ceiling use — and a bar pair occupies everything except its gap, so
     * any overlap means the ball is touching a bar. A touched bar never ends the
     * run: its leading face carries the ball backwards (left) for as long as they
     * touch, and slipping into the gap is the only way out (docs/games/06-sound-
     * barrier.md — Contact).
     */
    private fun checkBarrierContact() {
        if (barriers.isEmpty()) return
        val worldX = worldXAt(elapsedMs)
        val ballLeft = ballX - config.playerRadius
        val ballRight = ballX + config.playerRadius
        val ballTop = ballY + config.playerRadius
        val ballBottom = ballY - config.playerRadius

        for (barrier in barriers) {
            val left = xOf(barrier, worldX)
            if (left + config.barWidth < ballLeft || left > ballRight) continue

            val gapTop = barrier.gapCenter + barrier.gapHalf
            val gapBottom = barrier.gapCenter - barrier.gapHalf
            if (ballTop > gapTop || ballBottom < gapBottom) {
                // The face carries the ball: park it exactly on the leading edge.
                ballX = minOf(ballX, left - config.playerRadius)
            }
        }
    }

    /**
     * The only contact-driven way to lose: bars push the ball backwards (left), so
     * the run ends once the whole ball has left the screen through that edge and
     * the points earned so far are kept (docs/games/06-sound-barrier.md — Fail).
     */
    private fun checkBallOnScreen() {
        if (ballX + config.playerRadius <= 0.0) end(EndReason.LEFT_BEHIND)
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
