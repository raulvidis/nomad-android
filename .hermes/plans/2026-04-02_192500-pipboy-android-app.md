# NOMAD Android — Pip-Boy Edition: Implementation Plan

**App Concept:** Fallout 4 Pip-Boy themed offline survival/knowledge Android app with on-device AI (Gemma 4), offline maps, Wikipedia, and emergency content.

**Theme:** Fallout 4 Pip-Boy — green-on-black CRT terminal aesthetic, retro-futuristic UI, scanline effects, amber/green phosphor glow, Vault-Tec branding.

---

## Phase 1: Project Scaffold & Core UI Theme (Steps 1-4)

### Step 1: Android Project Scaffold
- Create Android project with Kotlin + Jetpack Compose
- Min SDK 26, Target SDK 36
- Package: `com.nomad.android`
- Add core dependencies to build.gradle: Compose BOM, Navigation Compose, Room, Coroutines, Hilt
- Set up project structure:
  ```
  app/
  ├── src/main/java/com/nomad/android/
  │   ├── MainActivity.kt
  │   ├── NomadApp.kt (Application class)
  │   ├── ui/
  │   │   ├── theme/          — Pip-Boy theme (colors, typography, shapes)
  │   │   ├── components/     — Shared UI components
  │   │   ├── navigation/     — NavHost, routes
  │   │   ├── dashboard/      — Dashboard screen
  │   │   ├── maps/           — Maps screen
  │   │   ├── knowledge/      — Wikipedia/knowledge screen
  │   │   ├── chat/           — AI chat screen
  │   │   └── emergency/      — Emergency/SOS screen
  │   ├── data/
  │   │   ├── local/          — Room DB, DAOs, entities
  │   │   ├── ai/             — AI engine (AICore + LiteRT-LM)
  │   │   ├── maps/           — Map data management
  │   │   └── content/        — Content pack management
  │   └── di/                 — Hilt modules
  └── src/main/res/
      ├── values/
      │   ├── colors.xml
      │   ├── strings.xml
      │   └── themes.xml
      ├── drawable/           — Pip-Boy assets
      ├── font/               — Monospace/retro fonts
      └── raw/                — Static survival content (JSON)
  ```

### Step 2: Pip-Boy Theme System
- Define Pip-Boy color palette:
  - Primary: `#14F195` (bright CRT green)
  - Primary Dim: `#0A7A4C` (dim green for secondary elements)
  - Accent: `#FFB000` (amber/gold for warnings, alerts)
  - Danger: `#FF3333` (red for emergency)
  - Background: `#0C0C0C` (near-black CRT)
  - Surface: `#1A1A1A` (dark card background)
  - Scanline: `#14F195` at 8% opacity
- Typography: Monospace font family (Roboto Mono or bundled retro font)
- Custom Compose theme: `NomadTheme` with Pip-Boy `ColorScheme`, `Typography`, custom `Shapes`
- CRT scanline overlay composable (repeating horizontal lines at 2px intervals)
- Phosphor glow effect (green shadow/blur on text)
- Pip-Boy boot animation (flicker-on effect)

### Step 3: Navigation & Shell
- Set up Jetpack Navigation Compose with 5 main routes:
  - Dashboard ("/")
  - Maps ("/maps")
  - Knowledge ("/knowledge")
  - AI Chat ("/chat")
  - Emergency ("/emergency")
- Bottom navigation bar styled as Pip-Boy tab buttons (tab1-tab5 with icons)
- Main screen scaffold with Pip-Boy status bar (top: VATS-style indicators for battery, storage, AI status)
- Side drawer or rotary dial metaphor for settings

### Step 4: Pip-Boy UI Components Library
- `PipBoyButton` — Rectangle button with green border, scanline fill on press
- `PipBoyCard` — Dark card with green border, corner notch decorations
- `PipBoyText` — Text with phosphor glow effect
- `PipBoyTextField` — Input with green underline, monospace font
- `PipBoyProgressBar` — Horizontal segmented bar (VATS-style)
- `PipBoyStatusIndicator` — Blinking dot + label for system status
- `PipBoyDivider` — Horizontal green line with gap/notch
- `PipBoyListTile` — Row item with arrow indicator
- `ScanlineOverlay` — Full-screen scanline effect modifier
- `CrtFlicker` — Animation effect for screen transitions
- `PipBoyTopBar` — Status bar with "VAULT-TEC" branding, date/time, system indicators

---

## Phase 2: Dashboard & Content System (Steps 5-7)

### Step 5: Dashboard Screen
- Pip-Boy main menu layout (like the actual Pip-Boy home screen)
- Status section: AI model loaded, storage used/free, content packs status
- Quick-access cards for each module (Maps, Knowledge, Chat, Emergency)
- Recent activity feed (last searches, chats, viewed articles)
- Download manager card (available content packs, download progress)

### Step 6: Room Database Setup
- Entities:
  - `ContentPack` (id, name, type, size, status, downloadedAt)
  - `SearchHistory` (id, query, source, timestamp)
  - `ChatMessage` (id, sessionId, role, content, timestamp)
  - `ChatSession` (id, title, createdAt)
  - `FavoriteArticle` (id, title, source, zimPath)
  - `MapRegion` (id, name, bounds, fileSize, status)
  - `Settings` (key, value)
- DAOs for each entity
- RoomDatabase class with migrations
- Hilt module for database injection

### Step 7: Content Pack Manager
- ContentPackManager class to manage downloads and status
- Built-in emergency content (bundled in APK as JSON):
  - First aid protocols
  - SOS signals
  - Shelter building
  - Water purification
  - Fire starting
  - Knot tying
  - Navigation by stars
- Remote content pack definitions (fetched once on first launch):
  - Wikipedia packs (mini, nopic, full)
  - Map regions
  - AI model downloads (LiteRT-LM fallback)
- Download progress tracking with WorkManager
- Pack verification (checksum after download)

---

## Phase 3: AI Engine (Steps 8-11)

### Step 8: AI Engine Interface & AICore Implementation
- `AIEngine` interface:
  ```kotlin
  interface AIEngine {
      suspend fun generate(prompt: String, system: String?): String
      fun generateStream(prompt: String, system: String?): Flow<String>
      fun isAvailable(): Boolean
      fun getModelName(): String
  }
  ```
- `AICoreEngine` implementation using `com.google.ai.edge.aicore`
- Check AICore availability at runtime
- Handle model download triggers if needed
- Error handling for SERVICE_DISCONNECTED, etc.

### Step 9: LiteRT-LM Fallback Engine
- `LiteRTLMEngine` implementation using MediaPipe LiteRT-LM
- Model file management (download to internal storage)
- GPU acceleration detection (OpenCL/Vulkan)
- Model selection logic: E2B for 6GB+ RAM, Gemma 3 1B for 4GB+
- RAM detection and auto-recommendation

### Step 10: RAG Engine
- `RAGEngine` class:
  ```kotlin
  class RAGEngine(db: SQLiteDatabase, embedder: Embedder, aiEngine: AIEngine) {
      suspend fun query(question: String, topK: Int = 5): Flow<String>
  }
  ```
- sqlite-vec integration:
  - Load .so extension into Room's SQLite
  - Create vec0 virtual table for embeddings
  - cosine similarity search
- Embedding model: small MobileBERT-based embedder via MediaPipe
- Chunking service: split content into 512-token chunks with 128 overlap
- Pre-embedding pipeline: runs during content pack download in background
- Query flow: embed query → vector search → retrieve chunks → build prompt → stream response

### Step 11: AI Chat Screen
- Pip-Boy terminal-style chat UI
- Green monospace text on black background
- Typing animation (character-by-character reveal, like a terminal)
- Chat history with session management
- "QUERY" prefix for user messages, "RESPONSE" prefix for AI
- Context selector: choose which content packs to include in RAG context
- Streaming response display
- Sound effects (optional): keyboard clicks, terminal beeps

---

## Phase 4: Maps (Steps 12-13)

### Step 12: MapLibre Integration
- Add MapLibre GL Native Android dependency
- Custom map style JSON: dark theme with green/amber elements (Pip-Boy aesthetic)
  - Water: dark teal (#0A2A2A)
  - Land: very dark gray (#0C0C0C)
  - Roads: dim green (#0A5A3A)
  - Major roads: bright green (#14F195)
  - Buildings: dark surface (#1A1A1A)
  - Labels: phosphor green with glow
  - POIs (hospitals, shelters): amber (#FFB000)
  - Emergency POIs: red (#FF3333)
- PMTiles support for offline regions
- Offline region download and management
- Custom map controls styled as Pip-Boy buttons

### Step 13: Maps Screen & Features
- Full-screen map with Pip-Boy styled controls
- Layer toggles (basemap, POIs, topography, emergency)
- Offline search via bundled SQLite geocoder
- Location pins for emergency POIs (hospitals, shelters, water sources, gas stations)
- "Near me" radar view (circular VATS-style proximity indicator)
- Coordinate display in Pip-Boy status format
- Compass rose with Pip-Boy styling

---

## Phase 5: Knowledge Browser (Steps 14-15)

### Step 14: Kiwix/libkiwix Integration
- Build libkiwix for Android NDK (arm64-v8a, armeabi-v7a)
- Create JNI wrapper for:
  - Opening ZIM files
  - Searching articles (title + full-text)
  - Getting article HTML
- WebView-based article renderer with Pip-Boy CSS override (dark theme, green text, monospace)
- Content provider that bridges libkiwix to Android

### Step 15: Knowledge Screen
- Browse screen: category tabs (Wikipedia, Survival, First Aid, Books)
- Search bar with autocomplete (Pip-Boy styled)
- Article viewer with custom CSS (green-on-black, monospace, scanlines)
- Favorites/bookmarks
- "Related articles" suggestions
- Font size controls
- Survival guides with step-by-step layout (numbered steps, checklists)

---

## Phase 6: Emergency Screen (Step 16)

### Step 16: Emergency Screen
- Big red "SOS" button with pulsing animation
- Quick-access cards:
  - First Aid (alphabetical condition list)
  - SOS Signals (visual reference)
  - Nearest Hospital (from offline map)
  - Emergency Contacts (user-configured)
  - Survival Checklist
- One-tap access to critical info (no scrolling, no loading)
- Always available (bundled in APK, no download needed)
- Flashlight toggle
- Loud whistle/alarm sound

---

## Phase 7: First Launch & Polish (Steps 17-18)

### Step 17: First Launch Flow
- Pip-Boy boot animation (power-on sequence)
- Device capability scan (RAM, AICore, storage)
- AI model selection (auto-recommended based on device)
- Download starter pack (Essentials ~500MB)
- Progress screen with Pip-Boy styled progress bars
- Welcome screen with app overview

### Step 18: Settings & Polish
- Settings screen (Pip-Boy styled):
  - AI model selection
  - Content pack management (download/delete)
  - Map region management
  - Storage usage display
  - Theme options (green CRT, amber CRT, blue CRT)
  - About screen (Vault-Tec themed)
- Notification system for downloads
- App icon (Pip-Boy style vault icon)
- Splash screen with boot animation

---

## Execution Order (for OpenCode tasks)

Each step is one OpenCode run. Sequential dependencies marked with →.

**Batch 1 (parallel — no dependencies):**
- Step 1: Project scaffold

**Batch 2 (depends on Step 1):**
- Step 2: Pip-Boy theme system
- Step 6: Room database setup

**Batch 3 (depends on Steps 1+2):**
- Step 3: Navigation & shell
- Step 4: UI components library

**Batch 4 (depends on Steps 3+4):**
- Step 5: Dashboard screen
- Step 11: AI chat screen (UI only, engine mocked)
- Step 13: Maps screen (UI only, engine mocked)
- Step 15: Knowledge screen (UI only)
- Step 16: Emergency screen

**Batch 5 (depends on Step 6):**
- Step 7: Content pack manager
- Step 10: RAG engine

**Batch 6 (parallel engines):**
- Step 8: AICore engine
- Step 9: LiteRT-LM engine
- Step 12: MapLibre integration

**Batch 7 (integration):**
- Step 14: Kiwix/libkiwix integration
- Step 17: First launch flow
- Step 18: Settings & polish

---

## Files to Create (key files)

```
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/com/nomad/android/
├── MainActivity.kt
├── NomadApp.kt
├── ui/theme/
│   ├── Color.kt          — Pip-Boy colors
│   ├── Type.kt           — Monospace typography
│   ├── Theme.kt          — NomadTheme composable
│   └── PipBoyEffects.kt  — Scanlines, glow, flicker
├── ui/components/
│   ├── PipBoyButton.kt
│   ├── PipBoyCard.kt
│   ├── PipBoyText.kt
│   ├── PipBoyTextField.kt
│   ├── PipBoyProgressBar.kt
│   ├── PipBoyStatusIndicator.kt
│   ├── PipBoyDivider.kt
│   ├── PipBoyListTile.kt
│   └── ScanlineOverlay.kt
├── ui/navigation/
│   ├── NavHost.kt
│   └── Routes.kt
├── ui/dashboard/
│   └── DashboardScreen.kt
├── ui/maps/
│   └── MapsScreen.kt
├── ui/knowledge/
│   └── KnowledgeScreen.kt
├── ui/chat/
│   └── ChatScreen.kt
├── ui/emergency/
│   └── EmergencyScreen.kt
├── data/local/
│   ├── NomadDatabase.kt
│   ├── dao/
│   │   ├── ContentPackDao.kt
│   │   ├── ChatMessageDao.kt
│   │   ├── SearchHistoryDao.kt
│   │   └── SettingsDao.kt
│   └── entity/
│       ├── ContentPackEntity.kt
│       ├── ChatMessageEntity.kt
│       ├── SearchHistoryEntity.kt
│       ├── ChatSessionEntity.kt
│       ├── FavoriteArticleEntity.kt
│       ├── MapRegionEntity.kt
│       └── SettingsEntity.kt
├── data/ai/
│   ├── AIEngine.kt       — interface
│   ├── AICoreEngine.kt
│   ├── LiteRTLMEngine.kt
│   └── RAGEngine.kt
├── data/content/
│   ├── ContentPackManager.kt
│   └── EmbeddingService.kt
├── data/maps/
│   └── MapManager.kt
├── di/
│   ├── DatabaseModule.kt
│   ├── AIModule.kt
│   └── ContentModule.kt
app/src/main/res/
├── values/colors.xml
├── values/strings.xml
├── values/themes.xml
├── font/roboto_mono_*.ttf (or similar)
├── raw/survival_content.json
└── drawable/pipboy_*.xml (vector drawables)
```
