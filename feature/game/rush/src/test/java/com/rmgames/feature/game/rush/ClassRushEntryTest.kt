package com.rmgames.feature.game.rush

import com.rmgames.core.sensors.FakeAccelerometerSensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassRushEntryTest {
    @Test
    fun `entry exposes the game with its id, rules and own HUD, and needs no permission`() {
        val entry = classRushEntry(FakeAccelerometerSensor())
        assertEquals("06-class-rush", entry.game.id)
        assertEquals(60_000L, entry.game.durationMs)
        assertTrue(entry.game.higherWins)
        assertTrue(entry.drawsOwnHud)
        assertTrue(entry.requiredPermissions.isEmpty())
    }
}
