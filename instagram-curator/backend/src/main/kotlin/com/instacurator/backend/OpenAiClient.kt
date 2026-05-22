package com.instacurator.backend

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

/** Thrown when OpenAI returns a non-2xx response; mapped to HTTP 502 by the controller advice. */
class OpenAiException(val upstreamStatus: HttpStatusCode, message: String) : RuntimeException(message)

@Component
class OpenAiClient(
	private val properties: OpenAiProperties,
	private val objectMapper: ObjectMapper,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	private val restClient: RestClient = RestClient.builder()
		.baseUrl(properties.baseUrl)
		.defaultHeader("Authorization", "Bearer ${properties.apiKey}")
		.requestFactory(SimpleClientHttpRequestFactory().apply {
			setConnectTimeout(Duration.ofSeconds(10))
			setReadTimeout(Duration.ofSeconds(60))
		})
		.build()

	init {
		if (properties.apiKey.isBlank()) {
			log.warn("OPENAI_API_KEY is not set — OpenAI calls will fail until it is configured")
		} else {
			log.info(
				"OpenAI client ready (key={}, scoring={}, selection={})",
				maskKey(properties.apiKey), properties.scoringModel, properties.selectionModel,
			)
		}
	}

	/** Scores up to 10 images 1–10 in a single gpt-4o-mini call. */
	fun scoreBatch(images: List<ImagePayload>): ScoreBatchResponse {
		val content = buildList {
			add(ContentPart(type = "text", text = SCORING_INSTRUCTIONS))
			images.forEach { image ->
				add(ContentPart(type = "text", text = "Image id: ${image.id}"))
				add(ContentPart(type = "image_url", imageUrl = ImageUrl(dataUri(image.data), detail = "low")))
			}
		}
		return call(properties.scoringModel, content, SCORE_SCHEMA, "photo_scores")
	}

	/** Picks exactly pickCount cohesive images from up to 20 in a single gpt-4.1 call. */
	fun cohesiveSelect(images: List<ImagePayload>, pickCount: Int): CohesiveSelectResponse {
		val content = buildList {
			add(ContentPart(type = "text", text = selectionInstructions(pickCount)))
			images.forEach { image ->
				add(ContentPart(type = "text", text = "Image id: ${image.id}"))
				add(ContentPart(type = "image_url", imageUrl = ImageUrl(dataUri(image.data), detail = "high")))
			}
		}
		return call(properties.selectionModel, content, selectionSchema(pickCount), "cohesive_selection")
	}

	private inline fun <reified T> call(
		model: String,
		content: List<ContentPart>,
		schema: Map<String, Any>,
		schemaName: String,
	): T {
		val request = ChatRequest(
			model = model,
			messages = listOf(ChatMessage(role = "user", content = content)),
			responseFormat = ResponseFormat(
				jsonSchema = JsonSchema(name = schemaName, schema = schema),
			),
		)

		val startedAt = System.currentTimeMillis()
		val response = restClient.post()
			.uri("/v1/chat/completions")
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.onStatus({ it.isError }) { _, res ->
				val errorBody = res.body.bufferedReader().use { it.readText() }
				throw OpenAiException(res.statusCode, "OpenAI API error ${res.statusCode}: ${errorBody.take(500)}")
			}
			.body(ChatResponse::class.java)

		val latencyMs = System.currentTimeMillis() - startedAt
		val payload = response?.choices?.firstOrNull()?.message?.content
			?: throw OpenAiException(HttpStatusCode.valueOf(502), "OpenAI returned no message content")

		log.info("OpenAI call complete (model={}, latencyMs={})", model, latencyMs)
		return objectMapper.readValue(payload, T::class.java)
	}

	private fun dataUri(base64Jpeg: String) = "data:image/jpeg;base64,$base64Jpeg"

	private fun maskKey(key: String) =
		if (key.length <= 8) "***" else "${key.take(3)}...${key.takeLast(4)}"
}

private const val SCORING_INSTRUCTIONS =
	"You are scoring photos for an Instagram Story (9:16 vertical). " +
		"Score every image from 1 to 10 on composition, lighting, subject clarity, " +
		"visual energy, and color vibrancy. Return one score for each image id provided."

private fun selectionInstructions(pickCount: Int) =
	"You are curating an Instagram Story photo set (9:16 vertical). From the images " +
		"provided, select exactly $pickCount that work best together as a cohesive set — " +
		"prioritize variety, visual flow, and scroll-stopping power. Return the chosen " +
		"image ids and a brief reasoning."

private val SCORE_SCHEMA: Map<String, Any> = mapOf(
	"type" to "object",
	"properties" to mapOf(
		"scores" to mapOf(
			"type" to "array",
			"items" to mapOf(
				"type" to "object",
				"properties" to mapOf(
					"id" to mapOf("type" to "string"),
					"score" to mapOf("type" to "number"),
				),
				"required" to listOf("id", "score"),
				"additionalProperties" to false,
			),
		),
	),
	"required" to listOf("scores"),
	"additionalProperties" to false,
)

private fun selectionSchema(pickCount: Int): Map<String, Any> = mapOf(
	"type" to "object",
	"properties" to mapOf(
		"selectedIds" to mapOf(
			"type" to "array",
			"items" to mapOf("type" to "string"),
			"minItems" to pickCount,
			"maxItems" to pickCount,
		),
		"reasoning" to mapOf("type" to "string"),
	),
	"required" to listOf("selectedIds", "reasoning"),
	"additionalProperties" to false,
)

// --- OpenAI Chat Completions wire format ---

@JsonInclude(JsonInclude.Include.NON_NULL)
private data class ChatRequest(
	val model: String,
	val messages: List<ChatMessage>,
	@param:JsonProperty("response_format") val responseFormat: ResponseFormat,
)

private data class ChatMessage(val role: String, val content: List<ContentPart>)

@JsonInclude(JsonInclude.Include.NON_NULL)
private data class ContentPart(
	val type: String,
	val text: String? = null,
	@param:JsonProperty("image_url") val imageUrl: ImageUrl? = null,
)

private data class ImageUrl(val url: String, val detail: String)

private data class ResponseFormat(
	val type: String = "json_schema",
	@param:JsonProperty("json_schema") val jsonSchema: JsonSchema,
)

private data class JsonSchema(
	val name: String,
	val strict: Boolean = true,
	val schema: Map<String, Any>,
)

private data class ChatResponse(val choices: List<Choice> = emptyList())

private data class Choice(val message: ResponseMessage = ResponseMessage())

private data class ResponseMessage(val content: String? = null)
