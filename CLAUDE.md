# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NOMAD Android is an offline survival knowledge app with on-device AI, inspired by Fallout 4's Pip-Boy. It features offline maps, AI chat, knowledge browsing, and emergency tools — all designed to work without internet connectivity.

## Build & Development Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run lint checks
./gradlew lint

# Run all unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew testDebugUnitTest --tests "com.nomad.android.data.ai.FallbackEngineTest"

# Clean build
./gradlew clean assembleDebug
```

Build requirements: JDK 17 (Temurin), Android SDK API 35, Build Tools 35.0.0.

## Architecture

**MVVM + Repository + Hilt DI**, single-module (`app/`) Jetpack Compose app.

### Layer Overview

```
UI (Compose Screens + ViewModels)
  ↓ StateFlow<UiState>
Repositories
  ↓
Data Sources (Room DAOs, ContentPackManager, AIEngine)
```

- **ViewModels** expose `StateFlow<XxxUiState>` containing a data class with all screen state. Screens collect via `collectAsStateWithLifecycle()`.
- **Repositories** wrap DAOs/managers and return `Result<T>` (custom sealed class in `data/Result.kt` — not kotlin.Result).
- **Hilt modules** in `di/`: `DatabaseModule` (Room, OkHttp), `AIModule` (device-aware AI engine selection), `RepositoryModule`.

### AI Engine System

`AIEngine` interface in `data/ai/` with three implementations:
- **LiteRTLMEngine** — On-device Gemma models via MediaPipe LiteRT-LM. Model variant selected by device RAM (>=6GB → E2B, >=2GB → 1B).
- **RAGEngine** — Retrieval-augmented generation wrapper around any AIEngine.
- **FallbackEngine** — Rule-based responses for survival topics (no model required).

Selection logic lives in `AIModule.kt` based on `ActivityManager.memoryInfo.totalMem`.

### Navigation

Six routes defined in `ui/navigation/Routes.kt`: Dashboard, Maps, Knowledge, Chat, Emergency, Settings. Compose Navigation with `NavHost` in `ui/navigation/NavHost.kt`. App shell (`NomadApp.kt`) wraps content with PipBoyStatusBar (top) and PipBoyBottomNav (bottom).

### Theme & Styling

Pip-Boy retro terminal aesthetic: green monochrome palette (`#00FF00` primary), monospace typography, CRT effects (scanline overlay, flicker animation). All in `ui/theme/` — `Color.kt`, `Theme.kt`, `Type.kt`, `PipBoyEffects.kt`. Reusable styled components in `ui/components/PipBoyComponents.kt`.

### Database

Room database (`NomadDatabase`, version 1) with 5 entities: `ChatMessageEntity`, `ChatSessionEntity`, `ContentPackEntity`, `SearchHistoryEntity`, `SettingsEntity`. Uses destructive migration fallback. Schema export enabled to `app/schemas/`.

### Content Packs

`ContentPackManager` in `data/content/` manages downloadable offline content (Wikipedia, maps, AI models, books). Storage at `context.filesDir/contentPacks/`.

## Key Conventions

- All Compose UI is dark-theme only with the Pip-Boy green color scheme
- State classes follow the pattern: `XxxUiState(data: XxxData)` per screen
- Error handling uses `com.nomad.android.data.Result<T>` (not kotlin.Result)
- KSP is used for annotation processing (Room, Hilt) — not KAPT
- Kotlin 2.0.21 with Compose compiler plugin (not the old `kotlinCompilerExtensionVersion`)
- ProGuard enabled for release builds with keeps for Room, Hilt, Coroutines, TFLite, MediaPipe, MapLibre, OkHttp

## CI

GitHub Actions (`.github/workflows/ci.yml`): lint → unit tests → debug APK build. Triggered on push/PR to main.
