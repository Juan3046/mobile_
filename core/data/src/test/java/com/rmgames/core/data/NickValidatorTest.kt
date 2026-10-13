package com.rmgames.core.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NickValidatorTest {

    @Test
    fun `typical nick is valid`() {
        assertTrue(NickValidator.isValid("Player_1"))
    }

    @Test
    fun `too short is rejected`() {
        assertFalse(NickValidator.isValid("ab"))
    }

    @Test
    fun `too long is rejected`() {
        assertFalse(NickValidator.isValid("a".repeat(13)))
    }

    @Test
    fun `spaces and symbols are rejected`() {
        assertFalse(NickValidator.isValid("my nick"))
        assertFalse(NickValidator.isValid("nick!"))
        assertFalse(NickValidator.isValid(""))
    }
}
