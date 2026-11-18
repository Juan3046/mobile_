package com.rmgames.core.vision

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineDispatcher
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
    override fun eyeState(): Flow<EyeState> = eyeStateFlow(Dispatchers.Main) {
        AndroidCameraRig(appContext, lifecycleOwner, classifier)
    }
}

/**
 * CameraX operations for one collection of the eye-state flow.
 *
 * Every method is invoked on the flow's own dispatcher — see [eyeStateFlow].
 */
internal interface CameraRig {
    /** Binds the camera and starts feeding eye states into [emit]. */
    suspend fun bind(emit: (EyeState) -> Unit)

    /** Releases the camera. Called when the flow is cancelled. */
    fun unbind()

    /** Releases ML Kit. Called when the flow is cancelled. */
    fun close()
}

/**
 * Builds the eye-state flow: [open] creates a fresh [CameraRig] per collection,
 * and every rig call is routed to [main].
 *
 * Extracted from the adapter so the thread-affinity rule can be pinned by a JVM
 * test: CameraX throws `IllegalStateException: Not in application's main thread`
 * if `unbindAll()` / `bindToLifecycle()` run anywhere else (issue #22), so the
 * whole upstream — bind, emit and the `awaitClose` unbind — must stay on [main].
 * Production passes [Dispatchers.Main]; the test passes a test dispatcher to
 * observe exactly where the CameraX calls land.
 */
internal fun eyeStateFlow(
    main: CoroutineDispatcher,
    open: () -> CameraRig,
): Flow<EyeState> = callbackFlow {
    val rig = open()
    rig.bind { state -> trySend(state) }
    awaitClose {
        rig.unbind()
        rig.close()
    }
}.conflate().flowOn(main)

/** Real CameraX + ML Kit rig, created fresh for each collection. */
@ExperimentalGetImage
private class AndroidCameraRig(
    private val appContext: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val classifier: BlinkClassifier,
) : CameraRig {

    private var provider: ProcessCameraProvider? = null
    private var detector: FaceDetector? = null

    override suspend fun bind(emit: (EyeState) -> Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(appContext).await()
        val faceDetector = FaceDetection.getClient(blinkFaceDetectorOptions())
        provider = cameraProvider
        detector = faceDetector

        val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)

        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(ANALYSIS_WIDTH, ANALYSIS_HEIGHT),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                        ),
                    )
                    .build(),
            )
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
                    faceDetector.process(input)
                        .addOnSuccessListener { faces ->
                            val face = faces.firstOrNull() ?: return@addOnSuccessListener
                            val state = classifier.classify(
                                left = face.leftEyeOpenProbability,
                                right = face.rightEyeOpenProbability,
                            )
                            emit(state)
                        }
                        .addOnCompleteListener { imageProxy.close() }
                }
            }

        val selector = CameraSelector.DEFAULT_FRONT_CAMERA
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, analysis)
    }

    override fun unbind() {
        provider?.unbindAll()
    }

    override fun close() {
        detector?.close()
    }
}

/**
 * Analysis resolution for the blink detector.
 *
 * ML Kit's per-frame processing time is what caps the sampling rate, and sampling has
 * to stay faster than a blink (~200-400 ms) or the CLOSED state is never observed at
 * all — the shot is an OPEN -> CLOSED edge, so a missed sample is a missed shot.
 *
 * Measured on the emulator webcam before this change: the 640x480 default yielded
 * **2.78 samples/s** (median 327 ms between them, max 1300 ms) against blinks of
 * 200-400 ms, so most blinks fell in the gap between two samples. Playtest symptom:
 * "the CAM box almost never changes to the closed line"; only 6 blinks were caught
 * in a 48.6 s run.
 *
 * At 320x240 — a quarter of the pixels — the same run measured **8.57 samples/s**
 * (median 88 ms, max 905 ms) and 14 blinks caught. The face fills a large share of a
 * front-camera frame (measured bounding boxes of 160x233 up to 248x432 at 640x480),
 * so it stays comfortably large enough for ML Kit's eye classification.
 */
private const val ANALYSIS_WIDTH = 320
private const val ANALYSIS_HEIGHT = 240

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
