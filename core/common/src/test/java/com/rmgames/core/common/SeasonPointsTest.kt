package com.rmgames.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonPointsTest {

    @Test
    fun `4 players award 100 75 50 25`() {
        assertEquals(100, SeasonPoints.pointsFor(position = 1, playerCount = 4))
        assertEquals(75, SeasonPoints.pointsFor(position = 2, playerCount = 4))
        assertEquals(50, SeasonPoints.pointsFor(position = 3, playerCount = 4))
        assertEquals(25, SeasonPoints.pointsFor(position = 4, playerCount = 4))
    }

    @Test
    fun `6 players start at 150 stepping 25`() {
        assertEquals(150, SeasonPoints.pointsFor(position = 1, playerCount = 6))
        assertEquals(125, SeasonPoints.pointsFor(position = 2, playerCount = 6))
        assertEquals(25, SeasonPoints.pointsFor(position = 6, playerCount = 6))
    }

    @Test
    fun `single player still earns last-place points`() {
        assertEquals(25, SeasonPoints.pointsFor(position = 1, playerCount = 1))
    }

    @Test
    fun `higher score ranks first`() {
        val positions = SeasonPoints.positions(
            mapOf("a" to 9000L, "b" to 8000L, "c" to 7000L),
            higherWins = true
        )
        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 3), positions)
    }

    @Test
    fun `ties share position and next position is skipped`() {
        val positions = SeasonPoints.positions(
            mapOf("a" to 100L, "b" to 80L, "c" to 80L, "d" to 60L),
            higherWins = true
        )
        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 2, "d" to 4), positions)
    }

    @Test
    fun `lower wins orders fastest first`() {
        val positions = SeasonPoints.positions(
            mapOf("fast" to 300L, "slow" to 500L),
            higherWins = false
        )
        assertEquals(mapOf("fast" to 1, "slow" to 2), positions)
    }
}
