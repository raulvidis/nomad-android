# Navbar Fix + Offline Maps Design

## 1. Navbar Text Overflow Bug Fix

### Problem
6 bottom nav tabs with fixed `10.sp` uppercase labels in `Arrangement.SpaceEvenly`. On narrow screens, labels like "DASHBOARD", "KNOWLEDGE", "SETTINGS" wrap to 2 lines (e.g. "DASHBOA/RD").

### Solution
Show only icons by default. Display the label **only for the currently selected tab**. This eliminates all sizing issues and produces a cleaner retro terminal aesthetic.

### Changes
- **File:** `ui/theme/TerminalEffects.kt` — `TerminalBottomNav` composable
- Remove the `Text` label from unselected tabs
- Keep the label only when `selected == true`
- Add `maxLines = 1` and `TextOverflow.Clip` as safety net on the selected label

---

## 2. Offline Maps with Live Position

### Overview
Replace the current text-only Maps screen with a full-screen MapLibre map. Users can download map regions for offline use and see their live GPS position on the map. All UI controls are floating overlay panels styled in the terminal aesthetic.

### Architecture

```
MapsScreen (Compose)
  +-- MapLibre MapView (full screen)
  +-- CoordinatesOverlay (top bar: lat/lon)
  +-- LocationOverlay (pulsing green dot, auto-center toggle)
  +-- MapControlsOverlay (zoom buttons, layer toggle, download trigger)
  +-- SavedPointsOverlay (map markers + bottom sheet for saved points)
  +-- DownloadOverlay (region selection + progress UI)

MapsViewModel
  +-- MapLibre camera state, loaded tile sources
  +-- LocationRepository (existing GPS tracking)
  +-- OfflineTileManager (new)

OfflineTileManager
  +-- downloadRegion(bounds, zoomRange) : Flow<DownloadProgress>
  +-- getTileSource() : TileDataProvider for MapLibre offline source
  +-- getDownloadedRegions() : List<OfflineRegion>
  +-- deleteRegion(id)
  +-- hasTilesForBounds(bounds, zoom) : Boolean
```

### Components

#### MapLibre MapView
- Full-screen `MapView` wrapped in a Compose `AndroidView`
- Tile sources: online OSM raster tiles when connected, offline MBTiles when no network
- Default camera position: user's last known location or world view
- Style: minimal/dark raster tiles to match terminal aesthetic (OSM standard tile URL)

#### Offline Tile Storage
- Format: MBTiles (SQLite) — one file per downloaded region
- Storage path: `context.filesDir/mapTiles/<region-id>.mbtiles`
- Each MBTiles file stores raster PNG tiles for the selected zoom range
- Tile schema: standard Z/X/Y OSM slippy map coordinates
- Metadata table stores: region name, bounds (N/S/E/W), zoom range, download timestamp

#### Region Download Flow
1. User taps "Download" floating button on map
2. A draggable bounding box appears over the map
3. User adjusts the box and selects zoom levels (default: 10-15)
4. App calculates estimated tile count and download size (warn if >500MB)
5. Download proceeds tile-by-tile over HTTP, stored directly into MBTiles
6. Progress shown as terminal-styled overlay: `[████████░░] 80% | 1,240 / 1,550 tiles`
7. On completion, the region is immediately available for offline rendering

#### Live Position
- Blue dot replaced by **pulsing green dot** (terminal green `#00FF00`)
- Auto-center button: toggles camera follow mode
- When tracking is active, camera smoothly follows user position
- GPS accuracy shown as semi-transparent green circle around the dot

#### Floating Overlays
All overlays use the existing terminal theme colors (`TerminalGreen`, `TerminalBg`, `TerminalSurface`, etc.) with rounded corners and `TerminalGreenDim` borders.

- **Top bar:** Semi-transparent `TerminalBg` strip showing lat/lon coordinates in monospace
- **Right side:** Zoom in/out buttons, auto-center toggle, layer toggle
- **Bottom left:** "Download Region" button (only visible when no offline region covers current viewport)
- **Saved points:** Green markers on map. Tap to show terminal-styled card with name/coords/notes
- **Download panel:** Full overlay with bounding box controls, zoom selector, progress bar

### Data Flow

```
User taps "Download Region"
  -> MapsViewModel.startRegionDownload(bounds, zoomRange)
    -> OfflineTileManager.downloadRegion(bounds, zoomRange)
      -> Calculate tile list from bounds + zoom levels
      -> For each tile: HTTP GET tile URL -> store in MBTiles SQLite
      -> Emit DownloadProgress(count, total) per tile
    -> Flow collected in ViewModel, updates UiState.downloadProgress
  -> UI renders progress overlay

MapLibre needs a tile
  -> Custom TileDataProvider checks OfflineTileManager
    -> If tile exists in any MBTiles file: return decoded bitmap
    -> If no offline tile: return null (MapLibre skips or shows blank)
```

### Dependencies
- **MapLibre GL Android SDK 11.8.4** — already in `build.gradle.kts`
- **OkHttp** — already in `build.gradle.kts`, used for tile downloads
- **Google Play Services Location 21.3.0** — already in `build.gradle.kts`
- No new dependencies needed

### Error Handling
- **No offline tiles for area:** Show terminal-styled "NO MAP DATA" overlay with download prompt
- **Download interrupted:** Resume support via tracking completed tiles in MBTiles metadata
- **Storage full:** Check available space before download, warn if insufficient
- **No GPS fix:** Show "NO FIX" in coordinates overlay, gray out auto-center
- **WiFi not connected:** Warn that download uses mobile data, offer cancel

### Files to Create/Modify

| File | Action | Purpose |
|------|--------|---------|
| `ui/theme/TerminalEffects.kt` | Modify | Fix navbar: icon-only for unselected tabs |
| `ui/maps/MapsScreen.kt` | Rewrite | Full-screen MapLibre map with overlays |
| `ui/maps/MapsViewModel.kt` | Modify | Add offline tile state, download actions |
| `data/maps/OfflineTileManager.kt` | Create | MBTiles storage, tile download, region management |
| `data/maps/MBTilesWriter.kt` | Create | MBTiles SQLite format writer |
| `data/maps/MBTilesReader.kt` | Create | MBTiles reader for MapLibre tile provider |
| `data/maps/TileCalculator.kt` | Create | Convert bounds + zoom to tile coordinate lists |
| `di/MapsModule.kt` | Create | Hilt module for OfflineTileManager |
| `data/repository/MapsRepository.kt` | Modify | Add offline tile integration |

### Testing Approach
- Unit tests for `TileCalculator`: bounds-to-tile conversion accuracy
- Unit tests for `MBTilesWriter`/`MBTilesReader`: round-trip tile storage
- Unit tests for `OfflineTileManager`: download progress flow, region listing
- Integration test: download a tiny 2x2 tile region and verify rendering
