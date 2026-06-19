package com.nomad.android.ui.notes

import com.nomad.android.data.Result
import com.nomad.android.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveCurrentNote_writesBackGeneratedRowId_soResaveDoesNotDuplicate() {
        // Regression: a brand-new note starts with noteId == -1. On first save the
        // repository returns the generated row id (Result<Long>); that id MUST be
        // written back into uiState.noteId. Previously the id was discarded, so
        // every subsequent saveCurrentNote() passed existingId=null and inserted a
        // duplicate row instead of updating.
        val repo = mock<NoteRepository>()
        // New-note path: existingId is null (noteId == -1 on the ui state).
        whenever(runBlocking { repo.saveNote(any(), any(), isNull()) })
            .thenReturn(Result.success(42L))
        val vm = NoteEditorViewModel(repo)

        vm.updateTitle("Survival note")
        vm.updateContent("bandages, water")
        // Precondition: a never-saved note carries noteId == -1.
        assertEquals(-1L, vm.uiState.value.noteId)

        runTest(mainDispatcher) {
            vm.saveCurrentNote()
            advanceUntilIdle()
        }

        // The generated id must be captured so the next save updates row 42,
        // not inserts a new row.
        assertEquals(42L, vm.uiState.value.noteId)
    }
}
