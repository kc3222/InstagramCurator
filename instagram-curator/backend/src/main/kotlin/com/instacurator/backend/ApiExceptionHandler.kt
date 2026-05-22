package com.instacurator.backend

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

	private val log = LoggerFactory.getLogger(javaClass)

	@ExceptionHandler(IllegalArgumentException::class)
	fun handleValidation(ex: IllegalArgumentException): ResponseEntity<Map<String, String>> {
		log.warn("Rejected request: {}", ex.message)
		return ResponseEntity.badRequest().body(mapOf("error" to (ex.message ?: "invalid request")))
	}

	@ExceptionHandler(OpenAiException::class)
	fun handleOpenAi(ex: OpenAiException): ResponseEntity<Map<String, String>> {
		log.error("OpenAI upstream failure ({}): {}", ex.upstreamStatus, ex.message)
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
			.body(mapOf("error" to "upstream AI request failed"))
	}
}
