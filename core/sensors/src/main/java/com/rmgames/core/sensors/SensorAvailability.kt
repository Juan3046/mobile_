package com.rmgames.core.sensors

/** Sensors used across minigames. Wrappers land in Phase 1 (owner: P4). */
enum class SensorType {
    GYROSCOPE,
    ACCELEROMETER,
    MAGNETOMETER,
    FRONT_CAMERA
}

interface SensorAvailability {
    fun isAvailable(type: SensorType): Boolean
}
