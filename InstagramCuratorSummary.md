# 📱 Instagram Curator — Final Architecture

An Android app that uses on-device intelligence + Claude Vision AI to pick the best **N photos (user-selected, 1–10)** from up to **100 camera photos**, formatted for Instagram Story (9:16 vertical).

---

## 🔄 The Pipeline

```
Up to 100 camera photos selected by user
        ↓
[1] Sharpness filter             100 → ~85
        ↓
[2] pHash dedup + face scoring   ~85 → ~40
        ↓
[3] Claude Haiku batch scoring   40 → top 20
        ↓
[4] Claude Sonnet cohesive pick  20 → final N (1–10)
        ↓
Display in 9:16 story cards → save to camera roll
```

---

## 🎛️ User Input Controls

**Photo selection:**
- User taps "Pick Photos" → opens Android Photo Picker
- Hard cap at 100 — if exceeded, show toast: "Max 100 photos at a time"
- Show running count: "47 of 100 photos selected"

**Number of output photos (1–10):**
- Stepper or slider on the home screen
- Default: 6

---

## 🧠 Stage-by-Stage Breakdown

### [1] Sharpness Filter (100 → ~85)

On-device, fast. The only quality gate we need for camera photos:

- Compute **Laplacian variance** on a downscaled version of each photo
- Skip anything below the blur threshold (motion blur, missed focus)
- Saves API tokens by not sending obviously bad shots

### [2] Perceptual Hash Dedup + Face Scoring (~85 → ~40)

Eliminates near-duplicates while keeping the best version of each moment:

- Compute pHash for every survivor
- Group images with hash distance under 10 → duplicate clusters
- For each cluster, score candidates on:
  - **Sharpness** (Laplacian variance) — 50%
  - **Eyes open** (ML Kit face detection) — 25%
  - **Smile probability** (ML Kit) — 15%
  - **Exposure quality** (histogram clipping) — 10%
- Keep only the winner per cluster

### [3] Claude Haiku Batch Scoring (40 → top 20)

Cheap, fast subjective scoring on the survivors:

- Compress to 600px JPEG quality 75
- Send in batches of 10 in parallel (4 API calls)
- Each image scored 1–10 on: composition, lighting, subject clarity, visual energy, color vibrancy
- Keep the top 20 scorers

### [4] Claude Sonnet Cohesive Selection (20 → N)

Higher-resolution final curation:

- Re-compress top 20 to 1200px JPEG quality 85
- One API call to Sonnet with all 20 + user's requested `pickCount`
- Pick the best N photos as a *cohesive set* — variety, flow, scroll-stopping power
- Return selected IDs + reasoning

---

## ⚙️ Tech Stack

| Layer | Tech |
|---|---|
| **Mobile** | Android Native (Kotlin) |
| **Photo picker** | Android Photo Picker API |
| **Image loading** | Coil |
| **Sharpness analysis** | OpenCV for Android (Laplacian variance) |
| **Perceptual hashing** | `JImageHash` Kotlin library |
| **Face / eyes / smile** | Google ML Kit Face Detection (free, on-device) |
| **Backend** | Kotlin + Spring Boot, hosted on Railway (later: AWS App Runner) |
| **AI Pass 1** | Claude Haiku (`claude-haiku-4-5`) |
| **AI Pass 2** | Claude Sonnet (`claude-sonnet-4-6`) |
| **Output** | Save selected photos to camera roll via MediaStore |

---

## 💰 Cost & Performance

**Per analysis of 100 photos:**

- Local stages [1] + [2]: ~3-5 seconds, free
- Claude Haiku Pass 1: ~5-8 seconds, ~$0.04
- Claude Sonnet Pass 2: ~5-8 seconds, ~$0.05
- **Total: ~15-20 seconds, ~$0.09 per run**

---

## 🎯 Design Principles

- **Heavy lifting happens on-device** — sharpness, dedup, face scoring run locally. Free, private, fast.
- **AI does only what AI is good at** — by the time Claude sees photos, every one is sharp and non-duplicate. Claude focuses on *subjective* judgment (composition, energy, flow).
- **Two-tier AI** — Haiku grunt-works the scoring, Sonnet handles the nuanced final curation.
- **Backend is a thin proxy** — only exists to protect the API key. No data stored.
- **User controls the output** — pick exactly 1 to 10 photos.

---

## 📂 Project Structure (Monorepo)

```
instagram-curator/
├── app/                  # Android Kotlin app
└── backend/              # Kotlin + Spring Boot backend
```

---

## 🔑 Key Architectural Decisions Log

1. **Android-only Kotlin native** — chosen over Flutter for performance and Android-specific ecosystem (ML Kit, MediaStore, Photo Picker API)
2. **Local pre-filtering before AI** — eliminates obvious-bad images and duplicates locally so Claude only sees worthwhile candidates
3. **Two-tier AI (Haiku → Sonnet)** — cheap model for scoring, expensive model for nuanced final curation
4. **Backend as thin proxy** — protects the API key, no data storage, no auth (personal tool)
5. **100-photo input cap** — keeps processing time under 20 seconds and cost under $0.10 per run
6. **User-controlled output (1–10)** — flexibility for different reel formats
