package com.instacurator.app.pipeline

import android.net.Uri

/**
 * A photo that survived all pipeline stages, with its measured features and the
 * weighted composite score used to pick a winner inside each pHash cluster.
 */
data class CandidatePhoto(
	val uri: Uri,
	val sharpness: Double,
	val eyesOpenProb: Float,
	val smileProb: Float,
	val exposureQuality: Float,
	val phash: Long,
	val compositeScore: Double,
)
