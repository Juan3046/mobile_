package com.rmgames.app

import com.rmgames.core.sensors.SensorAvailability
import com.rmgames.core.sensors.SensorType

/**
 * Hardware each game cannot run without (from each spec in docs/games/). Poruba Casino
 * needs nothing: without a front camera it switches to HIT/STAND buttons.
 */
internal fun requiredSensors(gameId: String): List<SensorType> = when (gameId.split('-').getOrNull(1)) {
    "onion" -> listOf(SensorType.GYROSCOPE)
    "blink" -> listOf(SensorType.FRONT_CAMERA)
    "compass" -> listOf(SensorType.MAGNETOMETER)
    "soda" -> listOf(SensorType.ACCELEROMETER)
    else -> emptyList()
}

/**
 * Why [gameId] can't be played on this phone, or null when it can (docs/02 "Missing sensor:
 * block + message"): Play is disabled with this text and no attempt is ever spent.
 */
internal fun unavailableReason(gameId: String, title: String, availability: SensorAvailability): String? {
    val missing = requiredSensors(gameId).firstOrNull { !availability.isAvailable(it) } ?: return null
    return "$title needs ${sensorName(missing)}, and this phone doesn't have one."
}

private fun sensorName(type: SensorType): String = when (type) {
    SensorType.GYROSCOPE -> "a gyroscope"
    SensorType.ACCELEROMETER -> "an accelerometer"
    SensorType.MAGNETOMETER -> "a compass (magnetometer)"
    SensorType.FRONT_CAMERA -> "a front camera"
}
