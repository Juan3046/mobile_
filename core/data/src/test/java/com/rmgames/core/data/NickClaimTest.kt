package com.rmgames.core.data

import com.rmgames.core.data.store.InMemorySnapshotStore
import com.rmgames.core.data.store.PlayerCodec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NickClaimTest {

    /** Records reservations; [taken] holds keys already reserved by others. */
    private class FakeRegistry(val taken: Set<String> = emptySet(), val offline: Boolean = false) : NickRegistry {
        val calls = mutableListOf<String>()
        override suspend fun reserve(nick: String): Reservation {
            calls += nick
            return when {
                offline -> Reservation.Offline
                nickKey(nick) in taken -> Reservation.Taken
                else -> Reservation.Reserved
            }
        }
    }

    @Test
    fun `nick problems explain the rule in plain words`() {
        assertEquals("3–12 characters", nickProblem("ab"))
        assertEquals("3–12 characters", nickProblem("abcdefghijklm"))
        assertEquals("Only letters, numbers and _", nickProblem("bad nick"))
        assertEquals("Only letters, numbers and _", nickProblem("ñandú"))
        assertNull(nickProblem("monica_rc"))
    }

    @Test
    fun `uniqueness ignores case and surrounding spaces`() {
        assertEquals("monica_rc", nickKey("  Monica_RC "))
    }

    @Test
    fun `a valid free nick is reserved and saved as typed`() = runTest {
        val players = PlayerRepository(InMemorySnapshotStore(), backgroundScope).apply { load() }
        val registry = FakeRegistry()
        val result = NickClaim(registry, players).claim("  Monica_RC ")
        assertEquals(ClaimResult.Claimed("Monica_RC"), result)
        assertEquals("Monica_RC", players.nick.value)
        assertEquals(listOf("Monica_RC"), registry.calls)
    }

    @Test
    fun `an invalid nick never reaches the registry`() = runTest {
        val players = PlayerRepository(InMemorySnapshotStore(), backgroundScope).apply { load() }
        val registry = FakeRegistry()
        assertEquals(ClaimResult.Invalid("3–12 characters"), NickClaim(registry, players).claim("ab"))
        assertTrue(registry.calls.isEmpty())
        assertNull(players.nick.value)
    }

    @Test
    fun `a taken nick is refused and nothing is saved`() = runTest {
        val players = PlayerRepository(InMemorySnapshotStore(), backgroundScope).apply { load() }
        val result = NickClaim(FakeRegistry(taken = setOf("pixel_pau")), players).claim("Pixel_Pau")
        assertEquals(ClaimResult.Taken, result)
        assertNull(players.nick.value)
    }

    @Test
    fun `no connection keeps the player on the screen`() = runTest {
        val players = PlayerRepository(InMemorySnapshotStore(), backgroundScope).apply { load() }
        assertEquals(ClaimResult.Offline, NickClaim(FakeRegistry(offline = true), players).claim("monica_rc"))
        assertNull(players.nick.value)
    }

    @Test
    fun `a nick is set once and never edited`() = runTest {
        val players = PlayerRepository(InMemorySnapshotStore(), backgroundScope).apply { load() }
        val claim = NickClaim(FakeRegistry(), players)
        claim.claim("monica_rc")
        assertEquals(ClaimResult.AlreadySet("monica_rc"), claim.claim("another"))
        assertEquals("monica_rc", players.nick.value)
    }

    @Test
    fun `the nick survives a restart`() = runTest {
        val store = InMemorySnapshotStore<PlayerProfile>()
        NickClaim(FakeRegistry(), PlayerRepository(store, backgroundScope).apply { load() }).claim("monica_rc")
        runCurrent()
        assertEquals("monica_rc", PlayerRepository(store, backgroundScope).apply { load() }.nick.value)
    }

    @Test
    fun `player profile codec round-trips and rejects junk`() {
        val p = PlayerProfile(nick = "monica_rc")
        assertEquals(p, PlayerCodec.decode(PlayerCodec.encode(p)))
        assertEquals(PlayerProfile(nick = null), PlayerCodec.decode(PlayerCodec.encode(PlayerProfile(null))))
        assertNull(PlayerCodec.decode("garbage"))
    }

    @Test
    fun `the local registry accepts any valid nick until Firebase is connected`() = runTest {
        assertEquals(Reservation.Reserved, LocalNickRegistry().reserve("monica_rc"))
    }

    @Test
    fun `debug forget clears the local nick so onboarding shows again`() = runTest {
        val store = InMemorySnapshotStore<PlayerProfile>()
        val players = PlayerRepository(store, backgroundScope).apply { load() }
        NickClaim(FakeRegistry(), players).claim("monica_rc")
        players.forgetNickForDebug()
        runCurrent()
        assertNull(players.nick.value)
        assertNull(PlayerRepository(store, backgroundScope).apply { load() }.nick.value)
    }

    /** Regression (device, 20 Nov): after "Forget nick", a new nick was refused as "No connection". */
    @Test
    fun `a player who already owns a nick gets it back instead of a new one`() = runTest {
        val players = PlayerRepository(InMemorySnapshotStore(), backgroundScope).apply { load() }
        val owns = object : NickRegistry {
            override suspend fun reserve(nick: String) = Reservation.AlreadyHas("Acido101")
        }
        assertEquals(ClaimResult.Claimed("Acido101"), NickClaim(owns, players).claim("Akex"))
        assertEquals("Acido101", players.nick.value)
    }
}
