package com.rmgames.feature.game.onion

import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

/** Sound/vibration moments of a run (docs/games/02-onion-roll.md — Audio & Haptics). */
enum class Cue {
    HIT,
    TICK,
    DANGER,
    FALL,
}

/**
 * Turns the stream of [OnionRollState] frames into [Cue]s. Pure and stateful (one
 * per screen): the first frame and any restart (time going backwards) only
 * re-sync, so a new run never replays the old one's sounds.
 */
internal class CueDetector {
    private var last: OnionRollState? = null
    private var lastHitAtMs = Long.MIN_VALUE / 2
    private var dangerArmed = true

    fun next(state: OnionRollState): List<Cue> {
        val previous = last
        last = state
        if (previous == null || state.elapsedMs < previous.elapsedMs) {
            lastHitAtMs = Long.MIN_VALUE / 2
            dangerArmed = dangerLevel(state.onionY) < DANGER_REARM
            return emptyList()
        }

        val cues = mutableListOf<Cue>()
        val running = state.phase == Phase.RUNNING
        if (previous.phase == Phase.RUNNING && !running) cues += Cue.FALL

        if (state.hits > previous.hits && state.elapsedMs - lastHitAtMs >= HIT_COOLDOWN_MS) {
            cues += Cue.HIT
            lastHitAtMs = state.elapsedMs
        }

        if (running && state.scoreSeconds > previous.scoreSeconds) cues += Cue.TICK

        val danger = dangerLevel(state.onionY)
        if (running && dangerArmed && danger >= DANGER_WARN) {
            cues += Cue.DANGER
            dangerArmed = false
        } else if (danger <= DANGER_REARM) {
            dangerArmed = true
        }
        return cues
    }

    private companion object {
        const val HIT_COOLDOWN_MS = 120L // a sustained push stays one "plof", not a buzz
        const val DANGER_WARN = 0.6f
        const val DANGER_REARM = 0.3f
    }
}

/** Vibration length for [cue] in ms; null = no vibration. */
internal fun hapticMs(cue: Cue): Long? = when (cue) {
    Cue.HIT -> 25L
    Cue.FALL -> 250L
    Cue.TICK, Cue.DANGER -> null
}

/**
 * Hands each cue to [sink] on [executor], so slow audio/vibration calls never
 * stall the caller — the game loop and Compose share the main thread, and
 * AudioTrack restarts there froze the game ~0.5 s per tick on a device playtest.
 * Cues arriving after the executor is shut down are dropped.
 */
internal class AsyncCuePlayer(
    private val executor: Executor,
    private val sink: (Cue) -> Unit,
) {
    fun play(cue: Cue) {
        try {
            executor.execute { sink(cue) }
        } catch (_: RejectedExecutionException) {
            // Screen already left: nothing to play.
        }
    }
}

/** What a cue actually does once the user's Settings are applied. */
internal data class CueOutput(val playSound: Boolean, val hapticMs: Long?)

/** Applies the app's Sound effects / Vibration switches (plan 09 §2B) to a cue. */
internal fun cueOutput(cue: Cue, sfxEnabled: Boolean, hapticsEnabled: Boolean) =
    CueOutput(playSound = sfxEnabled, hapticMs = if (hapticsEnabled) hapticMs(cue) else null)
