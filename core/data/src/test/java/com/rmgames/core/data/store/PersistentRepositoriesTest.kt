package com.rmgames.core.data.store

import com.rmgames.core.data.PersistentAttemptRepository
import com.rmgames.core.data.PersistentScoreRepository
import com.rmgames.core.data.Settings
import com.rmgames.core.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersistentRepositoriesTest {

    // ---- attempts ----

    @Test
    fun `consumed attempts survive a restart`() = runTest {
        val store = InMemorySnapshotStore<Map<Long, Int>>()
        PersistentAttemptRepository(store, backgroundScope).apply { load(); consume(10); consume(10) }
        runCurrent()
        val restarted = PersistentAttemptRepository(store, backgroundScope).apply { load() }
        assertEquals(0, restarted.remaining(10))
        assertFalse(restarted.consume(10))
    }

    @Test
    fun `debug reset is persisted too`() = runTest {
        val store = InMemorySnapshotStore<Map<Long, Int>>()
        PersistentAttemptRepository(store, backgroundScope).apply { load(); consume(10); reset(10) }
        runCurrent()
        assertEquals(2, PersistentAttemptRepository(store, backgroundScope).apply { load() }.remaining(10))
    }

    @Test
    fun `old days are pruned so storage stays tiny`() = runTest {
        val store = InMemorySnapshotStore(mapOf(1L to 2, 2L to 1))
        PersistentAttemptRepository(store, backgroundScope).apply { load(); consume(10) }
        runCurrent()
        assertEquals(mapOf(10L to 1), store.read())
    }

    @Test
    fun `nothing stored means a fresh day`() = runTest {
        val repo = PersistentAttemptRepository(InMemorySnapshotStore(), backgroundScope).apply { load() }
        assertEquals(2, repo.remaining(10))
    }

    // ---- scores ----

    @Test
    fun `daily bests survive a restart and keep best-of-2`() = runTest {
        val store = InMemorySnapshotStore<Map<Pair<Long, String>, Long>>()
        PersistentScoreRepository(store, backgroundScope).apply {
            load()
            submit(10, "05-soda-shake", 512, higherWins = false)
            submit(10, "05-soda-shake", 427, higherWins = false)
        }
        runCurrent()
        assertEquals(427L, PersistentScoreRepository(store, backgroundScope).apply { load() }.best(10, "05-soda-shake"))
    }

    @Test
    fun `the full history is kept, years back`() = runTest {
        val store = InMemorySnapshotStore(mapOf((1L to "g") to 5L))
        PersistentScoreRepository(store, backgroundScope).apply { load(); submit(1000, "g", 7, higherWins = true) }
        runCurrent()
        val restarted = PersistentScoreRepository(store, backgroundScope).apply { load() }
        assertEquals(5L, restarted.best(1, "g"))
        assertEquals(7L, restarted.best(1000, "g"))
    }

    // ---- settings ----

    @Test
    fun `settings start at the defaults`() = runTest {
        val repo = SettingsRepository(InMemorySnapshotStore(), backgroundScope).apply { load() }
        assertEquals(Settings(), repo.settings.value)
    }

    @Test
    fun `settings updates are observable and persisted`() = runTest {
        val store = InMemorySnapshotStore<Settings>()
        val repo = SettingsRepository(store, backgroundScope).apply { load() }
        repo.update { it.copy(vibrationEnabled = false) }
        assertFalse(repo.settings.value.vibrationEnabled)
        runCurrent()
        assertFalse(SettingsRepository(store, backgroundScope).apply { load() }.settings.value.vibrationEnabled)
    }

    @Test
    fun `music volume is clamped to 0 to 1`() = runTest {
        val repo = SettingsRepository(InMemorySnapshotStore(), backgroundScope).apply { load() }
        repo.update { it.copy(musicVolume = 3f) }
        assertEquals(1f, repo.settings.value.musicVolume)
    }

    // ---- write-through ----

    @Test
    fun `the last write wins even when writes overlap`() = runTest {
        val store = InMemorySnapshotStore<Map<Long, Int>>()
        val repo = PersistentAttemptRepository(store, backgroundScope).apply { load() }
        repo.consume(10); repo.reset(10); repo.consume(10)
        runCurrent()
        assertEquals(mapOf(10L to 1), store.read())
    }

    @Test
    fun `a failing store never breaks gameplay`() = runTest {
        val broken = object : SnapshotStore<Map<Long, Int>> {
            override suspend fun read(): Map<Long, Int>? = error("disk gone")
            override suspend fun write(value: Map<Long, Int>) = error("disk gone")
        }
        val repo = PersistentAttemptRepository(broken, backgroundScope).apply { load() }
        repo.consume(10)
        runCurrent()
        assertEquals(1, repo.remaining(10))
    }
}
