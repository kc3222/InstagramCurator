package com.instacurator.app.pipeline

/** State the pipeline can be in, observed by the UI via StateFlow. */
sealed class PipelineState {
	data object Idle : PipelineState()
	data class Running(val stage: String, val progress: Float) : PipelineState()
	data class Done(val candidates: List<CandidatePhoto>) : PipelineState()
	data class Error(val msg: String) : PipelineState()
}
