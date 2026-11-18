package com.rmgames.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.rmgames.core.data.DayClock
import com.rmgames.core.data.PersistentAttemptRepository
import com.rmgames.core.data.PersistentScoreRepository
import com.rmgames.core.data.SettingsRepository
import com.rmgames.core.data.store.AttemptsCodec
import com.rmgames.core.data.store.DataStoreSnapshotStore
import com.rmgames.core.data.store.ScoresCodec
import com.rmgames.core.data.store.SettingsCodec
import com.rmgames.core.data.store.rmDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Holds the process-wide [AppContainer] so it outlives activity recreation. */
class RmApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/**
 * Composition root for app-wide state (plan 09 PR 6). **This is where storage is chosen**:
 * each repository gets a `SnapshotStore`. Today they are on-device DataStore stores; to
 * keep data in the cloud, build a remote (or local + remote) `SnapshotStore` and pass it
 * here — nothing else changes (docs/04-tech-stack.md "Data layer"). Attempts stay local
 * by product rule (docs/02); scores and settings are the candidates for the cloud.
 */
class AppContainer(context: Context) {
    /** Background scope for storage writes; lives as long as the process. */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = context.applicationContext.rmDataStore
    private val logError: (Throwable) -> Unit = { Log.w("RmData", "storage error", it) }

    val clock = DayClock()

    val attempts = PersistentAttemptRepository(DataStoreSnapshotStore(dataStore, "attempts", AttemptsCodec), ioScope, logError)
    val scores = PersistentScoreRepository(DataStoreSnapshotStore(dataStore, "daily_bests", ScoresCodec), ioScope, logError)
    val settings = SettingsRepository(DataStoreSnapshotStore(dataStore, "settings", SettingsCodec), ioScope, logError)

    private val loadLock = Mutex()

    @Volatile
    var isLoaded = false
        private set

    /** Reads every repository once (a few ms from disk). Safe to call repeatedly. */
    suspend fun load() = loadLock.withLock {
        if (isLoaded) return@withLock
        attempts.load()
        scores.load()
        settings.load()
        isLoaded = true
    }
}
