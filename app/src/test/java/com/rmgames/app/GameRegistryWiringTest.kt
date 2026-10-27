package com.rmgames.app

import android.Manifest
import com.rmgames.core.common.GameRegistry
import com.rmgames.core.sensors.FakeAccelerometerSensor
import com.rmgames.core.sensors.FakeCompassSensor
import com.rmgames.core.sensors.FakeGyroscopeSensor
import com.rmgames.core.vision.FakeBlinkDetector
import com.rmgames.feature.game.blink.blinkSniperEntry
import com.rmgames.feature.game.compass.compassChaosEntry
import com.rmgames.feature.game.escape.escapePorubaEntry
import com.rmgames.feature.game.onion.onionRollEntry
import com.rmgames.feature.game.soda.sodaShakeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the Phase 1b wiring contract: the composition root in MainActivity wires
 * exactly the five daily games with their frozen entry factories. If any game
 * renames its id or factory, this fails before the shared `:app` breaks at runtime.
 */
class GameRegistryWiringTest {

    /** Same wiring as MainActivity, with fakes instead of Android adapters. */
    private fun registry(): GameRegistry = GameRegistry(
        listOf(
            escapePorubaEntry(),
            onionRollEntry(FakeGyroscopeSensor()),
            blinkSniperEntry(FakeBlinkDetector()),
            compassChaosEntry(FakeCompassSensor()),
            sodaShakeEntry(FakeAccelerometerSensor()),
        ),
    )

    @Test
    fun `registry exposes the five daily games with their frozen ids`() {
        val registry = registry()

        assertEquals(
            listOf(
                "01-escape-poruba",
                "02-onion-roll",
                "03-blink-sniper",
                "04-compass-chaos",
                "05-soda-shake",
            ),
            registry.all.map { it.game.id },
        )
    }

    @Test
    fun `every registered game resolves through find for the session route`() {
        val registry = registry()

        registry.all.forEach { entry ->
            assertNotNull("session/{gameId} must resolve ${entry.game.id}", registry.find(entry.game.id))
        }
    }

    @Test
    fun `only blink declares the camera runtime permission`() {
        val registry = registry()

        val blink = registry.find("03-blink-sniper")
        assertNotNull(blink)
        assertEquals(listOf(Manifest.permission.CAMERA), blink!!.requiredPermissions)

        registry.all
            .filter { it.game.id != "03-blink-sniper" }
            .forEach { entry ->
                assertTrue(
                    "${entry.game.id} needs no runtime permission, got ${entry.requiredPermissions}",
                    entry.requiredPermissions.isEmpty(),
                )
            }
    }
}
