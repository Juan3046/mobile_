package com.rmgames.feature.game.rush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rules of Class Rush (docs/games/06-class-rush.md) — pure JVM, hand-built lanes. */
class ClassRushEngineTest {

    private val pavement = Lane(LaneKind.PAVEMENT)

    /** Everything is pavement unless a row is listed. */
    private fun engine(vararg rows: Pair<Int, Lane>): ClassRushEngine {
        val map = rows.toMap()
        return ClassRushEngine { row -> map[row] ?: pavement }
    }

    @Test
    fun `a run starts on the pavement, middle column, no score`() {
        val s = engine().state.value
        assertEquals(0, s.row)
        assertEquals(3f, s.x, 0.001f)
        assertEquals(0, s.score)
        assertEquals(RushPhase.PLAYING, s.phase)
    }

    @Test
    fun `hopping forward scores rows, hopping back never lowers the score`() {
        val e = engine()
        repeat(3) { e.hop(Hop.FORWARD) }
        e.hop(Hop.BACK)
        assertEquals(2, e.state.value.row)
        assertEquals(3, e.state.value.score)
    }

    @Test
    fun `you can't run back more than three rows behind your best, nor below the start`() {
        val e = engine()
        e.hop(Hop.BACK)
        assertEquals(0, e.state.value.row)
        repeat(5) { e.hop(Hop.FORWARD) }
        repeat(6) { e.hop(Hop.BACK) }
        assertEquals(2, e.state.value.row)
    }

    @Test
    fun `sideways steps stop at the edges`() {
        val e = engine()
        repeat(10) { e.hop(Hop.LEFT) }
        assertEquals(0f, e.state.value.x, 0.001f)
        repeat(10) { e.hop(Hop.RIGHT) }
        assertEquals((COLUMNS - 1).toFloat(), e.state.value.x, 0.001f)
    }

    @Test
    fun `a car reaching you ends the run and keeps the score`() {
        // A 1-cell car on row 1 heading right, starting at column 0, 2 cells/s.
        val road = Lane(LaneKind.ROAD, dir = 1, speed = 2f, movers = listOf(Mover(start = 0f, length = 1f)))
        val e = engine(1 to road)
        e.hop(Hop.FORWARD) // row 1, column 3: the car reaches it in ~1.5 s
        e.tick(1_000)
        assertEquals(RushPhase.PLAYING, e.state.value.phase)
        e.tick(500)
        assertEquals(RushPhase.OVER, e.state.value.phase)
        assertEquals(RushEnd.HIT, e.state.value.end)
        assertEquals(1, e.state.value.score)
    }

    @Test
    fun `hopping onto a car ends the run at once`() {
        val road = Lane(LaneKind.ROAD, dir = 1, speed = 0.5f, movers = listOf(Mover(start = 2.6f, length = 2f)))
        val e = engine(1 to road)
        e.hop(Hop.FORWARD)
        assertEquals(RushEnd.HIT, e.state.value.end)
    }

    @Test
    fun `a floe carries you and the water does not`() {
        val pond = Lane(LaneKind.POND, dir = 1, speed = 1f, movers = listOf(Mover(start = 2f, length = 3f)))
        val e = engine(1 to pond)
        e.hop(Hop.FORWARD) // lands at 3, on the floe [2, 5)
        e.tick(1_000)
        assertEquals(RushPhase.PLAYING, e.state.value.phase)
        assertEquals(4f, e.state.value.x, 0.01f) // carried one cell right
        e.hop(Hop.LEFT); e.hop(Hop.LEFT); e.hop(Hop.LEFT) // off the floe's left end
        assertEquals(RushEnd.FELL, e.state.value.end)
    }

    @Test
    fun `being carried off the edge ends the run`() {
        val pond = Lane(LaneKind.POND, dir = 1, speed = 2f, movers = listOf(Mover(start = 2f, length = 3f)), span = 30f)
        val e = engine(1 to pond)
        e.hop(Hop.FORWARD)
        e.tick(2_000) // x 3 → 7: past the last column
        assertEquals(RushEnd.FELL, e.state.value.end)
    }

    @Test
    fun `landing back on solid ground snaps to the nearest column`() {
        val pond = Lane(LaneKind.POND, dir = 1, speed = 1f, movers = listOf(Mover(start = 2f, length = 3f)))
        val e = engine(1 to pond)
        e.hop(Hop.FORWARD)
        e.tick(400) // x = 3.4
        e.hop(Hop.FORWARD)
        assertEquals(3f, e.state.value.x, 0.001f)
    }

    @Test
    fun `standing still too long ends the run`() {
        val e = engine()
        e.hop(Hop.FORWARD)
        e.tick(7_900)
        assertEquals(RushPhase.PLAYING, e.state.value.phase)
        e.hop(Hop.LEFT) // sideways is not progress
        e.tick(200)
        assertEquals(RushEnd.TOO_SLOW, e.state.value.end)
    }

    @Test
    fun `the run ends at 60 seconds with a win`() {
        val e = engine()
        repeat(60) { e.hop(Hop.FORWARD); e.tick(1_000) }
        val s = e.state.value
        assertEquals(RushEnd.TIME_UP, s.end)
        assertTrue(s.end!!.win)
        assertEquals(60, s.score)
    }

    @Test
    fun `after the run ends nothing moves`() {
        val e = engine()
        e.tick(8_100)
        e.hop(Hop.FORWARD)
        assertEquals(0, e.state.value.row)
    }

    @Test
    fun `a tram lane warns before the tram comes into view`() {
        // Tram 5 long heading right, front at -8 (entirely off screen), 10 cells/s: enters at 0.8 s.
        val tram = Lane(LaneKind.TRAM, dir = 1, speed = 10f, movers = listOf(Mover(start = -13f, length = 5f)), span = 60f)
        assertTrue(tram.warning(0f))
        assertFalse(tram.warning(3f)) // passed and gone
    }

    @Test
    fun `a car only hits when it visibly touches the student`() {
        // Regression (playtest 2026-11-20 "collisions are wrong"): a car whose drawn body ends
        // 0.1 cell before the student's body used to count as a hit.
        val student = 3.5f // centre of column 3
        val nearMiss = Lane(LaneKind.ROAD, dir = 1, speed = 0f, movers = listOf(Mover(2.25f, 1f)))
        assertFalse(nearMiss.hits(student, 0f))
        val touching = Lane(LaneKind.ROAD, dir = 1, speed = 0f, movers = listOf(Mover(2.5f, 1f)))
        assertTrue(touching.hits(student, 0f))
        // The car's drawn body and its hit box are the same rectangle.
        assertEquals(2.25f + CAR_INSET, nearMiss.moverX(nearMiss.movers[0], 0f) + CAR_INSET, 0.001f)
    }

    @Test
    fun `you are safe on the drawn ice and fall just off it`() {
        val pond = Lane(LaneKind.POND, dir = 1, speed = 0f, movers = listOf(Mover(2f, 2f)))
        assertTrue(pond.carries(2.1f, 0f))
        assertTrue(pond.carries(3.9f, 0f))
        assertFalse(pond.carries(4.3f, 0f))
    }

    @Test
    fun `only road and tram movers hurt`() {
        val road = Lane(LaneKind.ROAD, dir = 1, speed = 0f, movers = listOf(Mover(3f, 1f)))
        assertTrue(road.hits(3.5f, 0f))
        assertFalse(road.hits(1.5f, 0f))
        assertFalse(pavement.hits(3.5f, 0f))
        assertNull(RushState().end)
    }
}
