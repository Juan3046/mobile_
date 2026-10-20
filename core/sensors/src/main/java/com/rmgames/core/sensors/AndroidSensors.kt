package com.rmgames.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn

/**
 * Android Adapter for the motion sensors: bridges `SensorManager` callbacks to
 * [Flow]. Collecting registers the listener; cancelling unregisters it — no
 * leaks, no manual start/stop for games.
 */
internal class AndroidMotionSensor(
    private val sensorManager: SensorManager,
    private val sensorType: Int,
) {
    fun readings(): Flow<Vector3> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            close(IllegalStateException("Sensor $sensorType not available"))
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(Vector3(event.values[0], event.values[1], event.values[2]))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }.conflate().flowOn(Dispatchers.Default)
}

/** [AccelerometerSensor] backed by the device accelerometer. */
class AndroidAccelerometerSensor(context: Context) : AccelerometerSensor {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val source = AndroidMotionSensor(manager, Sensor.TYPE_ACCELEROMETER)

    override fun readings(): Flow<Vector3> = source.readings()
}

/** [GyroscopeSensor] backed by the device gyroscope. */
class AndroidGyroscopeSensor(context: Context) : GyroscopeSensor {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val source = AndroidMotionSensor(manager, Sensor.TYPE_GYROSCOPE)

    override fun readings(): Flow<Vector3> = source.readings()
}

/** [CompassSensor] backed by the magnetometer, smoothed with [HeadingFilter]. */
class AndroidCompassSensor(
    context: Context,
    private val filter: HeadingFilter = HeadingFilter(alpha = 0.15f),
) : CompassSensor {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun headings(): Flow<Float> = callbackFlow {
        val sensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (sensor == null) {
            close(IllegalStateException("Magnetometer not available"))
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Raw magnetometer vector -> azimuth approximation; the filter
                // absorbs noise and the 0/360 wrap-around.
                val azimuthDeg = Math.toDegrees(
                    kotlin.math.atan2(event.values[1].toDouble(), event.values[0].toDouble())
                ).toFloat()
                trySend(filter.filter(azimuthDeg))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { manager.unregisterListener(listener) }
    }.conflate().flowOn(Dispatchers.Default)
}
