package com.rmgames.core.common.ui

import com.rmgames.core.common.theme.RmDimens
import com.rmgames.core.common.theme.RmPalette
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LedgeTest {
    private val d = RmDimens()

    @Test
    fun `resting control sits on the full ledge`() {
        assertEquals(LedgeGeometry(contentOffset = d.ledge * 0, ledgeHeight = d.ledge), ledgeGeometry(false, d))
    }

    @Test
    fun `pressed control sinks by the press shift and the ledge shrinks`() {
        val g = ledgeGeometry(true, d)
        assertEquals(d.pressShift, g.contentOffset)
        assertEquals(d.ledgePressed, g.ledgeHeight)
    }

    @Test
    fun `footprint never changes between states so layout does not jump`() {
        listOf(true, false).forEach {
            val g = ledgeGeometry(it, d)
            assertEquals(d.ledge, g.contentOffset + g.ledgeHeight)
        }
    }
}

class FormattingTest {
    @Test
    fun `countdown is HH MM SS`() {
        assertEquals("21:14:08", formatCountdown(21.hours + 14.minutes + 8.seconds))
        assertEquals("00:00:59", formatCountdown(59.seconds))
    }

    @Test
    fun `countdown rounds partial seconds up so it never shows zero early`() {
        assertEquals("00:00:01", formatCountdown(200.0.seconds / 1000))
    }

    @Test
    fun `negative countdown clamps to zero`() {
        assertEquals("00:00:00", formatCountdown((-5).seconds))
    }

    @Test
    fun `points use thousands separators and optional sign`() {
        assertEquals("4,975", formatPoints(4975))
        assertEquals("+4,975", formatPoints(4975, signed = true))
        assertEquals("0", formatPoints(0, signed = true))
        assertEquals("1,234,567", formatPoints(1_234_567))
    }

    @Test
    fun `attempts label reads naturally`() {
        assertEquals("2 of 2 attempts left", attemptsLabel(2, 2))
        assertEquals("1 of 2 attempts left", attemptsLabel(1, 2))
        assertEquals("No attempts left", attemptsLabel(0, 2))
    }

    @Test
    fun `pips fill remaining first and clamp out-of-range values`() {
        assertEquals(listOf(true, true), pipStates(2, 2))
        assertEquals(listOf(true, false), pipStates(1, 2))
        assertEquals(listOf(false, false), pipStates(0, 2))
        assertEquals(listOf(true, true), pipStates(5, 2))
        assertEquals(listOf(false, false), pipStates(-1, 2))
    }
}

class ScoreCounterLogicTest {
    @Test
    fun `count up starts at zero and lands exactly on the target`() {
        assertEquals(0L, countUpValue(27, 0f))
        assertEquals(27L, countUpValue(27, 1f))
    }

    @Test
    fun `count up is monotonic`() {
        val values = (0..20).map { countUpValue(4975, it / 20f) }
        assertTrue(values.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun `fraction outside 0 to 1 is clamped`() {
        assertEquals(0L, countUpValue(10, -0.5f))
        assertEquals(10L, countUpValue(10, 1.7f))
    }

    @Test
    fun `negative targets count down from zero`() {
        assertEquals(-8L, countUpValue(-8, 1f))
        assertEquals(-4L, countUpValue(-8, 0.5f))
    }
}

class SeasonStripLogicTest {
    @Test
    fun `days before today are done and after are upcoming`() {
        assertEquals(
            listOf(SeasonDay.Done, SeasonDay.Done, SeasonDay.Done, SeasonDay.Today,
                SeasonDay.Upcoming, SeasonDay.Upcoming, SeasonDay.Upcoming),
            seasonDays(todayIndex = 3),
        )
    }

    @Test
    fun `first and last day`() {
        assertEquals(SeasonDay.Today, seasonDays(0).first())
        assertEquals(SeasonDay.Today, seasonDays(6).last())
        assertEquals(7, seasonDays(6).size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `today outside the season is rejected`() {
        seasonDays(7)
    }
}

class GameVisualsTest {
    private val c = RmPalette.Dark

    @Test
    fun `each known game maps to its accent`() {
        assertEquals(c.gameEscape, gameAccent("01-escape-poruba", c))
        assertEquals(c.gameOnion, gameAccent("02-onion-roll", c))
        assertEquals(c.gameBlink, gameAccent("03-blink-sniper", c))
        assertEquals(c.gameCompass, gameAccent("04-compass-chaos", c))
        assertEquals(c.gameSoda, gameAccent("05-soda-shake", c))
    }

    @Test
    fun `unknown game falls back to a neutral container`() {
        assertEquals(c.surfaceContainerHighest, gameAccent("99-new-game", c))
    }

    @Test
    fun `each known game has its own glyph and unknown gets the play glyph`() {
        val ids = listOf("01-escape-poruba", "02-onion-roll", "03-blink-sniper", "04-compass-chaos", "05-soda-shake")
        assertEquals(ids.size, ids.map { gameGlyph(it) }.toSet().size)
        assertEquals(RmIcons.Play, gameGlyph("99-new-game"))
    }
}

class RmIconsTest {
    @Test
    fun `every icon builds on a 24 grid with at least one path`() {
        RmIcons.all.forEach { (name, icon) ->
            assertEquals("$name width", 24f, icon.viewportWidth)
            assertEquals("$name height", 24f, icon.viewportHeight)
            assertTrue("$name has no paths", icon.root.size > 0)
        }
    }

    @Test
    fun `catalogue holds the shell icons`() {
        listOf("play", "replay", "settings", "close", "back", "timer", "lock", "star", "trophy",
            "podium", "camera", "bug", "eye", "compass", "can", "onion", "run", "shake", "tilt", "info")
            .forEach { assertTrue("missing $it", it in RmIcons.all) }
    }
}

class ConfettiLogicTest {
    @Test
    fun `same seed gives the same burst`() {
        assertEquals(confettiPieces(24, seed = 7), confettiPieces(24, seed = 7))
    }

    @Test
    fun `different seeds differ`() {
        assertNotEquals(confettiPieces(24, seed = 7), confettiPieces(24, seed = 8))
    }

    @Test
    fun `pieces stay inside the unit box and use the three palette slots`() {
        val pieces = confettiPieces(200, seed = 1)
        assertEquals(200, pieces.size)
        assertTrue(pieces.all { it.x in 0f..1f && it.y in 0f..1f })
        assertEquals(setOf(0, 1, 2), pieces.map { it.colorSlot }.toSet())
    }
}

class RmClickTest {
    @Test
    fun `enabled click plays haptic and sound then runs the action`() {
        val feedback = mockk<UiFeedback>(relaxed = true)
        val haptic = mockk<() -> Unit>(relaxed = true)
        val action = mockk<() -> Unit>(relaxed = true)
        handleRmClick(enabled = true, onClick = action, feedback = feedback, haptic = haptic)
        verifyOrder { haptic(); feedback.click(); action() }
    }

    @Test
    fun `disabled click does nothing`() {
        val feedback = mockk<UiFeedback>(relaxed = true)
        val haptic = mockk<() -> Unit>(relaxed = true)
        val action = mockk<() -> Unit>(relaxed = true)
        handleRmClick(enabled = false, onClick = action, feedback = feedback, haptic = haptic)
        verify(exactly = 0) { haptic(); feedback.click(); action() }
    }
}
