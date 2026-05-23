package com.instacurator.app.pipeline

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Eyes-open probability and smile probability for the largest face in a photo. */
data class FaceMetrics(val eyesOpenProb: Float, val smileProb: Float)

/**
 * ML Kit face detection in accurate + classification mode (no landmarks — they
 * cost CPU and aren't needed for the smile / eyes-open probabilities we use).
 */
@Singleton
class FaceScorer @Inject constructor() {

	private val detector = FaceDetection.getClient(
		FaceDetectorOptions.Builder()
			.setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
			.setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
			.build()
	)

	suspend fun score(bitmap: Bitmap): FaceMetrics {
		val image = InputImage.fromBitmap(bitmap, 0)
		val faces: List<Face> = detector.process(image).await()
		if (faces.isEmpty()) return FaceMetrics(0f, 0f)
		val largest = faces.maxBy { it.boundingBox.width() * it.boundingBox.height() }
		val leftEye = largest.leftEyeOpenProbability ?: 0f
		val rightEye = largest.rightEyeOpenProbability ?: 0f
		val smile = largest.smilingProbability ?: 0f
		return FaceMetrics(eyesOpenProb = maxOf(leftEye, rightEye), smileProb = smile)
	}
}
