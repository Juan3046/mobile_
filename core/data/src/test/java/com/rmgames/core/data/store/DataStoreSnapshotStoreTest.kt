package com.rmgames.core.data.store

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSnapshotStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `writes and reads back through a real DataStore file`() = runTest {
        val file = File(tmp.root, "rm.preferences_pb")
        val ds = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        val attempts = DataStoreSnapshotStore(ds, "attempts", AttemptsCodec)
        assertNull(attempts.read())
        attempts.write(mapOf(20410L to 2))
        assertEquals(mapOf(20410L to 2), attempts.read())
        // another key in the same file does not clash
        assertNull(DataStoreSnapshotStore(ds, "settings", SettingsCodec).read())
    }
}
