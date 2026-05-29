package com.nomad.android.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteEditorUiState(
    val noteId: Long = -1,
    val title: String = "",
    val content: String = "",
    val isPreview: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    fun loadNote(noteId: Long) {
        if (noteId <= 0) {
            _uiState.update { it.copy(noteId = -1) }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = noteRepository.getNote(noteId)
            if (result.isSuccess) {
                val note = result.getOrNull() ?: return@launch
                _uiState.update {
                    it.copy(
                        noteId = note.id,
                        title = note.title,
                        content = note.content,
                        isLoading = false,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    fun togglePreview() {
        _uiState.update { it.copy(isPreview = !it.isPreview) }
    }

    fun saveCurrentNote() {
        val state = _uiState.value
        val title = state.title.ifBlank { "Untitled" }
        viewModelScope.launch {
            noteRepository.saveNote(title, state.content, state.noteId.let { if (it > 0) it else null })
        }
    }

    fun deleteAndNavigateBack(noteId: Long, onNavigateBack: () -> Unit) {
        viewModelScope.launch {
            if (noteId > 0) {
                noteRepository.deleteNote(noteId)
            }
            onNavigateBack()
        }
    }
}
