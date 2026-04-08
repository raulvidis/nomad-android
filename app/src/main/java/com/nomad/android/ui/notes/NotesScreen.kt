package com.nomad.android.ui.notes

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalButton
import com.nomad.android.ui.components.TerminalEmptyScreen
import com.nomad.android.ui.components.TerminalErrorScreen
import com.nomad.android.ui.components.TerminalListTile
import com.nomad.android.ui.components.TerminalLoadingScreen
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.components.TerminalTextField
import com.nomad.android.ui.navigation.Routes
import com.nomad.android.ui.theme.BackgroundDark
import com.nomad.android.ui.theme.PhosphorGreen
import com.nomad.android.ui.theme.PhosphorGreenDim
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    navController: NavHostController,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> TerminalLoadingScreen("ACCESSING DATA LOGS...")
        uiState.error != null -> TerminalErrorScreen(
            message = uiState.error ?: "Unknown error",
            onRetry = { viewModel.loadNotes() },
        )
        else -> NotesContent(
            data = uiState.data,
            onSearch = { viewModel.searchNotes(it) },
            onToggleSearch = { viewModel.toggleSearch() },
            onNoteClick = { noteId ->
                navController.navigate("notes/$noteId") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
            },
            onNewNote = {
                navController.navigate("notes/-1") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
            },
            onDeleteNote = { viewModel.deleteNote(it) },
        )
    }
}

@Composable
private fun NotesContent(
    data: NotesData,
    onSearch: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onNewNote: () -> Unit,
    onDeleteNote: (Long) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TerminalText(
                text = "DATA LOG",
                color = PhosphorGreen,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.space_grotesk_bold, FontWeight.Bold),
                    ),
                ),
            )
            Row {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = if (data.isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = PhosphorGreen,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        if (data.isSearchActive) {
            TerminalTextField(
                value = data.searchQuery,
                onValueChange = onSearch,
                placeholder = "SEARCH LOGS...",
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(bottom = 12.dp),
            )
        }

        if (data.notes.isEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            TerminalEmptyScreen(
                message = if (data.searchQuery.isNotBlank()) "NO MATCHING ENTRIES" else "NO DATA LOGS RECORDED",
                action = if (data.searchQuery.isBlank()) "NEW ENTRY" else null,
                onAction = if (data.searchQuery.isBlank()) onNewNote else null,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(data.notes, key = { it.id }) { index, note ->
                    val preview = note.content.lines().firstOrNull { it.isNotBlank() } ?: ""
                    TerminalListTile(
                        title = note.title.ifBlank { "Untitled" },
                        subtitle = "${dateFormat.format(Date(note.updatedAt))} — ${preview.take(60)}",
                        onClick = { onNoteClick(note.id) },
                        onLongClick = { showDeleteDialog = note.id },
                        index = index,
                    )
                }
            }
        }

        TerminalButton(
            text = "+ NEW ENTRY",
            onClick = onNewNote,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }

    showDeleteDialog?.let { noteId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = {
                Text(
                    "DELETE ENTRY",
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold),
                    ),
                    color = PhosphorGreen,
                )
            },
            text = {
                Text(
                    "Permanently delete this log entry?",
                    color = PhosphorGreenDim,
                )
            },
            confirmButton = {
                TerminalButton(
                    text = "DELETE",
                    onClick = {
                        onDeleteNote(noteId)
                        showDeleteDialog = null
                    },
                    variant = com.nomad.android.ui.components.TerminalButtonVariant.DANGER,
                )
            },
            dismissButton = {
                TerminalButton(text = "CANCEL", onClick = { showDeleteDialog = null })
            },
            containerColor = BackgroundDark,
        )
    }
}
