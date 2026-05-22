package com.instacurator.backend

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

// --- Public request/response DTOs (the contract with the Android app) ---

data class ImagePayload(val id: String, val data: String)

data class ScoreBatchRequest(val images: List<ImagePayload> = emptyList())

data class ImageScore(val id: String, val score: Double)

data class ScoreBatchResponse(val scores: List<ImageScore> = emptyList())

data class CohesiveSelectRequest(
	val images: List<ImagePayload> = emptyList(),
	val pickCount: Int = 0,
)

data class CohesiveSelectResponse(
	val selectedIds: List<String> = emptyList(),
	val reasoning: String = "",
)

@RestController
class ScoringController(private val openAiClient: OpenAiClient) {

	private val log = LoggerFactory.getLogger(javaClass)

	@PostMapping("/score-batch")
	fun scoreBatch(@RequestBody request: ScoreBatchRequest): ScoreBatchResponse {
		require(request.images.isNotEmpty()) { "images must not be empty" }
		require(request.images.size <= 10) { "score-batch accepts at most 10 images" }

		log.info("score-batch received ({} images, ids={})", request.images.size, request.images.map { it.id })
		val response = openAiClient.scoreBatch(request.images)
		log.info("score-batch returning {} scores", response.scores.size)
		return response
	}

	@PostMapping("/cohesive-select")
	fun cohesiveSelect(@RequestBody request: CohesiveSelectRequest): CohesiveSelectResponse {
		require(request.images.isNotEmpty()) { "images must not be empty" }
		require(request.images.size <= 20) { "cohesive-select accepts at most 20 images" }
		require(request.pickCount in 1..10) { "pickCount must be between 1 and 10" }
		require(request.pickCount <= request.images.size) { "pickCount cannot exceed the number of images" }

		log.info(
			"cohesive-select received ({} images, pickCount={}, ids={})",
			request.images.size, request.pickCount, request.images.map { it.id },
		)
		val response = openAiClient.cohesiveSelect(request.images, request.pickCount)
		log.info("cohesive-select returning {} ids", response.selectedIds.size)
		return response
	}
}
