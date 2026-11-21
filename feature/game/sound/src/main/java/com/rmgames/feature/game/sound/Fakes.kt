package com.rmgames.feature.game.sound

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake for game unit tests (Phase 1b): tests inject this instead of
 * [AndroidSoundSensor] and push levels via [emit].
 *
 * A level is *current state* rather than a one-off event — "how loud is it right
 * now" — so it is backed by a [MutableStateFlow]: a collector that subscribes after
 * a level was set still reads that level immediately.
 */
class FakeSoundSensor(initialLevel: Float = 0f) : SoundSensor {
    private val level = MutableStateFlow(initialLevel)

    override fun readings(): Flow<Float> = level

    fun emit(newLevel: Float) {
        level.value = newLevel
    }
}
