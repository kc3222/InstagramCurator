package com.instacurator.app.pipeline

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.instacurator.app.network.CohesiveSelectRequest
import com.instacurator.app.network.CuratorApiService
import com.instacurator.app.network.ImagePayload
import com.instacurator.app.network.ScoreBatchRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The end-to-end on-device pipeline plus the AI round-trip to the OpenAI proxy.
 *
 * Phase 2B `process()`: decode → sharpness → per-photo features → cluster.
 * Phase 3 `runAiPipeline()`: compress 600px → /score-batch (4 concurrent) →
 *   top 20 → compress 1200px → /cohesive-select → ordered final URIs.
 */
@Singleton
class PipelineProcessor @Inject constructor(
	@ApplicationContext private val context: Context,
	private val faceScorer: FaceScorer,
	private val api: CuratorApiService,
) {
	@Volatile private var openCvReady = false

	// --- Phase 2B (unchanged) ------------------------------------------------

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

	// --- Phase 3 -------------------------------------------------------------

	/**
	 * AI round-trip. Returns final URIs in the order the backend selected them.
	 *
	 * Stages emitted via [onState]:
	 *   AiScoring(0..1) — compression + parallel /score-batch
	 *   AiSelecting(0..1) — /cohesive-select
	 */
	suspend fun runAiPipeline(
		candidates: List<CandidatePhoto>,
		pickCount: Int,
		onState: (PipelineState) -> Unit,
	): List<Uri> = withContext(Dispatchers.Default) {
		if (candidates.isEmpty()) return@withContext emptyList()
		onState(PipelineState.AiScoring(0f))

		// 1. Compress all candidates to 600px.
		val compressed600 = coroutineScope {
			candidates.map { c ->
				async { c.uri to ImageCompressor.compressTo600px(context, c.uri) }
			}.awaitAll()
		}.filter { it.second.isNotEmpty() }
		onState(PipelineState.AiScoring(0.05f))

		// 2. Batch into 10s, fire concurrent /score-batch calls, accumulate progress.
		val batches = compressed600.chunked(10)
		val totalBatches = batches.size.coerceAtLeast(1)
		val completedMutex = Mutex()
		var completed = 0

		val allScores = coroutineScope {
			batches.map { batch ->
				async {
					val request = ScoreBatchRequest(
						images = batch.map { (uri, bytes) ->
							ImagePayload(id = uri.toString(), data = base64NoWrap(bytes))
						}
					)
					val resp = api.scoreBatch(request)
					completedMutex.withLock {
						completed++
						val frac = completed.toFloat() / totalBatches
						onState(PipelineState.AiScoring(0.1f + 0.5f * frac))
					}
					resp.scores
				}
			}.awaitAll()
		}.flatten()

		// 3. Top 20 by score → URIs (in score-desc order so we have a stable subset).
		val top20Uris = allScores
			.sortedByDescending { it.score }
			.take(20)
			.map { Uri.parse(it.id) }
		if (top20Uris.isEmpty()) return@withContext emptyList()

		// 4. Compress top-20 to 1200px.
		onState(PipelineState.AiSelecting(0.0f))
		val compressed1200 = coroutineScope {
			top20Uris.map { uri ->
				async { uri to ImageCompressor.compressTo1200px(context, uri) }
			}.awaitAll()
		}.filter { it.second.isNotEmpty() }
		onState(PipelineState.AiSelecting(0.5f))

		// 5. Single /cohesive-select call.
		val response = api.cohesiveSelect(
			CohesiveSelectRequest(
				images = compressed1200.map { (uri, bytes) ->
					ImagePayload(id = uri.toString(), data = base64NoWrap(bytes))
				},
				pickCount = pickCount,
			)
		)
		onState(PipelineState.AiSelecting(1.0f))
		Log.i(TAG, "Cohesive reasoning: ${response.reasoning}")

		// 6. Preserve backend's ordering.
		response.selectedIds.map { Uri.parse(it) }
	}

	// --- helpers -------------------------------------------------------------

	private fun base64NoWrap(bytes: ByteArray): String =
		Base64.encodeToString(bytes, Base64.NO_WRAP)

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
