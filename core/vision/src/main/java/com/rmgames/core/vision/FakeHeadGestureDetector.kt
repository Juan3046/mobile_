package com.rmgames.core.vision

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Fake detector for JVM game tests; tests drive camera observations explicitly. */
class FakeHeadGestureDetector : HeadGestureDetector {
    private val source = MutableSharedFlow<HeadGestureObservation>(extraBufferCapacity = 32)
    override fun observations(): Flow<HeadGestureObservation> = source
    suspend fun emit(observation: HeadGestureObservation) = source.emit(observation)
}
