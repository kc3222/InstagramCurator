# app/CLAUDE.md

Android client. Read alongside the repo-root `CLAUDE.md`.

## Stack pins

- Kotlin **1.9.25**
- AGP **8.7.2**
- Compose Compiler extension **1.5.15** (set in `composeOptions` — we are NOT on the Kotlin 2.0+ Compose plugin)
- Compose BOM **2024.10.01**
- `compileSdk` 34, `targetSdk` 34, `minSdk` 26, `jvmTarget` 17
- `applicationId` and `namespace`: `com.instacurator.app`

If you bump Kotlin to 2.0+, you must also migrate from `composeOptions { kotlinCompilerExtensionVersion = ... }` to the `org.jetbrains.kotlin.plugin.compose` Gradle plugin. Don't do this casually.

## Module layout

This is a **single-module project** (`:app`) inside a Gradle root project (`InstaCurator`). The folder structure is `instagram-curator/app/app/` — the outer `app/` is the Android project root, the inner `app/` is the single Gradle module. Don't try to "fix" this nesting.

## Source layout

```
app/src/main/
├── java/com/instacurator/app/
│   ├── MainActivity.kt
│   └── ui/theme/Theme.kt
├── res/
└── AndroidManifest.xml
```

Use `java/com/instacurator/app/` for Kotlin (Android convention — yes, Kotlin files go under `java/`).

When adding code in later phases, organize by feature, not by layer:

```
com/instacurator/app/
├── picker/          # Photo Picker integration (Phase 1)
├── pipeline/        # On-device filters: sharpness, pHash, face scoring (Phase 2)
├── network/         # Backend client (Phase 3)
├── ui/              # Composables, screens, theme
└── MainActivity.kt
```

## Compose conventions

- Material 3 only (`androidx.compose.material3`). Don't pull in Material 2.
- Each screen is one composable + one preview. Previews are required for any new screen.
- Use `LocalContext.current` for Context inside composables; don't pass Context as a parameter.
- Use `remember { ... }` and `rememberSaveable { ... }` correctly — saveable for anything that should survive config changes.

## Dependencies — what's allowed in which phase

Add dependencies only when the phase that uses them is active. Approved list:

- **Phase 1**: Photo Picker (already in androidx.activity), nothing new needed
- **Phase 2**:
  - OpenCV for Android (Laplacian variance)
  - JImageHash (perceptual hashing)
  - Google ML Kit Face Detection
  - Coil (image loading) — `io.coil-kt:coil-compose`
- **Phase 3**:
  - Ktor client OR OkHttp + Retrofit (decide when we get there; lean Ktor for Kotlin-native)
  - kotlinx-serialization for JSON
- **Phase 4**: no new deps expected

Don't add Hilt, Dagger, Koin, or any DI framework. App is small enough for manual DI via constructor params.

Don't add Room, Realm, DataStore, or any persistence layer. Photos are read from MediaStore on demand; results aren't stored.

## Permissions

- Android 13+ (API 33+): Photo Picker requires **no permissions** — that's the whole point of using it.
- Android 12 and below: `READ_EXTERNAL_STORAGE` would be needed, but our `minSdk` is 26 — Phase 1 should still use Photo Picker (it has a back-compat path via `ActivityResultContracts.PickMultipleVisualMedia`).

Don't add `READ_MEDIA_IMAGES` unless we hit a concrete Photo Picker limitation.

## Common mistakes to avoid

- **The launcher icon trap.** `AndroidManifest.xml` must not reference `@mipmap/ic_launcher` or `@mipmap/ic_launcher_round` unless those mipmap resources actually exist. Phase 0 manifest intentionally omits both — Android falls back to a default system icon. We'll add a real icon in Phase 4.
- **Don't open the monorepo root in Android Studio.** Open `instagram-curator/app/`, not `instagram-curator/`. Sync will silently no-op on the wrong root.
- **Don't downgrade `compileSdk`.** Material 3 + recent Compose BOM require 34+.
- **Don't put network calls on the main thread.** Use `viewModelScope` or `LaunchedEffect` with `Dispatchers.IO`. Phase 3.
- **Image bytes are large.** Anything in the pipeline must downscale before holding bitmaps in memory. 100 full-res photos = OOM territory. Use `BitmapFactory.Options.inSampleSize`.

## Run

Open `instagram-curator/app/` in Android Studio, let Gradle sync, hit Run. Emulator: Pixel 7 / API 34. Physical device: Android 8.0+ with USB debugging.
