package com.rmgames.core.vision

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.Executor

/**
 * Android Adapter: bridges CameraX + ML Kit face detection to the frozen
 * [BlinkDetector] contract. ML Kit types never leave this file — callers only
 * see [Flow]<[EyeState]>, and the camera-permission UX lives in `:app`.
 *
 * @param lifecycleOwner the screen that owns the camera (binds/unbinds with it).
 * @param classifier pure, injected blink logic (Strategy) — unit-tested on JVM.
 */
class MlKitBlinkDetector(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val classifier: BlinkClassifier = BlinkClassifier(BlinkClassifier.DEFAULT_OPEN_THRESHOLD),
) : BlinkDetector {

    private val appContext = context.applicationContext

    @ExperimentalGetImage
    override fun eyeState(): Flow<EyeState> = callbackFlow {
        val provider = ProcessCameraProvider.getInstance(appContext).await()
        val detector = FaceDetection.getClient(blinkFaceDetectorOptions())

        val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(mainExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees,
                    )
                    detector.process(input)
                        .addOnSuccessListener { faces ->
                            val face = faces.firstOrNull() ?: return@addOnSuccessListener
                            val state = classifier.classify(
                                left = face.leftEyeOpenProbability,
                                right = face.rightEyeOpenProbability,
                            )
                            trySend(state)
                        }
                        .addOnCompleteListener { imageProxy.close() }
                }
            }

        val selector = CameraSelector.DEFAULT_FRONT_CAMERA
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, analysis)

        awaitClose {
            provider.unbindAll()
            detector.close()
        }
    }.conflate().flowOn(Dispatchers.Default)
}

/**
 * Options for the single detector this adapter creates.
 *
 * Extracted so the two flags that decide whether Blink Sniper works at all are pinned
 * by a JVM test (AGENTS.md §5):
 *  - [FaceDetectorOptions.CLASSIFICATION_MODE_ALL] — without it ML Kit reports both
 *    eye probabilities as **null**, and [BlinkClassifier] maps a missing value to
 *    OPEN, so the detector claimed "eyes open" forever and no shot ever fired.
 *  - [FaceDetectorOptions.PERFORMANCE_MODE_FAST] — keeps latency down so a ~200 ms
 *    blink is still sampled while it happens.
 */
internal fun blinkFaceDetectorOptions(): FaceDetectorOptions =
    FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()
