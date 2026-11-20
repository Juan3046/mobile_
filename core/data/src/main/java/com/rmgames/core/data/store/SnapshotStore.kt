package com.rmgames.core.data.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Where a repository's state lives — the **one seam to swap for the cloud**.
 *
 * Repositories keep their state in memory (instant, offline-first reads for the UI) and
 * persist a whole snapshot through this interface. Today it is [DataStoreSnapshotStore]
 * (on the device). To store in the cloud, implement it on top of the remote API (e.g. a
 * Firestore document per player) — or wrap a local and a remote store in one that writes
 * both and reads the newest — and pass it to the repository in `:app`. Screens, rules
 * and tests stay unchanged. See docs/04-tech-stack.md "Data layer".
 */
interface SnapshotStore<T> {
    /** The stored snapshot, or null if there is none (first run, wiped, unreadable). */
    suspend fun read(): T?

    /** Replaces the stored snapshot. */
    suspend fun write(value: T)
}

/** Keeps the snapshot in memory. For tests, previews and as a fallback. */
class InMemorySnapshotStore<T>(private var value: T? = null) : SnapshotStore<T> {
    override suspend fun read(): T? = value
    override suspend fun write(value: T) { this.value = value }
}

/**
 * Writes a repository's current snapshot in the background, one write at a time.
 * Each write reads the snapshot when it runs, so the last state always wins. Storage
 * errors are reported to [onError] and never reach gameplay: the in-memory state stays
 * authoritative and the next write retries.
 */
internal class WriteThrough<T>(
    private val store: SnapshotStore<T>,
    private val scope: CoroutineScope,
    private val onError: (Throwable) -> Unit,
    private val snapshot: () -> T,
) {
    private val mutex = Mutex()

    fun schedule() {
        scope.launch {
            mutex.withLock {
                runCatching { store.write(snapshot()) }.onFailure(onError)
            }
        }
    }
}

/** Reads a snapshot, treating storage errors as "nothing stored". */
internal suspend fun <T> SnapshotStore<T>.readOrNull(onError: (Throwable) -> Unit): T? =
    runCatching { read() }.onFailure(onError).getOrNull()
