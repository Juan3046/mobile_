package com.rmgames.core.sensors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Fakes for game unit tests (Phase 1b): games inject these instead of the
 * Android adapters, and tests push samples via [emit].
 */
class FakeGyroscopeSensor : GyroscopeSensor {
    private val source = MutableSharedFlow<Vector3>(extraBufferCapacity = 64)
    override fun readings(): Flow<Vector3> = source
    suspend fun emit(sample: Vector3) = source.emit(sample)
}

class FakeAccelerometerSensor : AccelerometerSensor {
    private val source = MutableSharedFlow<Vector3>(extraBufferCapacity = 64)
    override fun readings(): Flow<Vector3> = source
    suspend fun emit(sample: Vector3) = source.emit(sample)
}

class FakeCompassSensor : CompassSensor {
    private val source = MutableSharedFlow<Float>(extraBufferCapacity = 64)
    override fun headings(): Flow<Float> = source
    suspend fun emit(heading: Float) = source.emit(heading)
}
