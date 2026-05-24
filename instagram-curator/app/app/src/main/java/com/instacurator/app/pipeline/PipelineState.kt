package com.instacurator.app.pipeline

import android.net.Uri

/** State the pipeline can be in, observed by the UI via StateFlow. */
sealed class PipelineState {
	data object Idle : PipelineState()

	/** Local on-device pipeline (sharpness, dedup, scoring). */
	data class Running(val stage: String, val progress: Float) : PipelineState()

	/** Local pipeline finished — kept for debug routing; not used in normal flow. */
	data class Done(val candidates: List<CandidatePhoto>) : PipelineState()

	/** Phase 3 — AI scoring round (gpt-4o-mini, parallel batches). */
	data class AiScoring(val progress: Float) : PipelineState()

	/** Phase 3 — final cohesive selection (gpt-4.1, single call). */
	data class AiSelecting(val progress: Float) : PipelineState()

	/** Final ordered photos the backend selected, ready for display + save. */
	data class FinalResult(val photos: List<Uri>) : PipelineState()

	data class Error(val msg: String) : PipelineState()
}
