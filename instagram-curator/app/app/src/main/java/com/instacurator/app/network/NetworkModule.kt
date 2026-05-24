package com.instacurator.app.network

import com.instacurator.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Provides the OkHttp client, Retrofit instance, and API service to Hilt's
 * SingletonComponent — one shared instance for the whole app.
 *
 * IMPORTANT: keep the logging interceptor at BASIC (or HEADERS). The request
 * bodies contain base64-encoded JPEGs; logging at BODY would flood Logcat with
 * megabytes of base64 per call.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

	private const val TIMEOUT_SECONDS = 60L

	@Provides
	@Singleton
	fun provideOkHttpClient(): OkHttpClient {
		val logging = HttpLoggingInterceptor().apply {
			level = HttpLoggingInterceptor.Level.BASIC
		}
		return OkHttpClient.Builder()
			.connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.addInterceptor(logging)
			.build()
	}

	@Provides
	@Singleton
	fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
		.baseUrl(BuildConfig.API_BASE_URL)
		.client(client)
		.addConverterFactory(GsonConverterFactory.create())
		.build()

	@Provides
	@Singleton
	fun provideCuratorApi(retrofit: Retrofit): CuratorApiService =
		retrofit.create(CuratorApiService::class.java)
}
