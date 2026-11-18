package com.rmgames.core.common.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GameInfoTest {

    @Test
    fun `each game names the sensor it plays with`() {
        assertEquals(GameSensor("Touch drag", RmIcons.Run), gameSensor("01-escape-poruba"))
        assertEquals(GameSensor("Tilt", RmIcons.Tilt), gameSensor("02-onion-roll"))
        assertEquals(GameSensor("Front camera", RmIcons.Camera), gameSensor("03-blink-sniper"))
        assertEquals(GameSensor("Compass", RmIcons.Compass), gameSensor("04-compass-chaos"))
        assertEquals(GameSensor("Shake", RmIcons.Shake), gameSensor("05-soda-shake"))
    }

    @Test
    fun `unknown game has no sensor chip`() {
        assertEquals(null, gameSensor("99-new"))
    }

    @Test
    fun `duration label follows the documented exceptions`() {
        assertEquals("60 seconds", gameDurationLabel("03-blink-sniper", 60_000))
        assertEquals("20 seconds", gameDurationLabel("04-compass-chaos", 20_000))
        assertEquals("90 seconds", gameDurationLabel("01-escape-poruba", 90_000))
        assertEquals("Endless", gameDurationLabel("02-onion-roll", Long.MAX_VALUE))
        assertEquals("20 shakes", gameDurationLabel("05-soda-shake", 10_000))
    }
}
