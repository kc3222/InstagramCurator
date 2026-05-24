package com.instacurator.app.network

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the OpenAI proxy backend. Retrofit 2.6+ understands
 * `suspend` natively, so no `Call<T>` wrapping is needed.
 */
interface CuratorApiService {

	@POST("score-batch")
	suspend fun scoreBatch(@Body request: ScoreBatchRequest): ScoreBatchResponse

	@POST("cohesive-select")
	suspend fun cohesiveSelect(@Body request: CohesiveSelectRequest): CohesiveSelectResponse
}
