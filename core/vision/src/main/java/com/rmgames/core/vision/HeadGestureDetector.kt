package com.rmgames.core.vision

import kotlinx.coroutines.flow.Flow

/** Deliberate head action recognized by the front-camera pipeline. */
enum class HeadGesture { NOD, SHAKE }

/** Camera/face tracking feedback consumed by camera-driven games. */
sealed interface HeadGestureObservation {
    data class FaceTracked(val pitchDegrees: Float, val yawDegrees: Float) : HeadGestureObservation
    data object FaceLost : HeadGestureObservation
    data object TrackingError : HeadGestureObservation
    data class Gesture(val value: HeadGesture) : HeadGestureObservation
}

/** Camera implementation stays behind this interface; consumers never depend on ML Kit. */
interface HeadGestureDetector {
    fun observations(): Flow<HeadGestureObservation>
}
