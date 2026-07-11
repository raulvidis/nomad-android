---
summary: "Build and runtime configuration: SDK levels, ABI, model, database."
read_when:
  - "Changing build settings, ABI, the model, or runtime config"
---

# Configuration

## Build config (`app/build.gradle.kts`)

| Setting | Value |
|---|---|
| `namespace` / `applicationId` | `com.nomad.android` |
| `compileSdk` / `targetSdk` | 35 |
| `minSdk` | 26 |
| `versionCode` | 1 |
| `versionName` | see `app/build.gradle.kts` (bumped every release) |
| `abiFilters` | `arm64-v8a` (only) |
| APK output name | `nomad-android-<versionName>.apk` |

Native build wired via `externalNativeBuild { cmake { ... } }` → `app/src/main/cpp/CMakeLists.txt` (target `nomad_llm` → `libnomad_llm.so`).

## Model

- Selectable Q4_K_M GGUF text models (defined in `LlamaCppEngine.ModelVariant`): MiniCPM5-1B (default/recommended, ~656 MB), Qwen3.5-0.8B (~508 MB), Gemma-4-E2B (~2.9 GB, run text-only), and Liquid LFM2.5-230M (~146 MB).
- Downloaded in-app from the official GGUF repo via OkHttp; not bundled in the APK.
- No model present → app falls back to the rule-based `FallbackEngine`.
- Context window: 4K (token budget sized to this).

## Database (Room)

- Version 6, 9 entities, 5 migrations. Schema change → add a migration (never destructive).
- `allowBackup` disabled (manifest) to keep DB/file state consistent.

## Network policy

OkHttp exists ONLY for user-initiated downloads (model, ZIM archives, map tiles). The app is offline-first: no telemetry, analytics, or cloud sync. See [spec.md](spec.md).
