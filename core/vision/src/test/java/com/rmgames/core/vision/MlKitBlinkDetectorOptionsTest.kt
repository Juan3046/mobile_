package com.rmgames.core.vision

import com.google.mlkit.vision.face.FaceDetectorOptions
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Regression guard for the half of the bug that made Blink Sniper never fire.
 *
 * ML Kit leaves `leftEyeOpenProbability` / `rightEyeOpenProbability` **null** unless
 * eye classification is explicitly requested, and [BlinkClassifier] maps a missing
 * probability to OPEN — so without [blinkFaceDetectorOptions] the detector reported
 * "eyes open" forever, no OPEN -> CLOSED edge ever existed and the score stayed at 0
 * (docs/games/03-blink-sniper.md).
 *
 * The runtime artifact behind [FaceDetectorOptions] (`play-services-mlkit-face-detection`)
 * exposes no getters and obfuscates its fields, so the only way to observe what was
 * actually requested is to read the private mode ints back and diff them against the
 * ML Kit defaults.
 */
class MlKitBlinkDetectorOptionsTest {

    @Test
    fun `eye classification is the only deviation from the ML Kit defaults`() {
        val requested = blinkFaceDetectorOptions()
        val defaults = FaceDetectorOptions.Builder().build()

        val diffs = modeInts(requested).zip(modeInts(defaults))
            .filter { (asked, default) -> asked != default }
            .map { (asked, _) -> asked }

        assertEquals(
            "expected classification mode to be the only thing we request from ML Kit",
            listOf(FaceDetectorOptions.CLASSIFICATION_MODE_ALL),
            diffs,
        )
    }

    /** The four private mode ints of a built options object, in declaration order. */
    private fun modeInts(options: FaceDetectorOptions): List<Int> =
        options.javaClass.declaredFields
            .filter { it.type == Integer.TYPE && !Modifier.isStatic(it.modifiers) }
            .map { field ->
                field.isAccessible = true
                field.getInt(options)
            }
}
