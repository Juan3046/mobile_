package com.rmgames.core.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** Local [SnapshotStore]: one string entry in a Preferences DataStore, encoded by [codec]. */
class DataStoreSnapshotStore<T>(
    private val dataStore: DataStore<Preferences>,
    key: String,
    private val codec: SnapshotCodec<T>,
) : SnapshotStore<T> {
    private val prefKey = stringPreferencesKey(key)

    override suspend fun read(): T? = dataStore.data.first()[prefKey]?.let(codec::decode)

    override suspend fun write(value: T) {
        dataStore.edit { it[prefKey] = codec.encode(value) }
    }
}

/** The app's single on-device DataStore file (`rm_games.preferences_pb`). */
val Context.rmDataStore: DataStore<Preferences> by preferencesDataStore(name = "rm_games")
