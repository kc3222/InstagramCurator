# backend/CLAUDE.md

Spring Boot proxy to the Anthropic API. Read alongside the repo-root `CLAUDE.md`.

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
└── HealthController.kt      # /health endpoint
```

When adding new endpoints in Phase 1, follow this structure:

- `ScoringController.kt` — REST endpoints (`/score-batch`, `/cohesive-select`)
- `AnthropicClient.kt` — WebClient-based call wrapper around the Anthropic API
- `AnthropicConfig.kt` — `@ConfigurationProperties("anthropic")` holder for the key + model IDs

Keep it flat. No premature service/repository/dto subpackages.

## HTTP client

Use Spring's reactive `WebClient` for outbound calls to `api.anthropic.com`, not `RestTemplate` (deprecated) and not OkHttp (extra dep). Already implied by the Spring Web starter.

## Anthropic API call shape

Outbound calls go to `https://api.anthropic.com/v1/messages`. Required headers:

- `x-api-key: $ANTHROPIC_API_KEY`
- `anthropic-version: 2023-06-01`
- `content-type: application/json`

Images go as base64 in the `content` array as `type: image` blocks. The Android app sends already-compressed JPEG bytes; backend forwards them base64-encoded without re-processing.

## Endpoint contracts (Phase 1)

**`POST /score-batch`**
- Request: `{ "images": [{ "id": "abc", "data": "<base64 jpeg>" }, ...] }` (up to 10)
- Response: `{ "scores": [{ "id": "abc", "score": 7.2 }, ...] }`
- Calls Haiku once per request.

**`POST /cohesive-select`**
- Request: `{ "images": [...up to 20...], "pickCount": 6 }`
- Response: `{ "selectedIds": ["abc", "def", ...], "reasoning": "..." }`
- Calls Sonnet once.

Keep request/response DTOs as Kotlin `data class`es in the controller file for now. If they grow past ~50 lines, split into `Dtos.kt`.

## Config

Spring reads `application.properties`:
- `server.port=${PORT:8080}` — honors Railway's injected `$PORT`, falls back to 8080 locally
- `ANTHROPIC_API_KEY` is read via `@ConfigurationProperties`, never hardcoded

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
- Don't catch `Exception` broadly in controllers; let Spring's default error handling produce the 500. Wrap only specific Anthropic error responses to map them to meaningful HTTP statuses.
- Don't add `@CrossOrigin` annotations — the Android app is a native client, not a browser, CORS is irrelevant.
- Don't add request/response logging filters that log full bodies — see "never log image bytes" above.
