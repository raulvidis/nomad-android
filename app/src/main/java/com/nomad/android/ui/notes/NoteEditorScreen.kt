package com.nomad.android.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalButton
import com.nomad.android.ui.components.TerminalButtonVariant
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.components.TerminalTextField
import com.nomad.android.ui.theme.BackgroundDark
import com.nomad.android.ui.theme.PhosphorGreen
import com.nomad.android.ui.theme.PhosphorGreenDim

private val SpaceGroteskBold = FontFamily(Font(R.font.space_grotesk_bold, FontWeight.Bold))
private val SpaceGroteskSemiBold = FontFamily(Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold))
private val SpaceGroteskRegular = FontFamily(Font(R.font.space_grotesk_regular, FontWeight.Normal))

@Composable
fun NoteEditorScreen(
    noteId: Long,
    navController: NavHostController,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    NoteEditorContent(
        uiState = uiState,
        onTitleChange = { viewModel.updateTitle(it) },
        onContentChange = { viewModel.updateContent(it) },
        onBack = {
            viewModel.saveCurrentNote()
            navController.popBackStack()
        },
        onDelete = {
            viewModel.deleteAndNavigateBack(noteId) {
                navController.popBackStack()
            }
        },
        onTogglePreview = { viewModel.togglePreview() },
    )
}

@Composable
private fun NoteEditorContent(
    uiState: NoteEditorUiState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onTogglePreview: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

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
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PhosphorGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
            TerminalText(
                text = if (uiState.isPreview) "PREVIEW" else "EDIT ENTRY",
                color = PhosphorGreen,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = SpaceGroteskSemiBold,
                ),
            )
            Row {
                if (uiState.noteId > 0) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = PhosphorGreenDim,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                IconButton(onClick = onTogglePreview) {
                    Icon(
                        imageVector = if (uiState.isPreview) Icons.Filled.Edit else Icons.Filled.Visibility,
                        contentDescription = if (uiState.isPreview) "Edit" else "Preview",
                        tint = PhosphorGreen,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isPreview) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                TerminalText(
                    text = uiState.title.ifBlank { "Untitled" },
                    color = PhosphorGreen,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontFamily = SpaceGroteskBold,
                    ),
                )
                Spacer(modifier = Modifier.height(16.dp))
                MarkdownText(content = uiState.content)
            }
        } else {
            TerminalTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                placeholder = "ENTRY TITLE...",
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
            TerminalTextField(
                value = uiState.content,
                onValueChange = onContentChange,
                placeholder = "Begin log entry...",
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "DELETE ENTRY",
                    fontFamily = SpaceGroteskSemiBold,
                    color = PhosphorGreen,
                )
            },
            text = {
                Text("Permanently delete this log entry?", color = PhosphorGreenDim)
            },
            confirmButton = {
                TerminalButton(
                    text = "DELETE",
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    variant = TerminalButtonVariant.DANGER,
                )
            },
            dismissButton = {
                TerminalButton(text = "CANCEL", onClick = { showDeleteDialog = false })
            },
            containerColor = BackgroundDark,
        )
    }
}

@Composable
private fun MarkdownText(content: String) {
    val greenStyle = SpanStyle(
        color = PhosphorGreen,
        fontFamily = SpaceGroteskRegular,
        fontSize = 14.sp,
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content.lines().forEach { line ->
            when {
                line.startsWith("### ") -> {
                    TerminalText(
                        text = line.removePrefix("### "),
                        color = PhosphorGreen,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontFamily = SpaceGroteskSemiBold,
                        ),
                    )
                }
                line.startsWith("## ") -> {
                    TerminalText(
                        text = line.removePrefix("## "),
                        color = PhosphorGreen,
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontFamily = SpaceGroteskSemiBold,
                        ),
                    )
                }
                line.startsWith("# ") -> {
                    TerminalText(
                        text = line.removePrefix("# "),
                        color = PhosphorGreen,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontFamily = SpaceGroteskBold,
                        ),
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val bulletContent = line.removePrefix("- ").removePrefix("* ")
                    Text(
                        text = buildAnnotatedString {
                            withStyle(greenStyle) { append("  • ") }
                            appendMarkdownInline(bulletContent, greenStyle)
                        },
                    )
                }
                line.startsWith("```") -> { }
                line.isBlank() -> Spacer(modifier = Modifier.height(4.dp))
                else -> {
                    Text(
                        text = buildAnnotatedString {
                            appendMarkdownInline(line, greenStyle)
                        },
                    )
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendMarkdownInline(text: String, baseStyle: SpanStyle) {
    // Single alternation regex: `\*\*` must precede `\*` so that `**bold**` is
    // matched as one bold span instead of also yielding an overlapping italic
    // range. The inner text is read straight from the captured group, so there
    // is no fragile delimiter-width arithmetic that dropped/mangled characters
    // (a single `*` italic marker was previously treated as width 2).
    val inlineRegex = Regex("""\*\*(.+?)\*\*|\*(.+?)\*|`(.+?)`""")

    var currentIndex = 0

    inlineRegex.findAll(text).forEach { match ->
        val range = match.range
        val (innerText, style) = when {
            match.groupValues[1].isNotEmpty() ->
                match.groupValues[1] to SpanStyle(fontWeight = FontWeight.Bold)
            match.groupValues[2].isNotEmpty() ->
                match.groupValues[2] to SpanStyle(fontStyle = FontStyle.Italic)
            else ->
                match.groupValues[3] to SpanStyle(fontFamily = FontFamily.Monospace)
        }
        if (range.first > currentIndex) {
            withStyle(baseStyle) { append(text.substring(currentIndex, range.first)) }
        }
        withStyle(baseStyle.merge(style)) { append(innerText) }
        currentIndex = range.last + 1
    }

    if (currentIndex < text.length) {
        withStyle(baseStyle) { append(text.substring(currentIndex)) }
    }
}
