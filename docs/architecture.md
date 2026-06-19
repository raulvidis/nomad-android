---
summary: "App modules, data flow, AI engine stack, and the native llama.cpp JNI bridge."
read_when:
  - "Understanding how the app is structured"
  - "Changing data flow, repositories, DI, or the native AI path"
---

# Architecture

Single-module Jetpack Compose app. **MVVM + Repository + Hilt DI.**

```
UI (Compose Screens + ViewModels)
  ↓ StateFlow<UiState>          (collectAsStateWithLifecycle)
Repositories                    (return Result<T>, sealed — data/Result.kt)
  ↓
Data Sources (Room DAOs · ContentPackManager · AIEngine · MBTiles/tiles)
```

## Source layout

`app/src/main/java/com/nomad/android/`

- `data/`
  - `ai/` — `AIEngine` interface + 3 implementations (see below).
  - `content/` — content packs + Kiwix ZIM management (`ContentPackManager`).
  - `local/` — Room database, DAOs, entities, migrations.
  - `maps/` — tile calculation, MBTiles, offline tile management (`OfflineTileManager`).
  - `repository/` — repository layer (7 repositories) wrapping DAOs/managers, returning `Result<T>`.
  - `Result.kt` — custom sealed result type.
- `di/` — Hilt modules: `DatabaseModule`, `AIModule`, `MapsModule`, `RepositoryModule`.
- `ui/` — `theme/` (CRT effects), `components/`, `navigation/`, and per-feature screens: `dashboard`, `maps`, `knowledge`, `chat`, `notes` (list + editor), `emergency`, `settings`, `onboarding`.
- `util/` — location tracking service.
- Top-level: `MainActivity.kt`, `NomadApp.kt`, `NomadApplication.kt`, `DownloadService.kt`.

## Local persistence (Room)

- Database version **6**, 9 entities, 5 migrations.
- Schema changes REQUIRE a migration — never drop/recreate. `allowBackup` is disabled to keep DB/file state consistent.

## AI engine stack

Three implementations behind a common `AIEngine` interface:

| Engine | Role |
|---|---|
| `LlamaCppEngine` | On-device inference via vendored llama.cpp (`libnomad_llm.so`). Runs exactly one model: OpenBMB MiniCPM5-1B (Q4_K_M GGUF, ~656 MB). |
| `RAGEngine` | Retrieval-augmented wrapper; grounds answers in a searchable offline knowledge base (keyword search, stopword-filtered). |
| `FallbackEngine` | Rule-based survival responses; no model required. |

**Single-model policy:** only MiniCPM5-1B. No other LLM, no vision/"-V" variant.

## Native llama.cpp path

```
LlamaCppEngine (Kotlin)
  → LlamaBridge (Kotlin JNI facade)
    → llama-jni.cpp (JNI)
      → libnomad_llm.so  (CMake target nomad_llm; links llama, llama-common, ggml)
```

- Native source: `app/src/main/cpp/` — `CMakeLists.txt` + `llama-jni.cpp`; `llama.cpp` vendored as a git submodule.
- Built **arm64-v8a only** (`abiFilters`). llama.cpp built with tests/examples/tools/server OFF and `LLAMA_CURL=OFF` (no network in native layer; downloads go through OkHttp).
- `GgufMetadata` validates GGUF headers (array-count cap, overflow guards) before load.

## Navigation

Eight Compose Navigation routes: Dashboard, Maps, Knowledge, Chat, Notes, Note Editor, Emergency, Settings.

See also: [spec.md](spec.md) for constraints, [design.md](design.md) for UI.
