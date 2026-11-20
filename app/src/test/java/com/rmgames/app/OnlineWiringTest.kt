package com.rmgames.app

import com.rmgames.core.data.LocalNickRegistry
import com.rmgames.core.data.NickRegistry
import com.rmgames.core.data.Reservation
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineWiringTest {
    private val online = object : NickRegistry {
        override suspend fun reserve(nick: String) = Reservation.Reserved
    }

    @Test
    fun `with a Firebase config the online registry is used`() {
        assertSame(online, chooseNickRegistry(firebaseConfigured = true) { online })
    }

    @Test
    fun `without a Firebase config the app stays on the local stand-in and never builds the online one`() {
        val chosen = chooseNickRegistry(firebaseConfigured = false) { error("must not be built") }
        assertTrue(chosen is LocalNickRegistry)
    }
}
