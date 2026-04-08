# Notes Feature Design

## Overview

Add a local markdown note-taking feature to NOMAD, inspired by FlatNotes from Project N.O.M.A.D. Notes replaces the Settings tab in the bottom navigation. Settings becomes accessible from the Dashboard screen.

## Navigation Changes

- **Remove** Settings tab from `TerminalTabs` in `TerminalEffects.kt`
- **Add** Notes tab at the same position with `Icons.Filled.StickyNote2` / `Icons.Outlined.StickyNote2`
- **Add** `NOTES = "notes"` route to `Routes.kt`
- **Register** `composable(Routes.NOTES) { NotesScreen() }` in `NavHost.kt`
- **Dashboard** gets a gear icon in its header row that navigates to `Routes.SETTINGS`

## Data Layer

### Entity

```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

### DAO

```kotlin
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<NoteEntity>>
}
```

### Database Migration

Bump `NomadDatabase` version from 4 to 5. `MIGRATION_4_5`:

```sql
CREATE TABLE IF NOT EXISTS notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
)
```

Register entity + DAO in `NomadDatabase`, register migration in `DatabaseModule`.

### Repository

`NoteRepository` wraps `NoteDao` with the project's `Result<T>` sealed class. Key methods:

- `getAllNotes(): Flow<List<NoteEntity>>` — passthrough to DAO
- `getNote(id): Result<NoteEntity>`
- `saveNote(title, content, existingId?): Result<Long>` — insert or update
- `deleteNote(id): Result<Unit>`
- `searchNotes(query): Flow<List<NoteEntity>>`

### DI

- `DatabaseModule`: add `provideNoteDao(db) = db.noteDao()`
- `RepositoryModule`: add `provideNoteRepository(noteDao): NoteRepository`

## UI Layer

### State

```kotlin
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class NotesData(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false
)

data class NotesUiState(
    val data: NotesData = NotesData(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### ViewModel

`NotesViewModel` (`@HiltViewModel`):

- `loadNotes()` — collects from `noteRepository.getAllNotes()`
- `searchNotes(query)` — delegates to `noteRepository.searchNotes(query)`
- `deleteNote(id)` — calls repository, removes from state

### Screens

#### Notes List (`NotesScreen`)

- Top: "DATA LOG" header with search icon and "NEW ENTRY" button
- Search bar (collapsible, triggered by search icon)
- Scrollable list of notes using `TerminalListTile` — shows title, first line of content as subtitle, formatted date
- Swipe-to-delete or long-press delete with confirmation
- Tap note to navigate to editor
- FAB-style "NEW ENTRY" `TerminalButton` at bottom

#### Note Editor (`NoteEditorScreen`)

Route: `"notes/{noteId}"` with argument. `noteId = -1` for new note.

- Header: back arrow, "EDIT ENTRY" title, delete button (existing notes only), edit/preview toggle
- Title: `TerminalTextField` (single line)
- Body: multi-line `TerminalTextField` in edit mode, rendered markdown in preview mode
- Auto-save on back navigation via `BackHandler`
- Empty title defaults to "Untitled" on save

#### Markdown Rendering

Add `com.halilibo:compose-richtext:0.20.0` (or latest) as a dependency for markdown rendering. Style the output with `PhosphorGreen` text color and `BackgroundDark` background to match the Pip-Boy theme.

If the library adds too much weight, fall back to a simple regex-based renderer that handles headers (`#`), bold (`**`), italic (`*`), lists (`-`), and code blocks (triple backtick) using Compose `Text` composables styled with Pip-Boy theme colors.

## File Structure

```
data/local/entity/NoteEntity.kt
data/local/dao/NoteDao.kt
data/repository/NoteRepository.kt
ui/notes/NotesViewModel.kt
ui/notes/NotesScreen.kt
ui/notes/NoteEditorScreen.kt
```

Modified files:
- `data/local/NomadDatabase.kt` — add entity, DAO, bump version, add migration
- `di/DatabaseModule.kt` — provide NoteDao, register migration
- `di/RepositoryModule.kt` — provide NoteRepository
- `ui/navigation/Routes.kt` — add NOTES constant
- `ui/navigation/NavHost.kt` — register NotesScreen, NoteEditorScreen
- `ui/theme/TerminalEffects.kt` — replace Settings tab with Notes tab
- `ui/dashboard/DashboardScreen.kt` — add settings gear icon in header
- `app/build.gradle.kts` — add markdown rendering dependency

## Success Criteria

- Users can create, edit, delete, and search notes
- Notes persist across app restarts via Room
- Markdown preview renders headers, bold, italic, lists, and code blocks
- All UI follows Pip-Boy green-on-dark theme with existing components
- Existing Settings functionality is preserved and accessible from Dashboard
- CI passes (lint, unit tests, debug build)
