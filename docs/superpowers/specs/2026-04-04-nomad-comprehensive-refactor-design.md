# NOMAD Android — Comprehensive Refactor Design Spec

**Date**: 2026-04-04
**Status**: Draft — Pending Review
**Author**: opencode

---

## 1. Overview

This spec covers a comprehensive refactor of the NOMAD Android app — an offline survival knowledge app themed after Fallout 4's Pip-Boy terminal. The refactor addresses all 14 identified issues from the codebase analysis, transforming the app from a prototype skeleton into a production-ready architecture.

### Goals
- Add proper ViewModel + Repository architecture
- Implement real functionality (AI, maps, downloads, knowledge)
- Fix all infrastructure issues (database, proguard, build config)
- Add error/loading/empty states to all screens
- Set up testing and CI/CD
- Fix all code quality issues

### Non-Goals
- Server-side infrastructure
- User authentication
- Social features

---

## 2. Architecture: ViewModels + Repository Layer

### 2.1 Repository Layer

Create repositories in `app/src/main/java/com/nomad/android/data/repository/`:

| Repository | Responsibilities | Dependencies |
|---|---|---|
| `ChatRepository` | Chat session CRUD, message persistence, recent sessions | `ChatMessageDao`, `ChatSessionDao` |
| `ContentPackRepository` | Pack listing, download with progress, pause/resume, deletion | `ContentPackDao`, `OkHttp`, `WorkManager` |
| `SearchRepository` | Article search, category filtering, search history, ZIM reading | `SearchHistoryDao`, `KiwixManager`, `ContentPackDao` |
| `SettingsRepository` | Key-value settings, onboarding state, theme persistence, storage metrics | `SettingsDao`, `DataStore Preferences`, `ContentPackDao` |
| `MapsRepository` | Offline tile management, layer configuration, map state | `MapLibre`, local tile storage |

Each repository:
- Annotated with `@Singleton` and `@Inject` constructor
- Exposes data as `Flow<T>` or `suspend fun`
- Handles error wrapping in sealed `Result<T>` types
- No direct DAO exposure to UI layer

### 2.2 ViewModel Layer

Create ViewModels in `app/src/main/java/com/nomad/android/ui/<feature>/`:

| ViewModel | State | Key Actions |
|---|---|---|
| `DashboardViewModel` | `DashboardUiState` (system status, storage, activity) | `refreshStatus()`, `getStorageMetrics()` |
| `MapsViewModel` | `MapsUiState` (layers, offline status, map config) | `toggleLayer()`, `downloadTiles()`, `initMap()` |
| `KnowledgeViewModel` | `KnowledgeUiState` (articles, categories, search, loading/error) | `search()`, `selectCategory()`, `loadArticle()` |
| `ChatViewModel` | `ChatUiState` (messages, sessions, typing, streaming) | `sendMessage()`, `newSession()`, `loadSession()` |
| `EmergencyViewModel` | `EmergencyUiState` (SOS active, contacts, checklist) | `toggleSOS()`, `acknowledgeChecklist()` |
| `SettingsViewModel` | `SettingsUiState` (engine info, packs, theme, storage) | `selectEngine()`, `downloadPack()`, `setTheme()` |
| `OnboardingViewModel` | `OnboardingUiState` (step, hardware info, selected model) | `nextStep()`, `completeOnboarding()` |

### 2.3 State Pattern

Each ViewModel exposes:
```kotlin
data class UiState<out T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

Screens collect `StateFlow<UiState<T>>` via `collectAsStateWithLifecycle()`.

### 2.4 Onboarding Persistence

- Replace `rememberSaveable` in `NomadApp` with `DataStore Preferences`
- `SettingsRepository.isOnboardingComplete(): Flow<Boolean>`
- `SettingsRepository.completeOnboarding()` persists flag
- Onboarding shows only on first launch, survives process death and app kills

---

## 3. Real Implementations

### 3.1 AI Engine (LiteRT-LM)

**Remove AICore entirely** — no dependency on Google AICore SDK.

**`LiteRTLMEngine` improvements**:
- Integrate MediaPipe LiteRT-LM SDK properly
- Two model variants:
  - **Gemma 4 E2B** — requires >= 6144MB RAM
  - **Gemma 3 1B** — requires >= 2048MB RAM
- Real `generateStream()` returns `Flow<String>` with token-by-token streaming
- Proper model loading/unloading lifecycle
- Error handling for OOM, model not found, device incompatibility

**Fallback** (`FallbackEngine`):
- If no LiteRT-LM model can load, provide a local rule-based response system
- Uses the same `res/raw/survival_content.json` bundled with the app for keyword-matched responses
- Clearly indicates to user that AI model is unavailable

**`AIEngine` interface** (updated):
```kotlin
interface AIEngine {
    suspend fun generate(prompt: String, context: List<String>): String
    fun generateStream(prompt: String, context: List<String>): Flow<String>
    suspend fun isAvailable(): Boolean
    fun getModelName(): String
    fun getDeviceInfo(): DeviceInfo
    suspend fun loadModel(): Result<Unit>
    fun unloadModel()
}
```

**`AIModule` fix**:
```kotlin
@Provides
fun provideAIEngine(...): AIEngine {
    return when {
        totalRamMB >= 6144 -> LiteRTLMEngine(ModelVariant.E2B)
        totalRamMB >= 2048 -> LiteRTLMEngine(ModelVariant.ONE_B)
        else -> FallbackEngine()
    }
}
```

### 3.2 Real Downloads

**`ContentPackRepository.downloadPack()`**:
- Uses OkHttp for HTTP downloads
- Streams progress via `Flow<Float>` (0.0 to 1.0)
- Downloads to `context.filesDir/contentPacks/`
- Supports pause (cancel OkHttp call) and resume (HTTP Range header)
- Updates `ContentPackEntity` status in Room on completion
- WorkManager for background downloads that survive app death

**`KiwixManager` improvements**:
- Integrate libkiwix JNI bindings for ZIM archive reading
- Real `search()` queries ZIM full-text index
- Real `getArticle()` extracts HTML from ZIM
- `downloadArchive()` uses same OkHttp pipeline as content packs

### 3.3 Real Maps

**MapLibre GL Native integration**:
- Add `org.maplibre.gl:android-sdk:10.3.0` dependency (stable v10 API)
- `MapView` wrapped in `AndroidView` composable
- Offline tile management:
  - Pre-bundled tiles in `assets/maps/` for initial offline capability
  - Download additional tile packs via `MapsRepository`
- Layer system:
  - Basemap layer (always on)
  - POI layer (toggleable — shelters, water sources, hospitals)
  - Topo layer (toggleable — contour lines)
- Fallback: if MapLibre fails to initialize, show static offline map image with grid coordinates

### 3.4 Real Knowledge Loading

**Initial content**:
- Load `res/raw/survival_content.json` on first launch
- Parse and insert into Room as initial articles
- Each article gets: title, category, content, source, isFavorite

**ZIM articles**:
- When content pack with ZIM is downloaded, `KiwixManager` indexes it
- Articles searchable via full-text search
- Category metadata extracted from ZIM namespace

**Search**:
- Real full-text search through Room FTS or ZIM index
- Search history persisted in `SearchHistoryEntity`
- Results ranked by relevance

**RAGEngine vector search**:
- Replace raw `SQLiteDatabase` vector store with Room-backed table
- Store embeddings as BLOB columns in a `VectorEmbeddingEntity` table
- Cosine similarity computed in Kotlin on retrieved vectors
- Text chunking (512 words, 128 overlap) feeds into embedding pipeline
- Used by `ChatRepository` to provide context-aware AI responses from local knowledge base

---

## 4. Database & Infrastructure

### 4.1 Room Database

**Schema export**:
```kotlin
@Database(
    entities = [...],
    version = 1,
    exportSchema = true
)
```
- Export to `app/schemas/` directory
- Add to version control for migration tracking

**Database callback**:
```kotlin
class NomadDatabaseCallback(
    private val contentPackManager: ContentPackManager
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Seed survival content from JSON
    }
}
```

**Migration strategy**:
- v1 is the baseline — no migrations needed yet
- Future schema changes require `Migration` objects
- Add `fallbackToDestructiveMigration()` only for dev builds

### 4.2 ProGuard Rules

Create `app/proguard-rules.pro`:
```proguard
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt/Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# LiteRT-LM / MediaPipe
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.ai.edge.** { *; }

# MapLibre
-keep class org.maplibre.gl.** { *; }
-keep class com.mapbox.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson / Kotlinx Serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.nomad.android.data.** { *; }
```

### 4.3 Build Config

**`gradle.properties`**:
- Remove `android.enableJetifier=true` (all deps are AndroidX)
- Keep `android.nonTransitiveRClass=true`
- Keep `kotlin.code.style=official`

**`app/build.gradle.kts`**:
- `compileSdk = 35` (stable Android 15)
- `targetSdk = 35`
- `minSdk = 26`
- Add new dependencies: OkHttp, WorkManager, MapLibre, Turbine (test)

**New dependencies**:
```kotlin
// Networking
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Background work
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Maps
implementation("org.maplibre.gl:android-sdk:11.0.0")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.1")

// Testing
testImplementation("app.cash.turbine:turbine:1.2.0")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

### 4.4 CRT Flicker Fix

**Before** (impure, causes excessive recomposition):
```kotlin
fun Modifier.crtFlicker(): Modifier = this.then(Modifier.graphicsLayer {
    alpha = 0.95f + (System.currentTimeMillis() % 100) / 1000f
})
```

**After** (proper Compose animation using Modifier.composed):
```kotlin
fun Modifier.crtFlicker(): Modifier = this.then(Modifier.composed {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 150),
            repeatMode = RepeatMode.Reverse
        )
    )
    Modifier.graphicsLayer { this.alpha = alpha }
})
```

Same pattern for `PipBoyStatusBar` clock — replace `while(true)` with `LaunchedEffect` + `delay(1000)` updating a `remember` state.

### 4.5 Color Dedup

**Single source of truth** in `ui/theme/Color.kt`:
```kotlin
val PipBoyGreen = Color(0xFF14F195)
val PipBoyGreenDim = Color(0xFF0A7A4C)
val PipBoyAmber = Color(0xFFFFB000)
val PipBoyAmberDim = Color(0xFF805800)
val PipBoyBlue = Color(0xFF00BFFF)
val PipBoyBlueDim = Color(0xFF006080)
val PipBoyDanger = Color(0xFFFF3333)
val PipBoyBg = Color(0xFF0C0C0C)
val PipBoySurface = Color(0xFF1A1A1A)
```

Remove all duplicate `val PipBoy*` constants from `PipBoyComponents.kt` and `PipBoyEffects.kt`. Import from `com.nomad.android.ui.theme` package.

### 4.6 Theme Selection

**Three complete color schemes** in `Theme.kt`:

| Theme | Primary | PrimaryDim | Accent | Background |
|---|---|---|---|---|
| CRT Green | `#14F195` | `#0A7A4C` | `#FFB000` | `#0C0C0C` |
| Amber Phosphor | `#FFB000` | `#805800` | `#14F195` | `#0C0C0C` |
| Blue Screen | `#00BFFF` | `#006080` | `#FFB000` | `#0C0C0C` |

- `SettingsRepository.setTheme(theme: PipBoyTheme)` persists choice to DataStore
- `SettingsRepository.getTheme(): Flow<PipBoyTheme>` provides current theme
- `NomadTheme` composable reads theme from ViewModel and applies correct color scheme
- All UI components respond to theme change immediately

### 4.7 Manifest

```xml
<application
    android:usesCleartextTraffic="false"
    ...>
```

---

## 5. Error States, Loading States, Empty States

### 5.1 State Design

Every screen handles four states:

| State | Visual | Trigger |
|---|---|---|
| `Loading` | CRT boot sequence animation (scanlines + "INITIALIZING..." text) | Initial data fetch |
| `Success` | Normal PipBoy-styled content | Data loaded successfully |
| `Error` | "ERROR: <message>" in red PipBoy text + "RETRY" button | Network failure, DB error, AI unavailable |
| `Empty` | "NO DATA FOUND" + suggestion text | Empty lists, no search results, no chat sessions |

### 5.2 Reusable State Components

Add to `PipBoyComponents.kt`:
- `PipBoyLoadingScreen(message: String)` — CRT boot animation
- `PipBoyErrorScreen(message: String, onRetry: () -> Unit)` — Error + retry
- `PipBoyEmptyScreen(message: String, action: String? = null, onAction: (() -> Unit)? = null)` — Empty state with optional action

### 5.3 Screen-Specific States

| Screen | Loading | Error | Empty |
|---|---|---|---|
| Dashboard | "SCANNING SYSTEMS..." | "SYSTEM DIAGNOSTIC FAILED" | N/A (always shows something) |
| Maps | "LOADING CARTOGRAPHY..." | "MAP MODULE OFFLINE" | "NO OFFLINE MAPS AVAILABLE" |
| Knowledge | "INDEXING ARCHIVES..." | "DATABASE CORRUPTION DETECTED" | "NO ARTICLES IN THIS CATEGORY" |
| Chat | "ESTABLISHING NEURAL LINK..." | "AI ENGINE UNAVAILABLE" | "NO MESSAGES — START A NEW SESSION" |
| Emergency | "ACTIVATING BEACON..." | "EMERGENCY SYSTEMS OFFLINE" | N/A (always shows contacts) |
| Settings | "READING CONFIG..." | "SETTINGS READ ERROR" | N/A |

---

## 6. Testing

### 6.1 Unit Tests

**ViewModel tests** (new):
- `DashboardViewModelTest` — state transitions, error handling
- `ChatViewModelTest` — message sending, streaming, session management
- `KnowledgeViewModelTest` — search, category filtering, loading states
- `SettingsViewModelTest` — theme changes, pack management
- `OnboardingViewModelTest` — step progression, completion flow

**Repository tests** (new):
- `ChatRepositoryTest` — with fake DAOs, verify CRUD operations
- `ContentPackRepositoryTest` — download flow, progress emission
- `SearchRepositoryTest` — search queries, history management

**Existing tests** (fixed):
- Merge `ContentPackManagerTest` and `ContentPackManagerFormatTest` into single file
- Remove reflection-based private method access — test public APIs only
- Use proper constructor injection in tests (no `null` params)

### 6.2 Compose UI Tests

- `OnboardingScreenTest` — full 5-step flow, completion persistence
- `ChatScreenTest` — send message, receive response, error state
- `KnowledgeScreenTest` — search, category filter, empty state

### 6.3 Test Dependencies

```kotlin
testImplementation("app.cash.turbine:turbine:1.2.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

---

## 7. CI/CD

### 7.1 GitHub Actions Workflow

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
          cache: gradle
      - name: Grant execute permission
        run: chmod +x gradlew
      - name: Run lint
        run: ./gradlew lint
      - name: Run unit tests
        run: ./gradlew testDebugUnitTest
      - name: Build debug APK
        run: ./gradlew assembleDebug
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: nomad-debug
          path: app/build/outputs/apk/debug/*.apk
```

---

## 8. File Structure (After Refactor)

```
app/src/main/java/com/nomad/android/
├── NomadApp.kt                          # Application class + root Composable (updated)
├── MainActivity.kt                      # Single Activity entry point
├── di/
│   ├── AIModule.kt                      # Updated (no AICore, proper thresholds)
│   ├── DatabaseModule.kt                # Updated (schema export)
│   └── RepositoryModule.kt              # NEW — repository bindings
├── data/
│   ├── ai/
│   │   ├── AIEngine.kt                  # Updated interface
│   │   ├── LiteRTLMEngine.kt            # Real implementation
│   │   ├── FallbackEngine.kt            # NEW — rule-based fallback
│   │   └── RAGEngine.kt                 # Updated (Room-backed vector store with cosine similarity)
│   ├── content/
│   │   ├── ContentPackManager.kt        # Updated (real downloads)
│   │   └── KiwixManager.kt             # Updated (real ZIM handling)
│   ├── local/
│   │   ├── NomadDatabase.kt             # Updated (schema export, callback)
│   │   ├── dao/                         # Unchanged structure
│   │   └── entity/                      # Unchanged structure
│   └── repository/                      # NEW
│       ├── ChatRepository.kt
│       ├── ContentPackRepository.kt
│       ├── MapsRepository.kt
│       ├── SearchRepository.kt
│       └── SettingsRepository.kt
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                     # Updated (3 themes)
│   │   ├── Color.kt                     # Updated (all colors, no dupes)
│   │   ├── Type.kt                      # Unchanged
│   │   └── PipBoyEffects.kt             # Updated (proper animations)
│   ├── components/
│   │   └── PipBoyComponents.kt          # Updated (state components, no dupe colors)
│   ├── navigation/
│   │   ├── NavHost.kt                   # Updated (ViewModel wiring)
│   │   └── Routes.kt                    # Unchanged
│   ├── dashboard/
│   │   ├── DashboardScreen.kt           # Updated (collects ViewModel)
│   │   └── DashboardViewModel.kt        # NEW
│   ├── chat/
│   │   ├── ChatScreen.kt                # Updated (collects ViewModel)
│   │   └── ChatViewModel.kt             # NEW
│   ├── knowledge/
│   │   ├── KnowledgeScreen.kt           # Updated (collects ViewModel)
│   │   └── KnowledgeViewModel.kt        # NEW
│   ├── maps/
│   │   ├── MapsScreen.kt                # Updated (real MapLibre)
│   │   └── MapsViewModel.kt             # NEW
│   ├── emergency/
│   │   ├── EmergencyScreen.kt           # Updated (collects ViewModel)
│   │   └── EmergencyViewModel.kt        # NEW
│   ├── settings/
│   │   ├── SettingsScreen.kt            # Updated (collects ViewModel, real themes)
│   │   └── SettingsViewModel.kt         # NEW
│   └── onboarding/
│       ├── OnboardingScreen.kt          # Updated (collects ViewModel)
│       └── OnboardingViewModel.kt       # NEW
```

---

## 9. Migration Plan

The refactor is implemented as a single cohesive change set, applied in sequential order within one branch. Each step builds on the previous and compiles independently:

1. **Foundation** — Build config, dependencies, proguard, color dedup, CRT fixes
2. **Data layer** — Repositories, updated Room config, real download pipeline
3. **ViewModels** — All 7 ViewModels with StateFlow state management
4. **UI wiring** — Connect all screens to ViewModels, add error/loading/empty states
5. **Real implementations** — LiteRT-LM, MapLibre, ZIM, OkHttp downloads
6. **Theme system** — Three complete themes with persistence
7. **Testing** — Unit tests, Compose tests, fix existing tests
8. **CI/CD** — GitHub Actions workflow
9. **Cleanup** — Remove unused code, fix imports, lint pass

---

## 10. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| LiteRT-LM SDK may require specific device hardware | FallbackEngine provides graceful degradation |
| MapLibre native libs increase APK size | Use ABI splits, proguard shrinks unused code |
| libkiwix JNI may have build complexity | Start with bundled JSON content, add ZIM incrementally |
| Large refactor may introduce regressions | Comprehensive test coverage, CI catches failures |
| compileSdk 35 downgrade from 36 | Audit all API usage, ensure no 36-only APIs are used |
