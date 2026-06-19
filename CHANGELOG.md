# Changelog

All notable changes to this project are documented here. Format is loosely based on [Keep a Changelog](https://keepachangelog.com/); this project follows [Semantic Versioning](https://semver.org/) from 1.0.0 onward.

## [Unreleased]

### Added
- Models: two additional downloadable AI models at onboarding/settings — **Qwen3.5-0.8B** (Q4_K_M, ~508 MB) and **Gemma-4-E2B** (Q4_K_XL, ~2.5 GB; a multimodal model run text-only). MiniCPM5-1B remains the default/recommended. Supersedes the prior single-model policy.
- Chat: model-driven tool calling — the LLM decides when to read offline data via `search_knowledge_base` and `search_notes` tools (read-only, auto-run, no approval prompt), driven by a new `ChatAgent` loop over the existing `LlamaBridge` tool primitives.
- Chat: collapsible "thinking" UI — model reasoning (`<think>…</think>`, including MiniCPM5's implicit-close form) renders as a tappable "THOUGHT FOR A MOMENT" section instead of being discarded.
- Chat: interleaved tool-call cards showing the tool, arguments, status, and a collapsible result.
- Chat: stick-to-bottom streaming scroll that disengages when the user scrolls up and re-engages on send, with a jump-to-latest button.

### Changed
- Chat: retrieval is now tool-driven — the always-on RAG context injection in `ChatViewModel` is replaced by model-decided `search_knowledge_base` calls. When no model is loaded, chat still falls back to the rule-based engine.

### Removed
- `RAGEngine` (and its DI provider/tests): superseded by tool-driven retrieval; it had no remaining consumer.

### Fixed
- Database: remove `fallbackToDestructiveMigrationFrom(1, 2, 3, 4)` from the Room builder, which overlapped the supplied `MIGRATION_1_2`..`MIGRATION_4_5` start versions and made `Room.Builder.build()` throw `IllegalArgumentException` on every launch (including fresh installs), crashing the app before any UI rendered. The full 1→6 migration chain makes destructive fallback unnecessary.

## [1.0.0] - 2026-06-19

### Added
- On-device AI chat via vendored llama.cpp build (`libnomad_llm.so`) running OpenBMB MiniCPM5-1B (Q4_K_M GGUF, ~656 MB), with `LlamaCppEngine`, `RAGEngine`, and rule-based `FallbackEngine` behind a common `AIEngine` interface.
- Retrieval-augmented generation grounding LLM answers in a searchable offline knowledge base.
- `LlamaBridge` Kotlin facade over the llama.cpp JNI, plus `GgufMetadata` GGUF header validator.
- Offline maps (MapLibre Native) with MBTiles storage, GPS tracking, waypoints, route recording, and offline tile download.
- Knowledge browser for offline Wikipedia (Kiwix ZIM) and bundled survival content, with content pack management.
- Emergency tools (first aid, survival checklists, quick-reference procedures).
- Markdown notes with live preview, search, and offline storage.
- Pip-Boy retro-futuristic CRT terminal UI (phosphor green palette, scanlines, monospace) with first-run boot sequence.
- Room database (v6, 9 entities, 5 migrations); MVVM + Repository + Hilt architecture.
- CMake/llama.cpp native build (arm64-v8a only) with ProGuard keeps for the JNI bridge.
- CI publishing `main` builds to a rolling `latest-build` release; APK named `nomad-android-<version>.apk`.

### Fixed
- Maps: surface failed tile count on offline download completion (#45); guard final `setMetadata` in `downloadRegion` against a closed DB (#43); eliminate TOCTOU gap in `OfflineTileManager.getTile()` (#38).
- Chat: cancel DB collector before streaming to stop placeholder clobber (#41); make `generateStream` mutex unlock cancellation-safe (#33); queue concurrent requests via `lock` instead of `tryLock` (#25); show error feedback when a message exceeds the 10K char limit (#20).
- Notes: surface save/delete errors instead of silently dropping them (#42).
- Content: sanitize `packId` against path traversal in `ContentPackManager` (#34); apply `sanitizePath()` to simulated download destinations (#40).
- Database: write compacted/auto-compacted context to DB before updating the UI (#21, #31, #35, #39); make `autoCompactIfNeeded` atomic with `@Transaction` (#16).
- Native: cap `GgufMetadata` array count and guard `Long.toInt()` overflow against malformed GGUF (#26, #29); broaden `LlamaBridge` static-init catch to `Throwable` (#27).
- Services: add 2-hour WakeLock timeout safety net to `DownloadService` (#30); hold WakeLock until downloads complete (#24); cancel `LocationTrackerService` CoroutineScope on destroy (#36).
- Manifest: disable `allowBackup` to prevent DB/file inconsistency (#46).

### Changed
- Switched the AI stack from MediaPipe/LiteRT to llama.cpp + MiniCPM5-1B; removed `LiteRTLMEngine` and MediaPipe `tasks-genai`.
- Strip punctuation and filter stopwords in `RAGEngine` keyword search; corrected token budget to the actual 4K context window.
