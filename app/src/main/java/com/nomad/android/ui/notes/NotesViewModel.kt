package com.nomad.android.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.local.entity.NoteEntity
import com.nomad.android.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState(isLoading = true))
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            noteRepository.getAllNotes().collect { result ->
                _uiState.update { state ->
                    when {
                        result.isSuccess -> state.copy(
                            data = state.data.copy(notes = result.getOrNull()?.map { it.toNote() } ?: emptyList()),
                            isLoading = false,
                            error = null
                        )
                        result.isError -> state.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to load notes"
                        )
                        else -> state
                    }
                }
            }
        }
    }

    fun searchNotes(query: String) {
        _uiState.update { it.copy(data = it.data.copy(searchQuery = query)) }
        if (query.isBlank()) {
            loadNotes()
            return
        }
        viewModelScope.launch {
            noteRepository.searchNotes(query).collect { result ->
                _uiState.update { state ->
                    state.copy(
                        data = state.data.copy(notes = result.getOrNull()?.map { it.toNote() } ?: emptyList()),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleSearch() {
        _uiState.update {
            it.copy(data = it.data.copy(
                isSearchActive = !it.data.isSearchActive,
                searchQuery = if (it.data.isSearchActive) "" else it.data.searchQuery
            ))
        }
        if (_uiState.value.data.searchQuery.isBlank()) loadNotes()
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            noteRepository.deleteNote(id)
        }
    }

    private fun NoteEntity.toNote() = Note(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
