package com.rmgames.app

import android.Manifest
import com.rmgames.core.common.GameRegistry
import com.rmgames.core.sensors.FakeAccelerometerSensor
import com.rmgames.core.sensors.FakeCompassSensor
import com.rmgames.core.sensors.FakeGyroscopeSensor
import com.rmgames.core.vision.FakeBlinkDetector
import com.rmgames.core.vision.FakeHeadGestureDetector
import com.rmgames.feature.game.blink.blinkSniperEntry
import com.rmgames.feature.game.compass.compassChaosEntry
import com.rmgames.feature.game.casino.porubaCasinoEntry
import com.rmgames.feature.game.onion.onionRollEntry
import com.rmgames.feature.game.soda.sodaShakeEntry
import com.rmgames.feature.game.rush.classRushEntry
import com.rmgames.feature.game.sound.FakeSoundSensor
import com.rmgames.feature.game.sound.soundBarrierEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the Phase 1b wiring contract: the composition root in MainActivity wires
 * exactly the six daily games with their frozen entry factories. If any game
 * renames its id or factory, this fails before the shared `:app` breaks at runtime.
 */
class GameRegistryWiringTest {

    /** Same wiring as MainActivity, with fakes instead of Android adapters. */
    private fun registry(): GameRegistry = GameRegistry(
        listOf(
            porubaCasinoEntry(FakeHeadGestureDetector()),
            onionRollEntry(FakeGyroscopeSensor()),
            blinkSniperEntry(FakeBlinkDetector()),
            compassChaosEntry(FakeCompassSensor()),
            sodaShakeEntry(FakeAccelerometerSensor()),
            classRushEntry(),
            soundBarrierEntry(FakeSoundSensor()),
        ),
    )

    @Test
    fun `registry exposes the seven daily games with their frozen ids`() {
        val registry = registry()

        assertEquals(
            listOf(
                "01-poruba-casino",
                "02-onion-roll",
                "03-blink-sniper",
                "04-compass-chaos",
                "05-soda-shake",
                "06-class-rush",
                "07-sound-barrier",
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
    fun `camera games declare the camera runtime permission`() {
        val registry = registry()

        val blink = registry.find("03-blink-sniper")
        assertNotNull(blink)
        assertEquals(listOf(Manifest.permission.CAMERA), blink!!.requiredPermissions)

        val casino = registry.find("01-poruba-casino")
        assertNotNull(casino)
        assertEquals(listOf(Manifest.permission.CAMERA), casino!!.requiredPermissions)

        registry.all
            .filter { it.game.id !in setOf("01-poruba-casino", "03-blink-sniper", "07-sound-barrier") }
            .forEach { entry ->
                assertTrue(
                    "${entry.game.id} must not need the camera, got ${entry.requiredPermissions}",
                    Manifest.permission.CAMERA !in entry.requiredPermissions,
                )
            }
    }

    @Test
    fun `only sound barrier declares the microphone runtime permission`() {
        val registry = registry()

        val sound = registry.find("07-sound-barrier")
        assertNotNull(sound)
        assertEquals(listOf(Manifest.permission.RECORD_AUDIO), sound!!.requiredPermissions)

        registry.all
            .filter { it.game.id != "07-sound-barrier" }
            .forEach { entry ->
                assertTrue(
                    "${entry.game.id} must not need the microphone, got ${entry.requiredPermissions}",
                    Manifest.permission.RECORD_AUDIO !in entry.requiredPermissions,
                )
            }
    }

    @Test
    fun `every registered game ships its own thumbnail art (Decision 5)`() {
        registry().all.forEach { entry ->
            assertNotNull("${entry.game.id} has no thumbnail", entry.thumbnail)
        }
    }
}
