# VideoContextBot — AGENTS.md

Early-stage Android app (Kotlin/Compose). Only scaffolding exists — theme, stub Activity, JNI stubs, empty test dirs. No UI, ViewModel, data, or domain layers implemented yet.

## Commands

```bash
./gradlew assembleDebug        # Debug APK
./gradlew assembleRelease      # Release APK (unsigned, minified)
./gradlew test                 # Unit tests (currently none exist)
./gradlew connectedAndroidTest # Instrumented tests (currently none exist)
./gradlew build                # Full build (lint + test + assemble)
./gradlew clean                # Clean
```

Order when verifying: `./gradlew build` (includes lint and test).

## Architecture

- **Single module**: `:app` — no library or feature modules.
- **DI**: Hilt via KSP (not KAPT). `@HiltAndroidApp` in `VideoContextApplication`, `@AndroidEntryPoint` on activities.
- **Navigation**: Navigation Compose. Expected package: `com.videocontextbot.ui.navigation`.
- **Database**: Room via KSP.
- **Background**: WorkManager + foreground service (`ProcessingNotificationService`).
- **Native**: whisper.cpp submodule at `app/src/main/cpp/whisper`. JNI stubs in `whisper-jni.cpp` — not yet functional. Models go in `app/src/main/assets/models/`.
- **ABI targets**: `arm64-v8a`, `armeabi-v7a`, `x86_64` only.
- **OpenCV**: declared in version catalog but commented out in `app/build.gradle.kts` — do not uncomment until explicitly needed.

## Key conventions

- **Docs and strings are in Spanish** — maintain consistency.
- **Kotlin 2.0.0** with `org.jetbrains.kotlin.plugin.compose` (no manual `@Composable` import needed).
- **minSdk 26, targetSdk 35, compileSdk 35**, Java 17.
- **Debug builds** have `applicationIdSuffix = ".debug"`. Release builds are unsigned.
- **No linter/formatter config** — add `.editorconfig` before introducing formatting rules.
- **Clean Architecture** expected: `ui/`, `viewmodel/`, `domain/`, `data/`, `processor/`, `worker/` under `com.videocontextbot`.
- **Timber** for logging (initialized in `VideoContextApplication`).

## Testing

- JUnit 4 for unit tests (`app/src/test/java/`), AndroidX Test + Espresso + Compose UI Test for instrumented (`app/src/androidTest/java/`).
- **No tests written yet** — add them when implementing features.

## Git

- **whisper.cpp submodule** at `app/src/main/cpp/whisper` must be initialized: `git submodule update --init --recursive`.
- **Git LFS** expected for `.bin`, `.pt`, `.onnx`, `.tflite`, `.mp4`, `.mkv` files.
- Single commit on `main` branch — no release/develop branches exist yet.

## CI

GitHub Actions: runs on push to `main`/`develop` and PRs to `main`. Two jobs: `build` (Gradle) and `docker-build` (Docker image, main push only).
