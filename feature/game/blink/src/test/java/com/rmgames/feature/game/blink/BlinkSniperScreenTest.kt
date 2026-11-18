package com.rmgames.feature.game.blink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the procedural bomb animation (AGENTS.md §5 — TDD).
 *
 * The bomb halo breathes off the run clock (docs/games/03-blink-sniper.md — bombs must
 * read as hazards at a glance), so its driver, [bombPulse], has to stay bounded and
 * strictly periodic or the halo would snap on every wrap.
 */
class BlinkSniperScreenTest {

    @Test
    fun `the bomb pulse stays inside 0 and 1`() {
        val samples = (0L until 10_000L step 17L).map { bombPulse(it) }

        assertTrue("out of range: ${samples.filterNot { it in 0f..1f }.take(3)}", samples.all { it in 0f..1f })
    }

    @Test
    fun `the bomb pulse repeats every period`() {
        val periodMs = 900L

        (0L..3_000L step 50L).forEach { t ->
            assertEquals(
                bombPulse(t, periodMs).toDouble(),
                bombPulse(t + periodMs, periodMs).toDouble(),
                1e-6,
            )
        }
    }

    @Test
    fun `the bomb pulse peaks at a quarter of the period and rests halfway through`() {
        val periodMs = 800L

        assertEquals(0.5, bombPulse(0, periodMs).toDouble(), 1e-6)
        assertEquals(1.0, bombPulse(periodMs / 4, periodMs).toDouble(), 1e-6)
        assertEquals(0.0, bombPulse(3 * periodMs / 4, periodMs).toDouble(), 1e-6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a non positive period is rejected`() {
        bombPulse(0, 0)
    }
}
