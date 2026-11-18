package com.rmgames.feature.game.sound

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * What the game needs from the microphone: a live input level in 0..1.
 *
 * It lives in this game module instead of `:core:sensors` because that module is
 * frozen after Phase 1a (docs/08-implementation-plan.md) and Sound Barrier is the
 * first game to read audio, so the contract is born alongside its game.
 */
interface SoundSensor {
    /**
     * Normalized input level (0..1) emitted while capture runs. Collection starts
     * and stops the microphone (Adapter + Observer), exactly like the frozen
     * sensor contracts in `:core:sensors`.
     */
    fun readings(): Flow<Float>
}

/**
 * [SoundSensor] backed by `AudioRecord`.
 *
 * Reads the raw input **level** only: nothing is encoded, written to disk or
 * uploaded (docs/games/06-sound-barrier.md — Persistence). The read blocks on the
 * audio driver, so the loop runs on [Dispatchers.IO] and stops as soon as the
 * collector cancels — which is what [com.rmgames.core.common.GameScope.hostScope]
 * does when the run ends.
 */
class AndroidSoundSensor : SoundSensor {

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun readings(): Flow<Float> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return@flow

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2,
            )
        } catch (_: IllegalArgumentException) {
            return@flow
        } catch (_: SecurityException) {
            return@flow
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return@flow
        }

        val block = ShortArray(BLOCK_SAMPLES)
        try {
            record.startRecording()
            while (coroutineContext.isActive) {
                val read = record.read(block, 0, block.size)
                if (read < 0) break
                if (read > 0) emit(SoundLevel.of(block, read))
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        /** 8 kHz mono is plenty for a loud/quiet decision and cheap to read. */
        const val SAMPLE_RATE_HZ = 8_000
        /** 20 ms of audio per block, so the ball reacts well inside one game tick. */
        const val BLOCK_SAMPLES = 160
    }
}
