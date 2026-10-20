package com.rmgames.core.vision

import kotlinx.coroutines.flow.Flow

/** Eye state as seen by the blink pipeline. */
enum class EyeState { OPEN, CLOSED }

/**
 * Frozen Phase 1a contract (see docs/08-implementation-plan.md).
 *
 * The game ([com.rmgames.feature.game.blink] in Phase 1b) consumes this Flow and
 * never touches CameraX or ML Kit — those stay behind the Adapter implementation.
 */
interface BlinkDetector {
    /** Live eye state from the front camera; the flow drives the pipeline. */
    fun eyeState(): Flow<EyeState>
}
