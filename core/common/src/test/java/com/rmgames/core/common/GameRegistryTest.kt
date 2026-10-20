package com.rmgames.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class GameRegistryTest {

    private fun fakeGame(id: String): Game = object : Game {
        override val id: String = id
        override val title: String = "Fake $id"
        override val description: String = "Test game"
        override val durationMs: Long = 30_000L
        override val higherWins: Boolean = true
        override fun start(scope: GameScope) = Unit
        override fun stop() = Unit
    }

    @Test
    fun `find returns the entry registered for a known game id`() {
        val entry = GameEntry(fakeGame("01-escape-poruba")) { }
        val registry = GameRegistry(listOf(entry))

        assertSame(entry, registry.find("01-escape-poruba"))
    }

    @Test
    fun `find returns null for an unknown game id`() {
        val registry = GameRegistry(emptyList())

        assertNull(registry.find("99-not-a-game"))
    }

    @Test
    fun `all exposes entries in registration order`() {
        val first = GameEntry(fakeGame("01-escape-poruba")) { }
        val second = GameEntry(fakeGame("02-onion-roll")) { }
        val registry = GameRegistry(listOf(first, second))

        assertEquals(
            listOf("01-escape-poruba", "02-onion-roll"),
            registry.all.map { it.game.id },
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate game ids are rejected at construction`() {
        GameRegistry(
            listOf(
                GameEntry(fakeGame("01-escape-poruba")) { },
                GameEntry(fakeGame("01-escape-poruba")) { },
            ),
        )
    }
}
