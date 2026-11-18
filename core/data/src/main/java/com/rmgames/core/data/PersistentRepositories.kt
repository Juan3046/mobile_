package com.rmgames.core.data

import com.rmgames.core.data.store.SnapshotStore
import com.rmgames.core.data.store.WriteThrough
import com.rmgames.core.data.store.readOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Persistent repositories (plan 09 PR 6). Same pattern for all three:
 * - state lives in memory → synchronous, instant reads for the UI (offline-first);
 * - every change writes the snapshot through a [SnapshotStore] in [scope] (background);
 * - [load] once at startup, before the first read.
 * The store decides where data lives (device now, cloud later) — see [SnapshotStore].
 */

/** Days of attempts kept: yesterday and today are enough for the noon boundary. */
private const val ATTEMPT_DAYS_KEPT = 2L

/** Days of daily bests kept on device; the full history arrives with Room/ranking (PR 10/13). */
const val SCORE_DAYS_KEPT = 60L

class PersistentAttemptRepository(
    store: SnapshotStore<Map<Long, Int>>,
    scope: CoroutineScope,
    private val onError: (Throwable) -> Unit = {},
) : AttemptRepository {
    private val source = store
    private val used = mutableMapOf<Long, Int>()
    private val writer = WriteThrough(store, scope, onError) { synchronized(used) { used.toMap() } }

    suspend fun load() {
        val stored = source.readOrNull(onError).orEmpty()
        synchronized(used) { used.clear(); used.putAll(stored) }
    }

    override fun remaining(dayIndex: Long): Int =
        MAX_ATTEMPTS_PER_DAY - synchronized(used) { used[dayIndex] ?: 0 }

    override fun consume(dayIndex: Long): Boolean {
        synchronized(used) {
            val count = used[dayIndex] ?: 0
            if (count >= MAX_ATTEMPTS_PER_DAY) return false
            used[dayIndex] = count + 1
            used.keys.removeAll { it <= dayIndex - ATTEMPT_DAYS_KEPT }
        }
        writer.schedule()
        return true
    }

    override fun reset(dayIndex: Long) {
        synchronized(used) { used.remove(dayIndex) }
        writer.schedule()
    }
}

class PersistentScoreRepository(
    store: SnapshotStore<Map<Pair<Long, String>, Long>>,
    scope: CoroutineScope,
    private val onError: (Throwable) -> Unit = {},
) : ScoreRepository {
    private val source = store
    private val bests = mutableMapOf<Pair<Long, String>, Long>()
    private val writer = WriteThrough(store, scope, onError) { synchronized(bests) { bests.toMap() } }

    suspend fun load() {
        val stored = source.readOrNull(onError).orEmpty()
        synchronized(bests) { bests.clear(); bests.putAll(stored) }
    }

    override fun submit(dayIndex: Long, gameId: String, value: Long, higherWins: Boolean): Long {
        val best = synchronized(bests) {
            val key = dayIndex to gameId
            val current = bests[key]
            val next = when {
                current == null -> value
                higherWins -> maxOf(current, value)
                else -> minOf(current, value)
            }
            bests[key] = next
            bests.keys.removeAll { it.first <= dayIndex - SCORE_DAYS_KEPT }
            next
        }
        writer.schedule()
        return best
    }

    override fun best(dayIndex: Long, gameId: String): Long? = synchronized(bests) { bests[dayIndex to gameId] }

    override fun history(): List<DailyBest> = synchronized(bests) { bests.toHistory() }
}

/** User preferences for the Settings screen (plan 09 Decision 3) and the audio/haptics layer. */
data class Settings(
    val musicVolume: Float = 0.7f,
    val sfxEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
)

class SettingsRepository(
    store: SnapshotStore<Settings>,
    scope: CoroutineScope,
    private val onError: (Throwable) -> Unit = {},
) {
    private val source = store
    private val state = MutableStateFlow(Settings())
    private val writer = WriteThrough(store, scope, onError) { state.value }

    /** Current settings; collect it to react to changes (audio, haptics). */
    val settings: StateFlow<Settings> = state.asStateFlow()

    suspend fun load() {
        source.readOrNull(onError)?.let { state.value = it.sanitized() }
    }

    fun update(transform: (Settings) -> Settings) {
        state.update { transform(it).sanitized() }
        writer.schedule()
    }

    private fun Settings.sanitized() = copy(musicVolume = musicVolume.coerceIn(0f, 1f))
}
