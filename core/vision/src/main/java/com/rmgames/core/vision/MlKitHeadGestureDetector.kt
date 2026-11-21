package com.rmgames.core.vision

import android.content.Context
import android.os.SystemClock
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

/** CameraX/ML Kit adapter; only head-pose observations cross into game modules. */
class MlKitHeadGestureDetector(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val classifier: HeadGestureClassifier = HeadGestureClassifier(),
) : HeadGestureDetector {
    private val appContext = context.applicationContext

    @ExperimentalGetImage
    override fun observations(): Flow<HeadGestureObservation> = callbackFlow {
        val provider = withContext(Dispatchers.Main.immediate) {
            ProcessCameraProvider.getInstance(appContext).await()
        }
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build(),
        )
        val executor: Executor = ContextCompat.getMainExecutor(appContext)
        var faceWasVisible = false
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(executor) { proxy ->
                    val mediaImage = proxy.image
                    if (mediaImage == null) {
                        trySend(HeadGestureObservation.TrackingError)
                        proxy.close()
                        return@setAnalyzer
                    }
                    detector.process(InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { faces ->
                            val face = faces.firstOrNull()
                            if (face == null) {
                                if (faceWasVisible) trySend(HeadGestureObservation.FaceLost)
                                faceWasVisible = false
                                classifier.reset()
                            } else {
                                if (!faceWasVisible) trySend(HeadGestureObservation.FaceTracked(face.headEulerAngleX, face.headEulerAngleY))
                                faceWasVisible = true
                                classifier.classify(
                                    face.headEulerAngleX,
                                    face.headEulerAngleY,
                                    SystemClock.elapsedRealtime(),
                                )?.let { trySend(HeadGestureObservation.Gesture(it)) }
                            }
                        }
                        .addOnFailureListener { trySend(HeadGestureObservation.TrackingError) }
                        .addOnCompleteListener { proxy.close() }
                }
            }

        withContext(Dispatchers.Main.immediate) {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
        }
        awaitClose {
            executor.execute {
                provider.unbindAll()
                detector.close()
                classifier.reset()
            }
        }
    }.catch { emit(HeadGestureObservation.TrackingError) }.flowOn(Dispatchers.Default)
}
