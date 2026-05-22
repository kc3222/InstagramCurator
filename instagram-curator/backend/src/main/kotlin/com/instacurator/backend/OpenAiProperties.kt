package com.instacurator.backend

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "openai")
data class OpenAiProperties(
	val apiKey: String = "",
	val scoringModel: String = "gpt-4o-mini",
	val selectionModel: String = "gpt-4.1",
	val baseUrl: String = "https://api.openai.com",
)
