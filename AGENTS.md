# AGENTS.md

READ ./.agents/AGENTS.base.md BEFORE ANYTHING (skip if missing).

Repo-specific hard rules only. Shared rules (Reviews, PR/CI, Git, Runtime Safety,
generic Project Defaults, Workflows) live in `AGENTS.base.md` — not duplicated here.

## Core
- Repo: `nomad-android` — offline-first Android survival app. Kotlin 2.0.21 + Jetpack Compose, single module `app/`.
- Package: `com.nomad.android`. Source root: `app/src/main/java/com/nomad/android/`.
- Native: `app/src/main/cpp/` — llama.cpp submodule + JNI → `libnomad_llm.so`.
- Branch: `main`. versionName `1.0.0`.
- **OFFLINE-FIRST is the product.** NEVER add network/cloud/telemetry/analytics deps. App must work fully with no internet. OkHttp exists for user-initiated downloads (models, ZIM, tiles) ONLY.
- **Models:** multiple downloadable GGUF text models, defined in `LlamaCppEngine.ModelVariant`. Default/recommended = OpenBMB MiniCPM5-1B (Q4_K_M). Also Qwen3.5-0.8B (Q4_K_M) and Gemma-4-E2B (Q4_K_XL — a multimodal model run TEXT-ONLY, no mmproj/vision). Add a variant → enum entry + `ContentPackManager` pack-id mapping + onboarding description.
- Release = git tag (semver), not a `main` merge. `latest-build` = CI rolling artifact, NOT a release.
- Contributing rules: see `CONTRIBUTING.md`.

## Routing
- Screenshots/assets: `stitch-assets/`.
- Docs index: `docs/index.md` — read at session start.
- Design system: `docs/design.md` (root `design.md` kept as-is, do not edit).
- Secrets: none in repo. Never echo env / signing keys. No keystore commits.
- Build needs: JDK 17, Android SDK API 35, Build Tools 35.0.0, CMake + NDK (r27c), llama.cpp submodule initialized.
- Native build: arm64-v8a ONLY (`abiFilters`). Do not add other ABIs without approval.

## Project Defaults (repo-specific)
- Build tool: Gradle (`./gradlew`).
- UI: Jetpack Compose + Material 3. No XML layouts / Views.
- DI: Hilt (KSP), modules in `di/`. Persistence: Room (KSP), DB v6, migrations required for schema changes — never drop/recreate.
- Pattern: MVVM + Repository. ViewModels expose `StateFlow<XxxUiState>`; repos return `Result<T>` (sealed, `data/Result.kt`).
- UI aesthetic: retro Pip-Boy CRT terminal — phosphor green `#00FF41`, scanlines, monospace, no rounded Material corners, no multi-color icons. Follow `docs/design.md`.
- New dep → also run an offline-first audit (reject anything phoning home).

## PR / CI (repo-specific)
- CI (GitHub Actions, on push/PR to `main`): lint → testDebugUnitTest → assembleDebug.

## Runtime Safety (repo-specific)
- Don't run full gradle build casually (slow; needs NDK + submodule). Prefer targeted tasks.
