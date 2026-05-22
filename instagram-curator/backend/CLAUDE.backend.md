# backend/CLAUDE.md

Spring Boot proxy to the OpenAI API. Read alongside the repo-root `CLAUDE.md`.

## Stack pins

- JDK **21** (Temurin)
- Kotlin **1.9.25**
- Spring Boot **3.4.2**
- Gradle wrapper **8.10.2** (Kotlin DSL)

Don't bump these casually — Spring Boot, Kotlin, and AGP have tight compatibility matrices.

## Package layout

```
src/main/kotlin/com/instacurator/backend/
├── BackendApplication.kt    # @SpringBootApplication entry point
├── HealthController.kt      # /health endpoint
├── ScoringController.kt     # /score-batch, /cohesive-select + public DTOs
├── OpenAiClient.kt          # RestClient wrapper around the OpenAI API
├── OpenAiProperties.kt      # @ConfigurationProperties("openai") — key + model IDs
└── ApiExceptionHandler.kt   # @RestControllerAdvice — 400/502 mapping
```

Keep it flat. No premature service/repository/dto subpackages.

## HTTP client

Use Spring's `RestClient` for outbound calls to `api.openai.com`, not `RestTemplate` (deprecated), not `WebClient` (needs the extra `spring-webflux` dependency), and not OkHttp (extra dep). `RestClient` ships with the existing `spring-boot-starter-web`.

## OpenAI API call shape

Outbound calls go to `https://api.openai.com/v1/chat/completions`. Required headers:

- `Authorization: Bearer $OPENAI_API_KEY`
- `content-type: application/json`

Images go in the message `content` array as `image_url` blocks holding a `data:image/jpeg;base64,<bytes>` data URI. The Android app sends already-compressed JPEG bytes; the backend forwards them base64-encoded without re-processing.

Both endpoints use **structured outputs** — a `response_format` of type `json_schema` with `strict: true` — so OpenAI returns schema-validated JSON, not free text.

## Endpoint contracts (Phase 1)

**`POST /score-batch`**
- Request: `{ "images": [{ "id": "abc", "data": "<base64 jpeg>" }, ...] }` (up to 10)
- Response: `{ "scores": [{ "id": "abc", "score": 7.2 }, ...] }`
- Calls `gpt-4o-mini` once per request.

**`POST /cohesive-select`**
- Request: `{ "images": [...up to 20...], "pickCount": 6 }`
- Response: `{ "selectedIds": ["abc", "def", ...], "reasoning": "..." }`
- Calls `gpt-4.1` once.

Keep request/response DTOs as Kotlin `data class`es in the controller file for now. If they grow past ~50 lines, split into `Dtos.kt`.

## Config

Spring reads `application.properties`:
- `server.port=${PORT:8080}` — honors Railway's injected `$PORT`, falls back to 8080 locally
- `OPENAI_API_KEY` is read via `@ConfigurationProperties` (prefix `openai`), never hardcoded

## Run / build

```bash
./gradlew bootRun         # local dev
./gradlew bootJar         # build fat jar
docker build -t instacurator-backend .   # container build (migration insurance)
```

`/health` should return `{"status":"ok","service":"instacurator-backend"}`. Spring Boot Actuator also exposes `/actuator/health` automatically — don't remove it, Railway uses it.

## Logging

Default Logback config is fine for Phase 1. Structured JSON logging is on the Phase 1 checklist — when you add it, use `logstash-logback-encoder`, not a custom appender.

**Never log image bytes or base64 payloads.** Log only image IDs, counts, and latencies.

## Common mistakes to avoid

- Don't use `@RestController` and `@Controller` interchangeably — always `@RestController` here (JSON only).
- Don't catch `Exception` broadly in controllers; let Spring's default error handling produce the 500. Wrap only specific OpenAI error responses to map them to meaningful HTTP statuses.
- Don't add `@CrossOrigin` annotations — the Android app is a native client, not a browser, CORS is irrelevant.
- Don't add request/response logging filters that log full bodies — see "never log image bytes" above.
