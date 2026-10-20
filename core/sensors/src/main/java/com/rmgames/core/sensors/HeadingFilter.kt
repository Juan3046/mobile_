package com.rmgames.core.sensors

/**
 * Pure heading smoothing (Strategy): low-pass filter that understands the
 * 0/360 wrap-around, so 350° → 10° moves through 0° and never through 180°.
 *
 * @param alpha 0 = keep previous value forever, 1 = no filtering at all.
 */
class HeadingFilter(private val alpha: Float) {
    private var smoothed: Float? = null

    /** Feeds one raw heading (0–360) and returns the smoothed heading (0–360). */
    fun filter(raw: Float): Float {
        val current = smoothed
        if (current == null) {
            smoothed = raw.normalize()
            return smoothed!!
        }
        val shortestDelta = shortestDelta(current = current, target = raw)
        smoothed = (current + alpha * shortestDelta).normalize()
        return smoothed!!
    }

    private fun shortestDelta(current: Float, target: Float): Float {
        var delta = (target - current) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    private fun Float.normalize(): Float {
        val mod = this % 360f
        return if (mod < 0f) mod + 360f else mod
    }
}
