package com.rmgames.core.vision

import kotlin.math.abs

/** Pure pose classifier with neutral re-arming and a gesture cooldown. */
class HeadGestureClassifier(
    private val nodThresholdDegrees: Float = DEFAULT_GESTURE_THRESHOLD_DEGREES,
    private val shakeThresholdDegrees: Float = DEFAULT_GESTURE_THRESHOLD_DEGREES,
    private val neutralThresholdDegrees: Float = DEFAULT_NEUTRAL_THRESHOLD_DEGREES,
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
) {
    private var baselinePitch: Float? = null
    private var baselineYaw: Float? = null
    private var armed = true
    private var lastGestureAt = Long.MIN_VALUE

    init {
        require(nodThresholdDegrees > neutralThresholdDegrees)
        require(shakeThresholdDegrees > neutralThresholdDegrees)
        require(neutralThresholdDegrees >= 0f)
        require(cooldownMs >= 0L)
    }

    fun classify(pitchDegrees: Float, yawDegrees: Float, nowMs: Long): HeadGesture? {
        if (!pitchDegrees.isFinite() || !yawDegrees.isFinite()) return null
        if (baselinePitch == null) {
            baselinePitch = pitchDegrees
            baselineYaw = yawDegrees
            return null
        }

        val pitchDelta = pitchDegrees - baselinePitch!!
        val yawDelta = yawDegrees - baselineYaw!!
        if (abs(pitchDelta) <= neutralThresholdDegrees && abs(yawDelta) <= neutralThresholdDegrees) {
            armed = true
            return null
        }
        if (!armed || (lastGestureAt != Long.MIN_VALUE && nowMs - lastGestureAt < cooldownMs)) return null

        val gesture = when {
            abs(pitchDelta) >= nodThresholdDegrees && abs(pitchDelta) >= abs(yawDelta) -> HeadGesture.NOD
            abs(yawDelta) >= shakeThresholdDegrees -> HeadGesture.SHAKE
            else -> return null
        }
        armed = false
        lastGestureAt = nowMs
        return gesture
    }

    fun reset() {
        baselinePitch = null
        baselineYaw = null
        armed = true
        lastGestureAt = Long.MIN_VALUE
    }

    companion object {
        const val DEFAULT_GESTURE_THRESHOLD_DEGREES = 18f
        const val DEFAULT_NEUTRAL_THRESHOLD_DEGREES = 8f
        const val DEFAULT_COOLDOWN_MS = 450L
    }
}
