package com.rmgames.core.sensors

/** Sensors used across minigames. Contracts + Android adapters frozen in Phase 1a (docs/08-implementation-plan.md). */
enum class SensorType {
    GYROSCOPE,
    ACCELEROMETER,
    MAGNETOMETER,
    FRONT_CAMERA
}

interface SensorAvailability {
    fun isAvailable(type: SensorType): Boolean
}
