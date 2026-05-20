# InstaCurator

AI-powered photo curation: pick the best N photos (1–10) out of up to 100, formatted for Instagram Story (9:16).

This is a monorepo:

```
instagram-curator/
├── app/        # Android (Kotlin, Jetpack Compose, min SDK 26)
└── backend/    # Spring Boot 3.4 (Kotlin, JDK 21) — proxy to Claude API
```

## Phase 0 — Setup checklist

- [x] Backend scaffold with `/health` endpoint
- [x] Backend `Dockerfile` (migration insurance — Railway also works without it)
- [x] Android scaffold with empty Compose home screen + Pick Photos button
- [ ] Both running locally (you do this; see below)
- [ ] Pushed to GitHub as one repo

## Backend

**Requirements:** JDK 21 (verify with `java -version`).

```bash
cd backend

# First time only: generate the Gradle wrapper jar
# (Skip if `gradle/wrapper/gradle-wrapper.jar` already exists)
gradle wrapper --gradle-version 8.10.2

# Run
./gradlew bootRun

# In another terminal:
curl http://localhost:8080/health
# Expected: {"status":"ok","service":"instacurator-backend"}
```

Spring Boot Actuator also exposes `/actuator/health` automatically.

### Docker (migration insurance)

```bash
cd backend
docker build -t instacurator-backend .
docker run --rm -p 8080:8080 instacurator-backend

# In another terminal:
curl http://localhost:8080/health
```

The Dockerfile honors `$PORT` so it'll work unchanged on Railway, Fly.io, Cloud Run, or anywhere else.

## Android app

**Requirements:** Android Studio Ladybug (2024.2.1) or newer, Android SDK 34 installed.

1. Open `app/` as a project in Android Studio
2. Let Gradle sync (first time will download dependencies)
3. Run on an emulator or device (Android 8.0 / API 26 or higher)

You should see "InstaCurator" with a "Pick Photos" button. Tapping it shows a placeholder toast — actual photo picker wiring lands in Phase 1.

## What's next

- **Phase 1:** Wire up Android Photo Picker, enforce the 100-photo cap, display selected count
- **Phase 2:** On-device sharpness filter (OpenCV Laplacian variance)
- **Phase 3:** pHash dedup + ML Kit face scoring
- **Phase 4:** Backend Claude API proxy + two-pass scoring pipeline

See `InstagramCuratorSummary.md` for full architecture. (Note: that doc still mentions Node/Fastify — the backend was switched to Spring Boot Kotlin in Phase 0. Worth updating.)
