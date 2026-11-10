package com.rmgames.feature.game.soda

import com.rmgames.core.sensors.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Game rules of Soda Shake (docs/games/05-soda-shake.md) — pure JVM, no Android.
 *
 * Timing note: the engine advances the run clock by exactly the ticks it is
 * given, so [shakeCycle] lands on exact elapsed values (20 cycles → the 20th
 * shake at 2_296 ms → score 229 centiseconds) and detection is deterministic.
 */
class SodaShakeEngineTest {

    private val rest = Vector3(0f, GRAVITY, 0f)

    /** One full shake: a peak on [dir] (+1 / -1) for 16 ms, then the reading settles. */
    private fun SodaShakeEngine.shakeCycle(dir: Float) {
        tick(16, peak(dir))
        tick(104, rest)
    }

    private fun peak(dir: Float) = Vector3(20f * dir, GRAVITY, 0f)

    private fun engine(initial: SodaShakeState = SodaShakeState.INITIAL) =
        SodaShakeEngine(initialState = initial)

    // --- lifecycle & gauge ---

    @Test
    fun `fresh run starts running with an empty gauge`() {
        val state = engine().state.value

        assertEquals(SodaPhase.RUNNING, state.phase)
        assertEquals(0, state.shakes)
        assertEquals(0f, state.pressure, 0.0001f)
        assertEquals(0L, state.scoreCentis)
        assertEquals(0L, state.elapsedMs)
    }

    @Test
    fun `resting gravity never counts as a shake`() {
        val e = engine()

        repeat(100) { e.tick(16, rest) } // 1.6 s of a phone lying still

        assertEquals(0, e.state.value.shakes)
        assertEquals(0f, e.state.value.pressure, 0.0001f)
    }

    @Test
    fun `a peak below the threshold never counts`() {
        val e = engine()

        repeat(20) { e.tick(16, Vector3(14f, 0f, 0f)) } // magnitude 14 < 15
        e.tick(104, rest)
        repeat(20) { e.tick(16, Vector3(14f, 0f, 0f)) }

        assertEquals(0, e.state.value.shakes)
    }

    @Test
    fun `one peak counts one shake and fills the gauge by 5 percent`() {
        val e = engine()

        e.tick(16, peak(+1f))

        assertEquals(1, e.state.value.shakes)
        assertEquals(SodaShakeEngine.PRESSURE_PER_SHAKE, e.state.value.pressure, 0.0001f)
    }

    @Test
    fun `a sustained peak counts only once until the phone settles`() {
        val e = engine()

        repeat(30) { e.tick(16, peak(+1f)) } // held hard for 480 ms

        assertEquals(1, e.state.value.shakes)
    }

    @Test
    fun `the reading must fall below the release level before the next peak counts`() {
        val e = engine()

        e.tick(16, peak(+1f)) // t=16 → count 1
        repeat(8) { e.tick(16, peak(+1f)) } // still high: no recount (t=144)
        e.tick(104, rest) // t=248 → settles, re-arms
        e.tick(16, peak(-1f)) // t=264, dead time ok, direction flips → count 2

        assertEquals(2, e.state.value.shakes)
    }

    @Test
    fun `peaks closer than the dead time are ignored`() {
        val e = engine()

        e.tick(16, peak(+1f)) // t=16 → count 1
        e.tick(10, rest) // t=26 → re-armed
        e.tick(50, peak(-1f)) // t=76 → only 60 ms after the count: ignored

        assertEquals(1, e.state.value.shakes)

        e.tick(100, rest) // t=176 → re-armed, past the dead time
        e.tick(16, peak(-1f)) // t=192 → 176 ms after count 1, direction flips → count 2

        assertEquals(2, e.state.value.shakes)
    }

    @Test
    fun `shakes must alternate direction — tapping the same way does not count`() {
        val e = engine()

        e.tick(16, peak(+1f)) // t=16 → count 1 (dir +)
        e.tick(120, rest) // t=136 → re-armed
        e.tick(16, peak(+1f)) // t=152 → dead time ok, but same direction: ignored
        e.tick(120, rest) // t=272 → re-armed
        e.tick(16, peak(-1f)) // t=288 → opposite direction → count 2

        assertEquals(2, e.state.value.shakes)
    }

    @Test
    fun `direction is the sign of the dominant axis`() {
        assertEquals(1, SodaShakeEngine.directionOf(Vector3(20f, GRAVITY, 0f)))
        assertEquals(-1, SodaShakeEngine.directionOf(Vector3(-20f, GRAVITY, 0f)))
        assertEquals(-1, SodaShakeEngine.directionOf(Vector3(0f, GRAVITY, -25f)))
    }

    // --- pop, score and end of run ---

    @Test
    fun `twenty shakes pop the can and freeze the score at the pop time`() {
        val e = engine()

        repeat(20) { index -> e.shakeCycle(if (index % 2 == 0) +1f else -1f) }

        val s = e.state.value
        assertEquals(SodaPhase.POPPING, s.phase)
        assertEquals(20, s.shakes)
        assertEquals(1f, s.pressure, 0.0001f)
        // The 20th peak lands at 19 * 120 ms + 16 ms = 2_296 ms → 229 centiseconds;
        // the trailing settle tick of the cycle already runs into the pop animation.
        assertEquals(229L, s.scoreCentis)
    }

    @Test
    fun `the pop animation ends the run without moving the score`() {
        val e = engine()
        repeat(20) { index -> e.shakeCycle(if (index % 2 == 0) +1f else -1f) }

        e.tick(400, rest) // pop animation

        assertEquals(SodaPhase.DONE, e.state.value.phase)
        assertEquals(1f, e.state.value.popProgress, 0.0001f)
        assertEquals(229L, e.state.value.scoreCentis)

        e.tick(5_000, rest) // the score never moves after the pop
        assertEquals(229L, e.state.value.scoreCentis)
        assertEquals(SodaPhase.DONE, e.state.value.phase)
    }

    @Test
    fun `the run clock is live stopwatch style until the pop`() {
        val e = engine()

        e.tick(1_500, rest)
        assertEquals(150L, e.state.value.scoreCentis)

        e.tick(1_000, rest)
        assertEquals(250L, e.state.value.scoreCentis)
    }

    // --- debug input (emulator playtesting only) ---

    @Test
    fun `debug shakes count without any sensor reading`() {
        val e = engine()

        repeat(3) { e.debugShake() }

        assertEquals(3, e.state.value.shakes)
        assertEquals(0.15f, e.state.value.pressure, 0.0001f)
    }

    // --- missing sensor: block, never a fake score ---

    @Test
    fun `blocking freezes the run without scoring`() {
        val e = engine()

        e.tick(500, rest)
        e.block()

        assertEquals(SodaPhase.BLOCKED, e.state.value.phase)
        assertEquals(0L, e.state.value.scoreCentis)
        assertEquals(0, e.state.value.shakes)

        e.tick(5_000, peak(+1f)) // a blocked run never resumes or scores
        assertEquals(SodaPhase.BLOCKED, e.state.value.phase)
        assertEquals(0L, e.state.value.scoreCentis)
    }

    @Test
    fun `reset restores a clean run for the retry`() {
        val e = engine()
        repeat(20) { index -> e.shakeCycle(if (index % 2 == 0) +1f else -1f) }
        e.tick(400, rest)
        assertEquals(SodaPhase.DONE, e.state.value.phase)

        e.reset()

        val s = e.state.value
        assertEquals(SodaPhase.RUNNING, s.phase)
        assertEquals(0, s.shakes)
        assertEquals(0f, s.pressure, 0.0001f)
        assertEquals(0L, s.scoreCentis)
        assertEquals(0L, s.elapsedMs)
    }

    private companion object {
        const val GRAVITY = 9.81f
    }
}
