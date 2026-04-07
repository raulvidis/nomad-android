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
import com.nomad.android.ui.components.TerminalButtonSize
import com.nomad.android.ui.components.TerminalButtonVariant
import com.nomad.android.ui.components.TerminalDivider
import com.nomad.android.ui.components.TerminalEmptyScreen
import com.nomad.android.ui.components.TerminalErrorScreen
import com.nomad.android.ui.components.TerminalLoadingScreen
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.components.TerminalTextField
import com.nomad.android.ui.theme.TertiaryAmber
import com.nomad.android.ui.theme.BackgroundDark
import com.nomad.android.ui.theme.PhosphorGreen
import com.nomad.android.ui.theme.PhosphorGreenDim
import com.nomad.android.ui.theme.SurfaceContainerLow

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
            SessionListScreen(
                sessions = uiState.data.sessions,
                onNewSession = { viewModel.newSession() },
                onLoadSession = { viewModel.loadSession(it) },
                onDeleteSession = { viewModel.deleteSession(it) },
                onDeleteAllSessions = { viewModel.deleteAllSessions() },
            )
        }
        else -> ChatContent(
            data = uiState.data,
            onSendMessage = { viewModel.sendMessage(it) },
            onNewSession = { viewModel.newSession() },
            onSelectFilter = { viewModel.selectFilter(it) },
            onSetThinkingPower = { viewModel.setThinkingPower(it) },
            onCompact = { viewModel.compactContext() },
        )
    }
}

@Composable
private fun SessionListScreen(
    sessions: List<ChatSession>,
    onNewSession: () -> Unit,
    onLoadSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDeleteAllSessions: () -> Unit,
) {
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
            Text(
                text = "CHAT SESSIONS",
                color = PhosphorGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.space_grotesk_bold, FontWeight.Bold),
                ),
                fontSize = 18.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TerminalButton(
                    text = "+ NEW",
                    onClick = onNewSession,
                    size = TerminalButtonSize.SMALL,
                )
                if (sessions.isNotEmpty()) {
                    TerminalButton(
                        text = "DEL ALL",
                        onClick = onDeleteAllSessions,
                        size = TerminalButtonSize.SMALL,
                        variant = TerminalButtonVariant.DANGER,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        TerminalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No sessions yet",
                    color = PhosphorGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                    ),
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(sessions, key = { it.id }) { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceContainerLow, RoundedCornerShape(0.dp))
                            .border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
                            .clickable { onLoadSession(session.id) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = session.title,
                            color = PhosphorGreen,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                            ),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TerminalButton(
                                text = "LOAD",
                                onClick = { onLoadSession(session.id) },
                                size = TerminalButtonSize.SMALL,
                            )
                            TerminalButton(
                                text = "DEL",
                                onClick = { onDeleteSession(session.id) },
                                size = TerminalButtonSize.SMALL,
                                variant = TerminalButtonVariant.DANGER,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.US)
    return sdf.format(java.util.Date(millis))
}

@Composable
private fun ChatContent(
    data: ChatData,
    onSendMessage: (String) -> Unit,
    onNewSession: () -> Unit,
    onSelectFilter: (String) -> Unit,
    onSetThinkingPower: (ThinkingPower) -> Unit,
    onCompact: () -> Unit,
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
    ) {
        // Header with session title + context counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = data.sessions.find { it.id == data.currentSessionId }?.title ?: "New Session",
                color = PhosphorGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.space_grotesk_bold, FontWeight.Bold),
                ),
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${data.contextTokenCount}tk",
                color = PhosphorGreenDim,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                ),
                fontSize = 10.sp,
            )
        }

        TerminalDivider(modifier = Modifier.padding(bottom = 4.dp))

        if (data.messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Type a message to begin",
                    color = PhosphorGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                    ),
                    fontSize = 13.sp,
                )
            }
        } else {
            val listState = rememberLazyListState()

            LaunchedEffect(data.messages.lastOrNull()?.content) {
                if (data.messages.isNotEmpty()) {
                    listState.animateScrollToItem(data.messages.size - 1)
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
                    if (message.content.isNotEmpty()) {
                        MessageBubble(
                            isUser = message.role == "user",
                            text = message.content,
                        )
                    }
                }
                if (data.isStreaming) {
                    item(key = "typing") { TypingIndicator() }
                }
            }
        }

        // Bottom controls
        Column {
            TerminalDivider(color = PhosphorGreen)
            Spacer(modifier = Modifier.height(6.dp))

            // Thinking power + compact row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThinkingPower.entries.forEach { power ->
                        val isSelected = data.thinkingPower == power
                        Box(
                            modifier = Modifier
                                .border(
                                    width = if (isSelected) 2.dp else 2.dp,
                                    color = if (isSelected) TertiaryAmber else PhosphorGreenDim,
                                    shape = RoundedCornerShape(0.dp),
                                )
                                .background(
                                    if (isSelected) TertiaryAmber.copy(alpha = 0.1f) else SurfaceContainerLow,
                                    RoundedCornerShape(0.dp),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSetThinkingPower(power) },
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = power.label,
                                color = if (isSelected) TertiaryAmber else PhosphorGreenDim,
                                fontFamily = androidx.compose.ui.text.font.FontFamily(
                                    androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                                ),
                                fontSize = 10.sp,
                            )
                        }
                    }
                }

                if (data.messages.size > 6) {
                    Box(
                        modifier = Modifier
                            .border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
                            .background(SurfaceContainerLow, RoundedCornerShape(0.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCompact,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Compact",
                            color = PhosphorGreenDim,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                            ),
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Knowledge filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                data.contextFilters.forEach { filter ->
                    val isSelected = data.selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .border(
                                width = if (isSelected) 2.dp else 2.dp,
                                color = if (isSelected) PhosphorGreen else PhosphorGreenDim,
                                shape = RoundedCornerShape(0.dp),
                            )
                            .background(
                                if (isSelected) PhosphorGreen.copy(alpha = 0.1f) else SurfaceContainerLow,
                                RoundedCornerShape(0.dp),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelectFilter(filter) },
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) PhosphorGreen else PhosphorGreenDim,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
                            ),
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Input row
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
    val borderColor = if (isUser) PhosphorGreen else TertiaryAmber
    val prefix = if (isUser) "QUERY" else "RESPONSE"
    val prefixColor = if (isUser) PhosphorGreen else TertiaryAmber

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow, RoundedCornerShape(0.dp))
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
                androidx.compose.ui.text.font.Font(R.font.space_grotesk_semi_bold, FontWeight.Medium),
            ),
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = if (isUser) PhosphorGreen else TertiaryAmber,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
            ),
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun StreamingMessageBubble(isStreaming: Boolean) {
    var dotCount by remember { mutableStateOf(1) }
    LaunchedEffect(isStreaming) {
        while (isStreaming) {
            kotlinx.coroutines.delay(400)
            dotCount = (dotCount % 3) + 1
        }
    }
    MessageBubble(
        isUser = false,
        text = "Processing${".".repeat(dotCount)}",
    )
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

    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Processing${".".repeat(dotCount)}",
            color = TertiaryAmber,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.space_grotesk_semi_bold, FontWeight.Medium),
            ),
            fontSize = 12.sp,
        )
    }
}
