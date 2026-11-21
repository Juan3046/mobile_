package com.rmgames.feature.onboarding

import com.rmgames.core.data.ClaimResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NickMessagesTest {
    @Test
    fun `each refusal tells the player what to do`() {
        assertEquals("3–12 characters", errorFor(ClaimResult.Invalid("3–12 characters")))
        assertEquals("That nick is taken — try another", errorFor(ClaimResult.Taken))
        assertEquals("No connection. Choosing a nick needs internet once.", errorFor(ClaimResult.Offline))
    }

    @Test
    fun `success has no error`() {
        assertNull(errorFor(ClaimResult.Claimed("monica_rc")))
        assertNull(errorFor(ClaimResult.AlreadySet("monica_rc")))
    }

    @Test
    fun `counter shows trimmed length out of twelve`() {
        assertEquals("9/12", counterFor(" monica_rc "))
        assertEquals("0/12", counterFor(""))
    }
}
