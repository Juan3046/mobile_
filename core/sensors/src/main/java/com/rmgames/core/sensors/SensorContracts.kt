package com.rmgames.core.sensors

import kotlinx.coroutines.flow.Flow

/**
 * Frozen Phase 1a contract (see docs/08-implementation-plan.md).
 *
 * Games collect [readings] — the flow starts/stops the hardware listener with
 * the collection (Adapter + Observer). Never expose `SensorManager` outside
 * this module; games depend on these interfaces + fakes only.
 */
interface GyroscopeSensor {
    /** Angular velocity per axis in rad/s. */
    fun readings(): Flow<Vector3>
}

interface AccelerometerSensor {
    /** Linear acceleration per axis in m/s² (including gravity). */
    fun readings(): Flow<Vector3>
}

interface CompassSensor {
    /** Heading in degrees (0–360, 0 = North), already smoothed. */
    fun headings(): Flow<Float>
}
