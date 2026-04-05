package com.nomad.android.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalButton
import com.nomad.android.ui.components.TerminalDivider
import com.nomad.android.ui.components.TerminalEmptyScreen
import com.nomad.android.ui.components.TerminalErrorScreen
import com.nomad.android.ui.components.TerminalLoadingScreen
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.components.TerminalTextField
import com.nomad.android.ui.theme.TerminalAmber
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim
import com.nomad.android.ui.theme.TerminalSurface

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> TerminalLoadingScreen("ESTABLISHING NEURAL LINK...")
        uiState.error != null -> TerminalErrorScreen(
            message = uiState.error ?: "Unknown error",
            onRetry = { viewModel.loadRecentSessions() },
        )
        uiState.data.messages.isEmpty() && uiState.data.currentSessionId == null -> {
            TerminalEmptyScreen(
                message = "No active session. Start a new conversation.",
                action = "New Session",
                onAction = { viewModel.newSession() },
            )
        }
        else -> ChatContent(
            data = uiState.data,
            onSendMessage = { viewModel.sendMessage(it) },
            onNewSession = { viewModel.newSession() },
            onSelectFilter = { viewModel.selectFilter(it) },
        )
    }
}

@Composable
private fun ChatContent(
    data: ChatData,
    onSendMessage: (String) -> Unit,
    onNewSession: () -> Unit,
    onSelectFilter: (String) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            TerminalText(
                text = data.sessions.firstOrNull()?.title ?: "AI Terminal",
                color = TerminalGreen,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 18.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                    ),
                ),
            )
        }

        TerminalDivider(modifier = Modifier.padding(bottom = 4.dp))

        if (data.messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No messages — start a new session",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 13.sp,
                )
            }
        } else {
            val listState = rememberLazyListState()

            LaunchedEffect(data.messages.size, data.isStreaming) {
                if (data.messages.isNotEmpty()) {
                    val targetIndex = data.messages.size - 1 + if (data.isStreaming) 1 else 0
                    listState.animateScrollToItem(targetIndex)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(data.messages, key = { "${it.sessionId}_${it.timestamp}_${it.role}" }) { message ->
                    MessageBubble(
                        isUser = message.role == "user",
                        text = message.content,
                    )
                }
                if (data.isStreaming) {
                    item { TypingIndicator() }
                }
            }
        }

        Column {
            TerminalDivider(color = TerminalGreen)

            Spacer(modifier = Modifier.height(8.dp))

            ContextFilterRow(
                filters = data.contextFilters,
                selectedFilter = data.selectedFilter,
                onFilterSelected = onSelectFilter,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TerminalTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "Enter query...",
                    singleLine = true,
                )

                Spacer(modifier = Modifier.width(8.dp))

                TerminalButton(
                    text = "Send",
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !data.isStreaming,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MessageBubble(isUser: Boolean, text: String) {
    val borderColor = if (isUser) TerminalGreen else TerminalAmber
    val prefix = if (isUser) "QUERY" else "RESPONSE"
    val prefixColor = if (isUser) TerminalGreen else TerminalAmber

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalSurface, RoundedCornerShape(6.dp))
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 3.dp.toPx(),
                )
            }
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Text(
            text = prefix,
            color = prefixColor,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
            ),
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = if (isUser) TerminalGreen else TerminalAmber,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun TypingIndicator() {
    var dotCount by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(400)
            dotCount = (dotCount % 3) + 1
        }
    }

    val dots = ".".repeat(dotCount)

    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Processing$dots",
            color = TerminalAmber,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
            ),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ContextFilterRow(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filters.forEach { label ->
            val isSelected = label == selectedFilter
            val borderColor = if (isSelected) TerminalGreen else TerminalGreenDim
            val bgColor = if (isSelected) TerminalGreen.copy(alpha = 0.15f) else TerminalSurface
            val textColor = if (isSelected) TerminalGreen else TerminalGreenDim

            Box(
                modifier = Modifier
                    .border(2.dp, borderColor, RoundedCornerShape(6.dp))
                    .background(bgColor, RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onFilterSelected(label) },
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 11.sp,
                )
            }
        }
    }
}
