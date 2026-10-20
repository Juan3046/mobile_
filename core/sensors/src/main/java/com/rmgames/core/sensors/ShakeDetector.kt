package com.rmgames.core.sensors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * Pure shake logic (Strategy): decoupled from the accelerometer hardware so it
 * runs on plain JUnit. Magnitude is compared against [threshold]; consecutive
 * shakes closer than [cooldownMs] collapse into a single event.
 *
 * @param clock injectable time source (ms) — keeps the class testable.
 */
class ShakeDetector(
    private val threshold: Float,
    private val cooldownMs: Long,
    private val clock: () -> Long,
) {
    /** null = never shaken yet (avoids Long overflow on first comparison). */
    private var lastShakeAtMs: Long? = null

    fun detect(samples: Flow<Vector3>): Flow<Unit> =
        samples
            .mapNotNull {
                val now = clock()
                val isShake = it.magnitude > threshold
                val last = lastShakeAtMs
                val cooledDown = last == null || now - last >= cooldownMs
                if (isShake && cooledDown) {
                    lastShakeAtMs = now
                    Unit
                } else {
                    null
                }
            }

    companion object {
        /** ~0.5 g of acceleration delta is a comfortable shake for MVP tuning. */
        const val DEFAULT_THRESHOLD = 15f
        const val DEFAULT_COOLDOWN_MS = 400L
    }
}
