package com.rmgames.core.sensors

import kotlin.math.sqrt

/** Generic 3-axis sample (rad/s for gyro, m/s² for accelerometer). */
data class Vector3(val x: Float, val y: Float, val z: Float) {
    val magnitude: Float get() = sqrt(x * x + y * y + z * z)
}
