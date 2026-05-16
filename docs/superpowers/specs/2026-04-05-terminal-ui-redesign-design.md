# Terminal UI Redesign Spec

## Overview

Redesign the NOMAD Android app UI from the current Pip-Boy-themed interface to a clean, readable retro terminal interface. The color scheme stays green-on-dark. All Pip-Boy/Vault-Tec references are removed. The app shell, typography, components, navigation, and screen layouts are all refined for clarity and usability.

## Design Principles

- **Readable first** — Text sizes, spacing, and contrast prioritize legibility over aesthetic density
- **Clean retro** — Terminal aesthetic without clutter, CRT effects are subtle
- **Consistent spacing** — 16dp root padding, 16dp between cards, 12dp internal card spacing, 8dp within tight groups
- **No dead code** — Remove unused color constants, no-op modifiers, and unused Scaffold

## 1. App Shell & Layout

### Current Problems
- `Scaffold` adds system window inset padding creating a visible gap between bottom nav and screen edge
- `CrtScreen` wraps a manual Column with Scaffold's `innerPadding` applied externally
- Layout is not truly edge-to-edge

### Proposed Changes
- Remove `Scaffold` entirely from `NomadApp.kt`
- `CrtScreen` becomes the direct root with `fillMaxSize()`
- Layout: `Column(modifier = Modifier.fillMaxSize())` containing:
  - `TerminalStatusBar` (top, fixed height)
  - `NomadNavHost` (middle, `Modifier.weight(1f)`)
  - `TerminalBottomNav` (bottom, fixed height)
- Use `Modifier.systemBarsPadding()` on the root Column for safe area handling
- Bottom nav touches the actual screen bottom with zero gap

### Files Changed
- `app/src/main/java/com/nomad/android/NomadApp.kt` — Remove Scaffold, restructure layout

## 2. Typography

### Font
- Bundle `JetBrains Mono` (Apache 2.0 license, open-source, highly readable monospace)
- Font files placed in `app/src/main/res/font/`
- Replace all `FontFamily.Monospace` references with the new `JetBrainsMono` type family

### Size Hierarchy
| Slot | Current | New |
|------|---------|-----|
| `headlineLarge` | 28sp | 32sp |
| `headlineMedium` | 22sp | 24sp |
| `headlineSmall` | 18sp | 20sp |
| `titleLarge` | 18sp | 18sp |
| `titleMedium` | 16sp | 16sp |
| `bodyLarge` | 16sp | 16sp |
| `bodyMedium` | 14sp | 14sp |
| `bodySmall` | 12sp | 12sp |
| `labelLarge` | 14sp | 14sp |
| `labelMedium` | 12sp | 12sp |
| `labelSmall` | 10sp | 11sp |

- Line height: 1.4x for body, 1.2x for headlines, 1.3x for labels
- Font weight: Regular (400) for body, Medium (500) for titles, Bold (700) for headlines

### Files Changed
- `app/src/main/res/font/` — Add JetBrains Mono TTF files (regular, medium, bold)
- `app/src/main/java/com/nomad/android/ui/theme/Type.kt` — Complete rewrite with JetBrains Mono

## 3. Color System

### Consolidation
Remove duplicate palette. Single consolidated set:

| Constant | Hex | Usage |
|----------|-----|-------|
| `TerminalGreen` | `#14F195` | Primary text, borders, accents |
| `TerminalGreenDim` | `#0A7A4C` | Secondary text, inactive states |
| `TerminalAmber` | `#FFB000` | Warnings, emergency, secondary accent |
| `TerminalAmberDim` | `#806000` | Dim amber |
| `TerminalBlue` | `#4A90D9` | Tertiary accent (optional screens) |
| `TerminalBlueDim` | `#2A5080` | Dim blue |
| `TerminalDanger` | `#FF3333` | Errors, destructive actions |
| `TerminalBg` | `#0C0C0C` | App background |
| `TerminalSurface` | `#1A1A1A` | Card/surface background |
| `TerminalBorder` | `#2AFF6A` | Border color (brighter than dim, softer than full green) |
| `TerminalOnBg` | `#14F195` | Text on background |
| `TerminalOnSurface` | `#14F195` | Text on surface |
| `TerminalBorder` | `#2AFF6A` | Border color (brighter than dim, softer than full green) |
| `TerminalOnBg` | `#14F195` | Text on background |
| `TerminalOnSurface` | `#14F195` | Text on surface |

Remove: `Primary`, `PrimaryDim`, `Accent`, `Danger`, `Background`, `Surface`, `OnBackground`, `OnSurface` (dead duplicates).

### Theme.kt
- `darkColorScheme` uses the consolidated `Terminal*` constants
- No light theme (intentional)

### Files Changed
- `app/src/main/java/com/nomad/android/ui/theme/Color.kt` — Consolidate palette
- `app/src/main/java/com/nomad/android/ui/theme/Theme.kt` — Use consolidated palette

## 4. CRT Effects

### Refinements
- Scanlines: 4dp spacing (was 3dp), 3% opacity (was 6%)
- Flicker: 800ms cycle (was 150ms), 0.98-1.0 alpha range (was 0.95-1.0)
- `phosphorGlow()` — implement as a subtle green shadow (`Shadow(color = TerminalGreen, blurRadius = 2dp)`) applied to primary text, or remove entirely if the visual cost outweighs the benefit after testing

### Files Changed
- `app/src/main/java/com/nomad/android/ui/theme/PipBoyEffects.kt` — Refine effects, rename to `TerminalEffects.kt`

## 5. Component System

All components renamed from `PipBoy*` to `Terminal*`.

### TerminalButton
- Default: 2dp green border, 8dp rounded corners, transparent bg, green text, 14sp
- Pressed: filled green bg, dark text
- Three sizes: `Large` (full-width, 48dp min height), `Medium` (40dp), `Small` (32dp)
- Color variants via `tintColor` parameter (default: green, options: amber, danger)
- Disabled state: dim border, dim text, no press interaction
- Ripple effect matches button shape (8dp rounded rectangle)

### TerminalCard
- 2dp border (`TerminalBorder`), 8dp rounded corners
- Background: `TerminalSurface`
- Internal padding: 12dp
- Optional `header` slot with automatic divider below
- Content slot for arbitrary children

### TerminalTextField
- Full bordered box (not underline-only)
- 2px border, 6dp rounded corners, 12dp internal padding
- Label above field, placeholder inside
- Focus state: border brightens to full `TerminalGreen`
- Min height: 48dp

### TerminalListTile
- Min height: 48dp touch target
- Title (14sp) + optional subtitle (12sp dim) + trailing chevron
- 12dp horizontal padding, 10dp vertical padding
- Press state: subtle background highlight
- Divider between items (optional)

### TerminalProgressBar
- Segmented bar: 12 segments (was 20), 2dp gaps, 4dp rounded segment corners
- Optional percentage label above bar
- Color variants: green (default), amber, danger

### TerminalSectionHeader
- New component: 16sp bold uppercase text with bottom divider
- Used to separate sections within screens
- Consistent visual anchor across all screens

### TerminalDivider
- 1dp full-width line, `TerminalGreenDim` color
- Optional horizontal padding parameter

### Files Changed
- `app/src/main/java/com/nomad/android/ui/components/PipBoyComponents.kt` → `TerminalComponents.kt`
- All screens updated to use new component names

## 6. Bottom Navigation

### Design
- Remove Material3 `NavigationBar` and `NavigationBarItem`
- Custom `Row` with 5 equal-weight `Box` cells (later 6 with Settings)
- Each cell: Column with icon (24dp) above label (11sp uppercase)
- Selected state: 2dp `TerminalGreen` border around entire cell, 4dp rounded corners
- Unselected: no border, dim green icon and text
- Ripple effect matches cell shape
- Min 48dp touch target per cell
- Container: `TerminalSurface` background, top border only

### Tabs
- Dashboard, Maps, Knowledge, Chat, Emergency, Settings (added)
- Route strings remain unchanged for navigation compatibility

### Files Changed
- `app/src/main/java/com/nomad/android/ui/theme/PipBoyEffects.kt` — Replace `PipBoyBottomNav` with `TerminalBottomNav`

## 7. Status Bar

### Changes
- Rename `PipBoyStatusBar` to `TerminalStatusBar`
- Remove "VAULT-TEC" text, replace with app name "NOMAD"
- Keep: clock, AI status dot, storage indicator
- Cleaner layout: left (app name), center (clock), right (status indicators)
- Same height, same border style

### Files Changed
- `app/src/main/java/com/nomad/android/ui/theme/PipBoyEffects.kt` — Rename and refine

## 8. Screen Layouts

### Dashboard
- Three sections with `TerminalSectionHeader`:
  1. "SYSTEM STATUS" — status card with AI, GPS, storage indicators
  2. "QUICK ACCESS" — refined 2x2 grid (Maps, Knowledge, Chat, Emergency)
  3. "RECENT ACTIVITY" — list of recent items
- 16dp root padding, 16dp between sections, 12dp internal card spacing

### Maps
- Grouped cards: GPS coordinates, tracking controls, save location form
- Clean `TerminalTextField` for location name input (replaces inline BasicTextField)
- Saved locations list with `TerminalListTile`
- Layer toggle as segmented control

### Knowledge
- `TerminalTextField` for search at top
- Horizontal scrollable category chips (clean bordered style)
- Expandable article cards with `TerminalListTile` rows
- `LazyColumn` for article list

### Chat
- Header with session info
- `LazyColumn` for messages
- Message bubbles: left border accent (green for user, amber for AI), clean background
- Input area: `TerminalTextField` + send button
- Typing indicator refined

### Emergency
- Expandable amber-bordered cards for first aid topics
- Survival checklist with properly sized checkboxes (min 48dp touch targets)
- 16dp spacing throughout
- Consider adding ViewModel for persistence

### Settings
- Consistent card spacing (use `Arrangement.spacedBy(16.dp)`)
- Section headers: "AI ENGINE", "CONTENT", "STORAGE", "ABOUT"
- AI engine status card, content pack management, storage bar, about section
- Remove non-functional theme selector or implement it

### Onboarding
- Remove hardcoded color constants, use theme colors
- Keep 5-step flow but with refined components

### Files Changed
- All screen files in `app/src/main/java/com/nomad/android/ui/*/`

## 9. File Renames

| Old Path | New Path |
|----------|----------|
| `ui/components/PipBoyComponents.kt` | `ui/components/TerminalComponents.kt` |
| `ui/theme/PipBoyEffects.kt` | `ui/theme/TerminalEffects.kt` |

All import statements across the codebase updated accordingly.

## 10. Non-Goals

- No light theme added
- No new screens or routes added (except Settings in bottom nav)
- No changes to business logic, repositories, or database
- No changes to AI engine system
- No changes to navigation routing logic
- No new dependencies beyond JetBrains Mono font files (bundled as resources, not a library)
