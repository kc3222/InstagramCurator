package com.instacurator.app.pipeline

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The end-to-end on-device pipeline. Owned as a singleton so that the ML Kit
 * detector inside [FaceScorer] is initialized once.
 *
 * Pipeline shape (per URI, sequential to bound memory):
 *   1. Decode + downscale.
 *   2. Sharpness filter (drop below SHARPNESS_THRESHOLD).
 *   3. For survivors: pHash, exposure, ML Kit face metrics — all reusing the
 *      same already-decoded bitmap before it's recycled.
 *   4. After the loop: cluster by pHash, keep the composite-score winner of
 *      each cluster.
 */
@Singleton
class PipelineProcessor @Inject constructor(
	@ApplicationContext private val context: Context,
	private val faceScorer: FaceScorer,
) {
	@Volatile private var openCvReady = false

	suspend fun process(
		uris: List<Uri>,
		onProgress: (stage: String, progress: Float) -> Unit = { _, _ -> },
	): List<CandidatePhoto> = withContext(Dispatchers.Default) {
		ensureOpenCvLoaded()
		if (uris.isEmpty()) return@withContext emptyList()

		val candidates = mutableListOf<CandidatePhoto>()

		uris.forEachIndexed { idx, uri ->
			onProgress("Analyzing", idx.toFloat() / uris.size)

			val bitmap = withContext(Dispatchers.IO) {
				BitmapDecoder.decodeDownscaled(context, uri)
			} ?: return@forEachIndexed

			try {
				val sharpness = SharpnessFilter.sharpnessVariance(bitmap)
				if (sharpness < SHARPNESS_THRESHOLD) return@forEachIndexed

				val phash = PerceptualHash.pHash(bitmap)
				val exposure = ExposureScorer.exposureQuality(bitmap)
				val face = faceScorer.score(bitmap)

				val score = composite(sharpness, face.eyesOpenProb, face.smileProb, exposure)
				candidates.add(
					CandidatePhoto(
						uri = uri,
						sharpness = sharpness,
						eyesOpenProb = face.eyesOpenProb,
						smileProb = face.smileProb,
						exposureQuality = exposure,
						phash = phash,
						compositeScore = score,
					)
				)
			} finally {
				if (!bitmap.isRecycled) bitmap.recycle()
			}
		}

		onProgress("Removing duplicates", 0.95f)
		val clusters = Clustering.clusterByPHash(candidates)
		clusters.map { Clustering.pickClusterWinner(it) }
	}

	private fun ensureOpenCvLoaded() {
		if (openCvReady) return
		check(OpenCVLoader.initLocal()) { "OpenCV initLocal failed" }
		openCvReady = true
		Log.i(TAG, "OpenCV initialized")
	}

	private companion object {
		const val TAG = "PipelineProcessor"
	}
}
