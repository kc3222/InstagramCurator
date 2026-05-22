# CLAUDE.md

Instructions for Claude Code working in this repo.

## What this is

InstaCurator — an Android app that picks the best N (1–10) photos out of up to 100 using on-device filters plus the OpenAI Vision API, formatted for Instagram Story (9:16). Monorepo:

```
instagram-curator/
├── app/        # Android — Kotlin, Jetpack Compose, min SDK 26
└── backend/    # Spring Boot 3.4 — Kotlin, JDK 21, proxy to OpenAI API
```

Each surface has its own `CLAUDE.md` with surface-specific rules. Read the one for the folder you're editing.

- `app/CLAUDE.md` — Android conventions
- `backend/CLAUDE.md` — Spring Boot conventions

## Architecture docs

Authoritative architecture lives in two project-root files:

- `InstagramCuratorSummary.md` — pipeline, tech stack, cost model
- `InstagramCuratorBuildPlan.md` — phased build plan

## Current state (update as phases complete)

- **Phase 0: done** — scaffolds for both projects, `/health` endpoint, Dockerfile, Compose home screen with placeholder "Pick Photos" button.
- **Phase 1: done** — backend `/score-batch` (`gpt-4o-mini`) and `/cohesive-select` (`gpt-4.1`) endpoints, OpenAI structured outputs, 400/502 error handling. Railway deploy is the remaining user-driven step.
- **Phase 2: next** — Android photo picker + on-device pipeline.
- Phases 3–6: see `InstagramCuratorBuildPlan.md`.

## Cross-cutting rules

**Do not add tests until Phase 4** unless a bug has appeared twice. This is a prototype until MVP ships.

**No auth, no user accounts, no analytics, no telemetry.** Personal tool. Resist adding them.

**The backend is a thin proxy.** It exists only to keep the OpenAI API key off the client. No DB, no caching layer, no auth in Phases 1–4. If a feature seems to want one of those, push back before adding it.

**The OpenAI API key only ever lives in the backend**, read from `OPENAI_API_KEY` env var via Spring's `@ConfigurationProperties`. It must never be committed, never appear in the Android app, never be logged.

**Don't add pipeline stages to fix model prompt quality.** If the model returns bad results, tune the prompt. The pipeline (sharpness → pHash dedup → `gpt-4o-mini` scoring → `gpt-4.1` cohesive selection) is fixed for Phases 1–4.

**Hard caps that aren't negotiable:**
- Max 100 input photos per run
- Output range: 1–10 photos
- `gpt-4o-mini` batch size: 10 images, 4 parallel calls = 40 candidates max into Pass 1
- Top 20 from Pass 1 go into `gpt-4.1` Pass 2

## Tech decisions worth knowing

These are settled; don't re-litigate without good reason:

- **Android native Kotlin**, not Flutter or React Native. Chosen for ML Kit, MediaStore, and Photo Picker API access.
- **Spring Boot + Kotlin**, not Node/Fastify. Same language across both surfaces matters more than ecosystem size for a one-person project.
- **Railway for Phases 1–4**, AWS App Runner via ECR for Phase 5. Dockerfile is the migration bridge.
- **Two-tier AI**: `gpt-4o-mini` for scoring (cheap, parallel), `gpt-4.1` for final cohesive pick (one call, higher quality).

## OpenAI models

When making API calls from the backend:
- Pass 1 scoring: `gpt-4o-mini`
- Pass 2 cohesive selection: `gpt-4.1`

Don't substitute other models without explicit instruction.

## When uncertain

Ask before doing any of these — they've come up in the build plan and have specific intended approaches:
- Adding any new dependency to either project
- Changing the pipeline stages or their order
- Adding persistence (DB, file cache, Redis) to the backend
- Adding error retry / backoff logic (intended for Phase 4)
- Touching the Dockerfile (migration insurance — changes have downstream Railway/AWS implications)
