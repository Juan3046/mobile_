package com.rmgames.core.vision

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Fake [BlinkDetector] for game unit tests (Phase 1b): tests push eye states via
 * [emit] instead of using the front camera.
 */
class FakeBlinkDetector : BlinkDetector {
    private val source = MutableSharedFlow<EyeState>(extraBufferCapacity = 16)
    override fun eyeState(): Flow<EyeState> = source
    suspend fun emit(state: EyeState) = source.emit(state)
}
