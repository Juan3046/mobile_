package com.rmgames.core.online

import com.rmgames.core.data.Reservation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OnlineNickRegistryTest {

    private class FakeAuth(private val uid: String = "uid-1", private val fail: Boolean = false) : AnonymousAuth {
        var calls = 0
        override suspend fun uid(): String { calls++; if (fail) error("network down"); return uid }
    }

    /** In-memory `nicks/{key}` = owner uid. */
    private class FakeDirectory(val owners: MutableMap<String, String> = mutableMapOf(), val fail: Boolean = false) : NickDirectory {
        val writes = mutableListOf<Triple<String, String, String>>()
        override suspend fun reserve(key: String, nick: String, uid: String): DirectoryOutcome {
            if (fail) error("firestore unavailable")
            val owner = owners[key]
            return when {
                owner == null -> { owners[key] = uid; writes += Triple(key, nick, uid); DirectoryOutcome.Reserved }
                owner == uid -> DirectoryOutcome.AlreadyMine
                else -> DirectoryOutcome.TakenByOther
            }
        }
    }

    @Test
    fun `a free nick is reserved under its lowercase key for this player`() = runTest {
        val dir = FakeDirectory()
        assertEquals(Reservation.Reserved, OnlineNickRegistry(FakeAuth(), dir).reserve("  Monica_RC "))
        assertEquals(listOf(Triple("monica_rc", "Monica_RC", "uid-1")), dir.writes)
    }

    @Test
    fun `a nick owned by another player is taken, whatever the case`() = runTest {
        val dir = FakeDirectory(mutableMapOf("pixel_pau" to "someone-else"))
        assertEquals(Reservation.Taken, OnlineNickRegistry(FakeAuth(), dir).reserve("PIXEL_pau"))
    }

    @Test
    fun `retrying after a dropped answer finds the nick already ours`() = runTest {
        val dir = FakeDirectory(mutableMapOf("monica_rc" to "uid-1"))
        assertEquals(Reservation.Reserved, OnlineNickRegistry(FakeAuth(), dir).reserve("monica_rc"))
    }

    @Test
    fun `no sign-in means offline, and the directory is never touched`() = runTest {
        val dir = FakeDirectory()
        assertEquals(Reservation.Offline, OnlineNickRegistry(FakeAuth(fail = true), dir).reserve("monica_rc"))
        assertEquals(emptyList<Any>(), dir.writes)
    }

    @Test
    fun `a Firestore failure means offline`() = runTest {
        assertEquals(Reservation.Offline, OnlineNickRegistry(FakeAuth(), FakeDirectory(fail = true)).reserve("monica_rc"))
    }

    @Test
    fun `a hanging network call times out as offline`() = runTest {
        val slow = object : NickDirectory {
            override suspend fun reserve(key: String, nick: String, uid: String): DirectoryOutcome = awaitCancellation()
        }
        assertEquals(Reservation.Offline, OnlineNickRegistry(FakeAuth(), slow, timeoutMs = 1_000).reserve("monica_rc"))
    }

    @Test(expected = CancellationException::class)
    fun `cancellation of the caller is not swallowed`() = runTest {
        val cancelling = object : AnonymousAuth {
            override suspend fun uid(): String = throw CancellationException("screen left")
        }
        OnlineNickRegistry(cancelling, FakeDirectory()).reserve("monica_rc")
    }
}
