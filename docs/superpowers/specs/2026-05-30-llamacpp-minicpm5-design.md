# llama.cpp + MiniCPM5 mounting — design (Spec 1)

**Date:** 2026-05-30
**Status:** Approved, ready for implementation planning

## Summary

Replace NOMAD's MediaPipe LiteRT-LM inference path with a vendored, build-from-source
**llama.cpp** GGUF engine, modeled directly on the `../opendroid` project. Expose exactly
**one** installable model — `openbmb/MiniCPM5-1B-GGUF` (Q4_K_M quant, ~656 MB). Keep the
existing `AIEngine` interface, `FallbackEngine`, and `RAGEngine` intact. Port opendroid's
tool-calling JNI surface as **dormant infrastructure** — the Kotlin facade exposes it, but
no NOMAD tools are wired up. Actual tool implementations (wiki/guides/notes/location access)
and the agent loop are deferred to **Spec 2**.

## Context

- `../opendroid` is an on-device GGUF chat app: vendored llama.cpp git submodule, built from
  source via CMake/NDK into a single static-linked `.so`, ~300-line JNI bridge, a Kotlin
  `object` facade, GGUF-only models downloaded from HuggingFace via OkHttp. arm64-v8a only.
- NOMAD currently uses `LiteRTLMEngine` (MediaPipe `tasks-genai`, a `.litertlm` Gemma model)
  behind the `AIEngine` interface, with `AIEngineManager`, `FallbackEngine`, `RAGEngine`, and
  downloads through `ContentPackManager`.
- The model was identified the way opendroid's search bar would: HF API
  `search=minicpm5&sort=downloads&direction=-1` → first repo containing a GGUF file is the
  official `openbmb/MiniCPM5-1B-GGUF`; opendroid picks the Q4_K_M quant tier.

### Model coordinates

| Field | Value |
|---|---|
| HF repo | `openbmb/MiniCPM5-1B-GGUF` |
| File | `MiniCPM5-1B-Q4_K_M.gguf` |
| Size | 688,065,920 bytes (~656 MB) |
| Download URL | `https://huggingface.co/openbmb/MiniCPM5-1B-GGUF/resolve/main/MiniCPM5-1B-Q4_K_M.gguf` |

### Decisions locked during brainstorming

- **Model:** `openbmb/MiniCPM5-1B-GGUF`, Q4_K_M.
- **LiteRT/MediaPipe:** removed fully (GGUF-only, matching opendroid).
- **ABI:** arm64-v8a only.
- **JNI surface:** full — including tool-calling — but tools stay un-wired in Spec 1.
- **Scope:** two specs. Spec 1 = engine + single-model install + tool-calling bridge
  (this doc). Spec 2 = real NOMAD tools + agent loop.

## Components

### 1. Vendored native layer (copied from opendroid, renamed)

- Git submodule `app/src/main/cpp/llama.cpp` pinned to commit **`f12cc6d0fa96d6a3c33952f06b7439ac43a3c3fe`**
  (opendroid's exact pinned SHA — a known-good build that loads MiniCPM5, which reports
  `arch=llama`). Add `.gitmodules`.
- `app/src/main/cpp/CMakeLists.txt` — opendroid's flags verbatim:
  `BUILD_SHARED_LIBS=OFF`, `GGML_BACKEND_DL=OFF`, `GGML_CPU_ALL_VARIANTS=OFF`,
  `GGML_NATIVE=OFF`, `GGML_LLAMAFILE=OFF`, `GGML_CPU=ON`, `LLAMA_BUILD_COMMON=ON`,
  `LLAMA_BUILD_{TESTS,EXAMPLES,TOOLS,SERVER}=OFF`, `LLAMA_CURL=OFF`. Target renamed to
  `nomad_llm` → produces `libnomad_llm.so`. Links `llama llama-common ggml android log`.
- `app/src/main/cpp/llama-jni.cpp` — opendroid's full bridge **including** the tool-calling
  surface (`nativeSubmitTurn` / `nativeFinishTurnAndParse` / `nativeAppendToolResult`). JNI
  symbols re-mangled from `Java_com_opendroid_llm_LlamacppProvider_*` to
  `Java_com_nomad_android_data_ai_LlamaBridge_*`. The two try/catch → `g_last_error` wrappers
  on `nativeSubmitUserPrompt`/`nativeNextToken`/`nativeSubmitTurn`/`nativeFinishTurnAndParse`
  are preserved verbatim (C++ exceptions across JNI = SIGABRT).

### 2. Build config

`app/build.gradle.kts`:
- Add `externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }`.
- In `defaultConfig`: `ndk { abiFilters += listOf("arm64-v8a") }` and
  `externalNativeBuild { cmake { cppFlags += listOf("-O3","-fexceptions");
  arguments += listOf("-DCMAKE_BUILD_TYPE=Release","-DANDROID_STL=c++_static") } }`.
- Remove `implementation("com.google.mediapipe:tasks-genai:0.10.33")`.
- `minSdk` stays **26**.

### 3. Kotlin engine layer (`data/ai/`)

- **`LlamaBridge.kt`** — Kotlin `object` facade over the JNI (opendroid's `LlamacppProvider`,
  renamed). `System.loadLibrary("nomad_llm")` + `nativeInit()` in static init; `isAvailable`
  guards every call so a load failure degrades cleanly instead of crashing. Exposes
  `loadModel` / `chat` / `streamTokens` / `resetConversation` / `setSystemPrompt` **and** the
  tool-call methods (`submitTurn` / `finishTurnAndParse` / `appendToolResult` + their
  `ParsedTurn`/`ParsedToolCall` data classes). `busy: AtomicBoolean` prevents concurrent
  load/generate.
- **`GgufMetadata.kt`** — ported header validator. Reject only unparseable GGUFs; trust the
  loader otherwise (no architecture allowlist — it wrongly rejected MiniCPM5 in opendroid).
- **`LlamaCppEngine.kt`** — **replaces `LiteRTLMEngine`**, implements `AIEngine`. Holds a
  single `ModelVariant.MINICPM5_1B` enum entry (`displayName="MiniCPM5 1B"`,
  `fileName="MiniCPM5-1B-Q4_K_M.gguf"`, `downloadUrl`, `sizeMB≈656`, `ramRequiredMB`).
  Behavior per `generate`/`generateStream`:
  1. Load model via `LlamaBridge.loadModel(file, ctxSize)` if not loaded; on failure return
     the same friendly strings the current engine uses (not downloaded / incomplete / load
     error).
  2. `setSystemPrompt(NOMAD_SYSTEM_PROMPT)`.
  3. `resetConversation()`, then submit **one user turn** with the conversation `context`
     inlined as a preamble (matching NOMAD's existing single-turn-with-history pattern —
     `buildPromptWithContext`). Spec 2 switches to true stateful multi-turn.
  4. Stream tokens; strip `<think>…</think>` reasoning blocks for clean display.
  - `cleanResponse` / `stripThinking` / prompt-building are pure functions (unit-testable).

### 4. Single-model install

`ContentPackManager`, `AIEngineManager`, and `AIModule` keep their existing shape but reference
`LlamaCppEngine.ModelVariant` (single entry) instead of `LiteRTLMEngine.ModelVariant`:
- Pack id `ai_gemma4` → **`ai_minicpm5`**; pack name/description updated to MiniCPM5.
- `ContentPackManager.downloadPack` reuses the existing OkHttp `downloadFile` path unchanged
  (streaming progress, sub-1MB HTML-error guard, atomic temp-file rename). The 656 MB model
  comfortably clears the HTML guard.
- `AIModule` drops RAM-based variant selection (one model); still constructs `FallbackEngine`
  fallback and `RAGEngine`.
- `AIEngineManager` keeps its public API (`switchModel`, `getDownloadedVariants`,
  `engineStatus`, `activeVariant`) but wraps `LlamaCppEngine`; `switchModel` is trivial with
  one variant but retained for source compatibility.

### 5. Cleanup

- Delete `LiteRTLMEngine.kt`.
- `AIEngineType`: `LITERTLM_E2B` → `LLAMACPP_MINICPM5` (display "llama.cpp (MiniCPM5 1B)").
  Update references in `AIEngineManager`, `DashboardViewModel`, `SettingsViewModel`,
  `OnboardingScreen`, `OnboardingViewModel`.
- ProGuard: drop MediaPipe/TFLite keeps; add `-keep class com.nomad.android.data.ai.LlamaBridge { *; }`
  so R8 doesn't rename the JNI-bound native method declarations.

### 6. CI

`.github/workflows/ci.yml`:
- `actions/checkout@v4` with `submodules: recursive`.
- Add `nttld/setup-ndk@v1` (ndk-version `r27c`); export `ANDROID_NDK_HOME` for the build steps.
- Note: first build ~10 min while llama.cpp compiles from source; CMake caches subsequent runs.

## Data flow

```
Install:  Settings/Onboarding → ContentPackManager.downloadPack("ai_minicpm5")
          → OkHttp stream → filesDir/models/MiniCPM5-1B-Q4_K_M.gguf

Chat:     ChatViewModel → AIEngineManager.generateStream
          → LlamaCppEngine: load if needed → setSystemPrompt → resetConversation
          → submit (system + inlined history + question) → nativeNextToken loop
          → strip <think> → emit tokens
          (model not downloaded → FallbackEngine)
```

## Error handling

Mirrors opendroid: native try/catch writes to `g_last_error`, surfaced to Kotlin as
`IllegalStateException`; `LlamaCppEngine` maps these to the existing friendly messages.
`GgufMetadata` rejects unparseable files before the loader is touched. `LlamaBridge.isAvailable
== false` (native lib failed to load) short-circuits every call to a clean error.

## Testing

- **Pure-Kotlin unit tests (JVM, run in CI):** `GgufMetadata` header parsing against a crafted
  minimal GGUF byte buffer; `stripThinking` / `cleanResponse` / `buildPromptWithContext`
  pure functions; `LlamaCppEngine.ModelVariant` descriptor values; `ContentPackManager` pack
  mapping (`ai_minicpm5` ↔ variant). Style mirrors the existing `FallbackEngineTest`.
- **Native/JNI:** cannot run under JVM unit tests (no `.so`). The **CI APK build compiling
  llama.cpp from source is the integration gate** — if the JNI surface or CMake config breaks,
  `assembleDebug` fails.

## Out of scope (→ Spec 2)

- Real NOMAD tool implementations: knowledge/wiki search, survival guides, notes, location.
- The agent loop driving `submitTurn` → stream → `finishTurnAndParse` → execute tool →
  `appendToolResult` → continue.
- Switching `ChatViewModel` from single-turn-with-inlined-history to true stateful multi-turn
  driven through `LlamaBridge`.
