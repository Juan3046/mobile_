package com.rmgames.app

import com.rmgames.core.sensors.SensorAvailability
import com.rmgames.core.sensors.SensorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SensorGateTest {

    private fun phoneWithout(vararg missing: SensorType) = object : SensorAvailability {
        override fun isAvailable(type: SensorType) = type !in missing
    }

    @Test
    fun `each game needs the sensor its spec names`() {
        assertEquals(emptyList<SensorType>(), requiredSensors("01-poruba-casino")) // touch fallback
        assertEquals(listOf(SensorType.GYROSCOPE), requiredSensors("02-onion-roll"))
        assertEquals(listOf(SensorType.FRONT_CAMERA), requiredSensors("03-blink-sniper"))
        assertEquals(listOf(SensorType.MAGNETOMETER), requiredSensors("04-compass-chaos"))
        assertEquals(listOf(SensorType.ACCELEROMETER), requiredSensors("05-soda-shake"))
    }

    @Test
    fun `a missing sensor blocks the game with a clear reason`() {
        assertEquals(
            "Soda Shake needs an accelerometer, and this phone doesn't have one.",
            unavailableReason("05-soda-shake", "Soda Shake", phoneWithout(SensorType.ACCELEROMETER)),
        )
        assertEquals(
            "Blink Sniper needs a front camera, and this phone doesn't have one.",
            unavailableReason("03-blink-sniper", "Blink Sniper", phoneWithout(SensorType.FRONT_CAMERA)),
        )
    }

    @Test
    fun `games whose sensors are present, or that have a fallback, stay playable`() {
        assertNull(unavailableReason("05-soda-shake", "Soda Shake", phoneWithout()))
        assertNull(unavailableReason("01-poruba-casino", "Poruba Casino", phoneWithout(SensorType.FRONT_CAMERA)))
        assertNull(unavailableReason("99-unknown", "Unknown", phoneWithout(*SensorType.entries.toTypedArray())))
    }
}
