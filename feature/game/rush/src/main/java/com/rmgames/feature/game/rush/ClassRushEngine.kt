package com.rmgames.feature.game.rush

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

/** The street is this many cells wide (docs/games/06). */
const val COLUMNS = 7

/** A run lasts 60 s (inside the standard 30–90 s rule). */
const val RUN_MS = 60_000L

/** No new row for this long ends the run ("too slow"). */
const val IDLE_LIMIT_MS = 8_000L

/** How far back from your best row you may hop. */
private const val MAX_BACK_ROWS = 3

/** Half the student's width in cells, for collisions with cars and trams. */
private const val PLAYER_HALF = 0.3f

/** A little forgiveness at the ends of an ice floe. */
private const val FLOE_GRACE = 0.1f

enum class Hop { FORWARD, BACK, LEFT, RIGHT }

enum class RushPhase { PLAYING, OVER, BLOCKED }

/** Why a run ended; only running out the clock is a win. */
enum class RushEnd(val win: Boolean) { TIME_UP(true), HIT(false), FELL(false), TOO_SLOW(false) }

/** Everything the screen draws for one frame. [x] is the student's cell (float on a floe). */
data class RushState(
    val row: Int = 0,
    val x: Float = (COLUMNS / 2).toFloat(),
    val maxRow: Int = 0,
    val elapsedMs: Long = 0,
    val phase: RushPhase = RushPhase.PLAYING,
    val end: RushEnd? = null,
    /** Counts every accepted hop (drives the hop haptic). */
    val hops: Int = 0,
) {
    /** Score = rows advanced (the furthest row reached). */
    val score: Int get() = maxRow
    val timeLeftMs: Long get() = (RUN_MS - elapsedMs).coerceAtLeast(0)
}

/**
 * Pure rules of Class Rush (docs/games/06-class-rush.md): no Android, no Compose. The host
 * loop calls [tick]; tilt gestures call [hop]; lanes come from [laneAt] (seeded per run).
 */
class ClassRushEngine(private var laneAt: (row: Int) -> Lane) {
    private val _state = MutableStateFlow(RushState())
    val state: StateFlow<RushState> = _state.asStateFlow()
    private var lastProgressMs = 0L

    fun lane(row: Int): Lane = laneAt(row)

    /** A fresh run on a new street. */
    fun reset(lanes: (row: Int) -> Lane) {
        laneAt = lanes
        lastProgressMs = 0
        _state.value = RushState()
    }

    /** Missing sensor: the run is void and never reports anything. */
    fun block() {
        _state.value = _state.value.copy(phase = RushPhase.BLOCKED)
    }

    fun hop(hop: Hop) {
        val s = _state.value
        if (s.phase != RushPhase.PLAYING) return
        val next = when (hop) {
            Hop.FORWARD -> s.copy(row = s.row + 1)
            Hop.BACK -> if (s.row - 1 < maxOf(0, s.maxRow - MAX_BACK_ROWS)) return else s.copy(row = s.row - 1)
            Hop.LEFT -> s.copy(x = (s.x - 1f).coerceAtLeast(0f))
            Hop.RIGHT -> s.copy(x = (s.x + 1f).coerceAtMost((COLUMNS - 1).toFloat()))
        }
        // Solid ground puts you back on the grid; a floe keeps you where you landed.
        val landed = if (hop == Hop.FORWARD || hop == Hop.BACK) {
            if (laneAt(next.row).kind == LaneKind.POND) next else next.copy(x = next.x.roundToInt().toFloat())
        } else next
        val progressed = landed.row > s.maxRow
        if (progressed) lastProgressMs = s.elapsedMs
        _state.value = check(landed.copy(maxRow = maxOf(s.maxRow, landed.row), hops = s.hops + 1))
    }

    fun tick(dtMs: Long) {
        val s = _state.value
        if (s.phase != RushPhase.PLAYING || dtMs <= 0) return
        val elapsed = s.elapsedMs + dtMs
        val lane = laneAt(s.row)
        // A floe carries the student with it.
        val x = if (lane.kind == LaneKind.POND) s.x + lane.dir * lane.speed * dtMs / 1000f else s.x
        var next = check(s.copy(elapsedMs = elapsed, x = x))
        if (next.phase == RushPhase.PLAYING) {
            next = when {
                elapsed >= RUN_MS -> next.over(RushEnd.TIME_UP).copy(elapsedMs = RUN_MS)
                elapsed - lastProgressMs >= IDLE_LIMIT_MS -> next.over(RushEnd.TOO_SLOW)
                else -> next
            }
        }
        _state.value = next
    }

    /** Hazards at the student's current spot. */
    private fun check(s: RushState): RushState {
        val lane = laneAt(s.row)
        val t = s.elapsedMs / 1000f
        val center = s.x + 0.5f
        return when {
            center < 0f || center > COLUMNS -> s.over(RushEnd.FELL)
            lane.hits(center, t) -> s.over(RushEnd.HIT)
            lane.kind == LaneKind.POND && !lane.carries(center, t) -> s.over(RushEnd.FELL)
            else -> s
        }
    }

    private fun RushState.over(end: RushEnd) = copy(phase = RushPhase.OVER, end = end)
}

enum class LaneKind { PAVEMENT, ROAD, TRAM, POND }

/** A car, tram or ice floe: where its left end is at t = 0 and how long it is (cells). */
data class Mover(val start: Float, val length: Float)

/**
 * One row of the street. Movers slide at [speed] cells/s in [dir] and wrap around a loop of
 * [span] cells centred on the visible street, so a lane repeats forever with fixed gaps.
 */
data class Lane(
    val kind: LaneKind,
    val dir: Int = 0,
    val speed: Float = 0f,
    val movers: List<Mover> = emptyList(),
    val span: Float = COLUMNS + 4f,
) {
    private val margin get() = (span - COLUMNS) / 2f

    /** Left end of [m] at [t] seconds. */
    fun moverX(m: Mover, t: Float): Float {
        val raw = m.start + margin + dir * speed * t
        return ((raw % span) + span) % span - margin
    }

    /** A car or tram covers the student whose centre is at [center]. */
    fun hits(center: Float, t: Float): Boolean =
        (kind == LaneKind.ROAD || kind == LaneKind.TRAM) && movers.any { m ->
            val mid = moverX(m, t) + m.length / 2
            abs(center - mid) < m.length / 2 + PLAYER_HALF
        }

    /** An ice floe is under the student whose centre is at [center]. */
    fun carries(center: Float, t: Float): Boolean = movers.any { m ->
        val x = moverX(m, t)
        center >= x - FLOE_GRACE && center <= x + m.length + FLOE_GRACE
    }

    /** Some mover is off screen now but enters within the warning time (tram light). */
    fun warning(t: Float): Boolean = movers.any { m ->
        !visible(m, t) && (1..WARNING_STEPS).any { i -> visible(m, t + i * WARNING_S / WARNING_STEPS) }
    }

    private fun visible(m: Mover, t: Float): Boolean {
        val x = moverX(m, t)
        return x + m.length > 0f && x < COLUMNS
    }

    private companion object {
        const val WARNING_S = 1.2f
        const val WARNING_STEPS = 12
    }
}
