package com.instacurator.app.network

/**
 * Wire format mirroring the Phase 1 backend's `ScoringController`. Field names
 * must match the backend exactly — Gson uses property names as JSON keys.
 *
 * `data` carries a base64-encoded JPEG payload (NO_WRAP, no newlines).
 */
data class ImagePayload(val id: String, val data: String)

data class ScoreBatchRequest(val images: List<ImagePayload>)

data class ImageScore(val id: String, val score: Double)

data class ScoreBatchResponse(val scores: List<ImageScore>)

data class CohesiveSelectRequest(
	val images: List<ImagePayload>,
	val pickCount: Int,
)

data class CohesiveSelectResponse(
	val selectedIds: List<String>,
	val reasoning: String,
)
