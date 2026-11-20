package com.rmgames.core.common.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GameInfoTest {

    @Test
    fun `each game names the sensor it plays with`() {
        assertEquals(GameSensor("Head gestures", RmIcons.Camera), gameSensor("01-poruba-casino"))
        assertEquals(GameSensor("Tilt", RmIcons.Tilt), gameSensor("02-onion-roll"))
        assertEquals(GameSensor("Front camera", RmIcons.Camera), gameSensor("03-blink-sniper"))
        assertEquals(GameSensor("Compass", RmIcons.Compass), gameSensor("04-compass-chaos"))
        assertEquals(GameSensor("Shake", RmIcons.Shake), gameSensor("05-soda-shake"))
        assertEquals(GameSensor("Tilt", RmIcons.Tilt), gameSensor("06-class-rush"))
    }

    @Test
    fun `unknown game has no sensor chip`() {
        assertEquals(null, gameSensor("99-new"))
    }

    @Test
    fun `duration label follows the documented exceptions`() {
        assertEquals("60 seconds", gameDurationLabel("03-blink-sniper", 60_000))
        assertEquals("20 seconds", gameDurationLabel("04-compass-chaos", 20_000))
        assertEquals("5 hands", gameDurationLabel("01-poruba-casino", Long.MAX_VALUE))
        assertEquals("Endless", gameDurationLabel("02-onion-roll", Long.MAX_VALUE))
        assertEquals("20 shakes", gameDurationLabel("05-soda-shake", 10_000))
        assertEquals("60 seconds", gameDurationLabel("06-class-rush", 60_000))
    }

    @Test
    fun `every game has short how-to steps and a goal`() {
        listOf("01-poruba-casino", "02-onion-roll", "03-blink-sniper", "04-compass-chaos", "05-soda-shake", "06-class-rush").forEach { id ->
            val h = requireNotNull(gameHowTo(id)) { id }
            assert(h.steps.size in 2..4) { "$id: ${h.steps}" }
            assert(h.steps.all { it.length <= 90 } && h.goal.length <= 90) { "$id: keep it short" }
        }
        assertEquals("Finish the 5 hands with as many bananas as you can.", gameHowTo("01-poruba-casino")!!.goal)
        assertEquals(null, gameHowTo("99-new"))
    }
}

class GameScoreTextTest {
    @Test
    fun `points games show the raw count`() {
        assertEquals(ScoreText("27", "points"), gameScoreText("03-blink-sniper", 27))
        assertEquals(ScoreText("1", "point"), gameScoreText("04-compass-chaos", 1))
    }

    @Test
    fun `class rush shows rows`() {
        assertEquals(ScoreText("17", "rows"), gameScoreText("06-class-rush", 17))
        assertEquals(ScoreText("1", "row"), gameScoreText("06-class-rush", 1))
    }

    @Test
    fun `casino shows the banana balance`() {
        assertEquals(ScoreText("130", "bananas"), gameScoreText("01-poruba-casino", 130))
        assertEquals(ScoreText("1", "banana"), gameScoreText("01-poruba-casino", 1))
    }

    @Test
    fun `onion shows whole seconds survived`() {
        assertEquals(ScoreText("62", "seconds"), gameScoreText("02-onion-roll", 62))
    }

    @Test
    fun `soda shows centiseconds as seconds with two decimals`() {
        assertEquals(ScoreText("4.27", "seconds"), gameScoreText("05-soda-shake", 427))
        assertEquals(ScoreText("10.05", "seconds"), gameScoreText("05-soda-shake", 1005))
        assertEquals(ScoreText("0.09", "seconds"), gameScoreText("05-soda-shake", 9))
    }

    @Test
    fun `big point totals get separators`() {
        assertEquals(ScoreText("1,250", "points"), gameScoreText("99-new", 1250))
    }
}
