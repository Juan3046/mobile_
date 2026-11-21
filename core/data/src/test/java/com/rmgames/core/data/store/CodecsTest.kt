package com.rmgames.core.data.store

import com.rmgames.core.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodecsTest {

    @Test
    fun `attempts round-trip`() {
        val v = mapOf(20410L to 1, 20411L to 2)
        assertEquals(v, AttemptsCodec.decode(AttemptsCodec.encode(v)))
    }

    @Test
    fun `scores round-trip with game ids containing dashes`() {
        val v = mapOf((20410L to "05-soda-shake") to 427L, (20411L to "03-blink-sniper") to 27L)
        assertEquals(v, ScoresCodec.decode(ScoresCodec.encode(v)))
    }

    @Test
    fun `settings round-trip`() {
        val v = Settings(musicVolume = 0.35f, sfxEnabled = false, vibrationEnabled = true)
        assertEquals(v, SettingsCodec.decode(SettingsCodec.encode(v)))
    }

    @Test
    fun `empty maps round-trip`() {
        assertEquals(emptyMap<Long, Int>(), AttemptsCodec.decode(AttemptsCodec.encode(emptyMap())))
        assertEquals(emptyMap<Pair<Long, String>, Long>(), ScoresCodec.decode(ScoresCodec.encode(emptyMap())))
    }

    @Test
    fun `payloads are versioned`() {
        assertEquals("v1|20410:1", AttemptsCodec.encode(mapOf(20410L to 1)))
    }

    @Test
    fun `corrupt or unknown-version data decodes to null instead of crashing`() {
        assertNull(AttemptsCodec.decode("garbage"))
        assertNull(AttemptsCodec.decode("v9|20410:1"))
        assertNull(ScoresCodec.decode("v1|20410,soda"))
        assertNull(SettingsCodec.decode("v1|music=loud"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `game ids with separators are rejected on encode`() {
        ScoresCodec.encode(mapOf((1L to "bad;id") to 1L))
    }
}
