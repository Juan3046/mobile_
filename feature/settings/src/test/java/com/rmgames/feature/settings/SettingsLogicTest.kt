package com.rmgames.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsLogicTest {

    @Test
    fun `privacy note tells the truth with and without the online ranking`() {
        val online = privacyNote(online = true)
        assertEquals("Anonymous, no personal data", online.first)
        assertTrue(online.second.contains("nick and each day's best score"))
        assertEquals("Your data stays on this phone", privacyNote(online = false).first)
    }

    @Test
    fun `volume shows as a whole percentage`() {
        assertEquals("70%", volumeLabel(0.7f))
        assertEquals("0%", volumeLabel(0f))
        assertEquals("100%", volumeLabel(1f))
        assertEquals("33%", volumeLabel(0.334f))
    }

    @Test
    fun `volume label is Off at zero for screen readers too`() {
        assertEquals("Music off", volumeDescription(0f))
        assertEquals("Music volume 70%", volumeDescription(0.7f))
    }

    @Test
    fun `every bundled font is credited with its licence`() {
        assertEquals(listOf("Bungee", "Rubik", "JetBrains Mono"), Credits.fonts.map { it.name })
        Credits.fonts.forEach {
            assertEquals("SIL Open Font License 1.1", it.licence)
            assertTrue(it.copyright.startsWith("©"))
        }
    }

    @Test
    fun `in-app credits stay in sync with CREDITS md`() {
        // Decision 6: CREDITS.md and the Settings credits page list the same assets.
        val credits = File("../../CREDITS.md").readText()
        Credits.fonts.forEach { assertTrue("${it.name} missing from CREDITS.md", it.name in credits) }
        assertTrue(Credits.fonts.size >= Regex("""\.ttf`""").findAll(credits).count())
    }
}
