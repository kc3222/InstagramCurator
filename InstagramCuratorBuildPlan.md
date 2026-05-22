# Instagram Curator — Build Plan

## At a glance

| Phase | Where | Time |
|---|---|---|
| 0 — Setup | Both | ~2h |
| 1 — Backend Proxy | Backend | ~4h |
| 2 — Photo Selection + Local Pipeline | Android Studio | ~8h |
| 3 — Wire Up Claude | Android Studio | ~4h |
| 4 — Polish & Beta Test | Android Studio | ~6h |
| 5 — AWS Migration | Backend + small Android tweak | ~6h |
| 6 — Real Infra Learning (optional) | Backend | — |

**Android Studio work:** ~18h of the ~24h MVP. **Backend work:** ~6h of the MVP, plus ~6h for AWS migration.

---

## Phase 0: Setup `[Both]` (~2 hours)

**Goal:** Both projects scaffolded, both running "hello world" locally.

- Create monorepo `instagram-curator/` with `app/` and `backend/` folders
- **Backend:** Initialize Spring Boot project via [start.spring.io](https://start.spring.io) — Kotlin, Gradle, Spring Web, Spring Boot Actuator. Verify `./gradlew bootRun` serves a `/health` endpoint
- **Android:** New Android Studio project, Kotlin, min SDK 26, Jetpack Compose. Empty home screen with a "Pick Photos" button
- Push both to GitHub as one repo
- **Critical:** Write a `Dockerfile` for the backend now, even though Railway doesn't require it. Test locally with `docker build` + `docker run`. This is your migration insurance.

---

## Phase 1: Backend Proxy `[Backend]` (~4 hours)

**Goal:** A working proxy that takes images and returns Claude's response.

- Add two endpoints:
  - `POST /score-batch` — accepts 10 images, calls GPT-4o mini (gpt-4o-mini), returns scores
  - `POST /cohesive-select` — accepts up to 20 images + pickCount, calls GPT-4.1 (gpt-4.1), returns selected IDs
- Wire up `OPENAI_API_KEY` via environment variable, read into a `@ConfigurationProperties` class
- Use Spring's `WebClient` (reactive HTTP) to call Anthropic's API
- Structured JSON logging via Logback config
- Test with `curl` and a couple of sample images
- Deploy to Railway: connect GitHub repo, set the env var, get your public URL

---

## Phase 2: Android — Photo Selection + Local Pipeline `[Android Studio]` (~8 hours)

**Goal:** User picks photos, on-device filters whittle them down to ~40 candidates.

- Integrate Android Photo Picker API, cap at 100, show running count
- Add stepper for output count (1–10, default 6)
- Pull in OpenCV for Android — implement Laplacian variance on downscaled bitmaps
- Pull in JImageHash for perceptual hashing, write the clustering logic (hash distance < 10)
- Integrate ML Kit Face Detection for eyes-open + smile scoring
- Compute composite cluster score (50/25/15/10 split), keep the winner per cluster
- Show a debug screen displaying the ~40 survivors so you can verify the pipeline works before adding AI

This is the biggest phase. Test it standalone before connecting to the backend.

---

## Phase 2A: Android — Photo Picker + UI Shell `[Android Studio]` (~3 hours)
 
**Goal:** User can pick photos, set output count, and see thumbnails. No pipeline logic yet.
 
- Integrate Android Photo Picker API (`PickMultipleVisualMedia`), hard cap at 100
- Show running count: "X of 100 photos selected"
- Add output count stepper (1–10, default 6): "Photos to pick: N"
- `SelectedPhotosGrid` composable — `LazyVerticalGrid` with Coil thumbnails, tap to deselect
- `MainViewModel` (Hilt) holding `selectedUris` and `pickCount`
- "Analyze" button disabled until at least 1 photo is selected
Verify photo selection and grid display are solid before touching any native libs.
 
---
 
## Phase 2B: Android — Local Filtering Pipeline `[Android Studio]` (~5 hours)
 
**Goal:** On-device filters whittle selected photos down to ~40 candidates.
 
- Add `PipelineProcessor` class (Hilt-injected) with `suspend fun process(uris: List<Uri>): List<Uri>`
- **Stage 1 — Sharpness filter:** Laplacian variance via OpenCV for Android on downscaled bitmaps, threshold 100.0
- **Stage 2 — pHash dedup:** JImageHash clustering (Hamming distance < 10), score each cluster candidate on the 50/25/15/10 split (sharpness / eyes open / smile / exposure), keep the winner
- **Stage 3 — Face scoring:** ML Kit Face Detection (`PERFORMANCE_MODE_ACCURATE`, `CLASSIFY_ALL_LANDMARKS`) for eyes-open and smile probabilities
- Expose `pipelineState: StateFlow<PipelineState>` — sealed class `Idle | Running(stage, progress) | Done(candidates) | Error(msg)`
- HomeScreen shows progress indicator with current stage name while running
- **Debug screen:** after pipeline completes, navigate to a grid showing surviving candidates with composite score overlaid as a badge
Note: OpenCV `.so` native libs must be manually added to `app/src/main/jniLibs/` before building. Don't proceed to Phase 3 until the debug screen shows sensible filtering results on real photos.

---

## Phase 3: Wire Up Claude `[Android Studio]` (~4 hours)

**Goal:** End-to-end pipeline working.

- Compress survivors to 600px JPEG q75, send to `/score-batch` in 4 parallel calls
- Take top 20 by score, recompress to 1200px JPEG q85, send to `/cohesive-select`
- Display the final N photos in a 9:16 story-card UI (Compose, `LazyColumn` with snap)
- Add MediaStore save-to-camera-roll

---

## Phase 4: Polish & Beta Test `[Android Studio]` (~6 hours)

**Goal:** App works reliably enough to share with friends.

- Loading states for each pipeline stage (sharpness → dedup → AI scoring → AI selection)
- Error handling: network failures, API errors, timeout retries
- Tweak Claude prompts based on real photos giving bad results
- Send APK to 3–5 friends, watch them use it, collect feedback
- Fix the top 3 issues

**Milestone: MVP shipped.** Stop and assess before moving on.

---

## Phase 5: AWS Migration `[Backend + small Android tweak]` (~6 hours)

**Goal:** Same app, running on AWS instead of Railway. Learning goal as much as shipping goal.

- AWS account setup, **billing alert at $10 first**, IAM user with limited permissions
- Push your existing Docker image to ECR (Elastic Container Registry)
- Deploy via **App Runner** — point it at the ECR image, set env vars
- Move the API key to **AWS Secrets Manager**, update Spring Boot to read from it
- Set up CloudWatch logging (your stdout logs flow there automatically)
- Update Android app's backend URL, test end-to-end
- GitHub Actions workflow: on push to main, build image → push to ECR → trigger App Runner deploy

---

## Phase 6 (Optional, later): Real Infra Learning `[Backend]`

Only if you want to keep going after migration. Each of these is a standalone learning project:

- Add Redis (ElastiCache) for response caching on identical image hashes
- Add OpenTelemetry instrumentation, ship traces to AWS X-Ray
- Migrate from App Runner to ECS Fargate to learn task definitions, load balancers, VPCs
- Eventually: EKS, to learn Kubernetes properly

---

## Notes on staying ship-focused

A few rules to keep Phase 1–4 from sprawling:

- No tests until Phase 4 unless something breaks twice — you're prototyping
- No auth, no user accounts, no analytics — it's a personal tool
- If a Claude prompt gives mediocre results, tune the prompt; don't add more pipeline stages
- Resist the urge to start AWS work before Phase 4 ships. The discipline of finishing the MVP first is the whole point.
