package com.nomad.android.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.ui.theme.PipBoyAmber
import com.nomad.android.ui.theme.PipBoyBg
import com.nomad.android.ui.components.PipBoyButton
import com.nomad.android.ui.components.PipBoyCard
import com.nomad.android.ui.components.PipBoyDivider
import com.nomad.android.ui.components.PipBoyEmptyScreen
import com.nomad.android.ui.components.PipBoyErrorScreen
import com.nomad.android.ui.theme.PipBoyGreen
import com.nomad.android.ui.theme.PipBoyGreenDim
import com.nomad.android.ui.components.PipBoyLoadingScreen
import com.nomad.android.ui.theme.PipBoySurface
import com.nomad.android.ui.components.PipBoyText
import com.nomad.android.ui.components.PipBoyTextField
import androidx.compose.material3.Text

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> PipBoyLoadingScreen("ESTABLISHING NEURAL LINK...")
        uiState.error != null -> PipBoyErrorScreen(uiState.error) { viewModel.loadRecentSessions() }
        uiState.data.messages.isEmpty() && uiState.data.currentSessionId == null -> {
            PipBoyEmptyScreen(
                message = "No active session. Start a new conversation.",
                action = "NEW SESSION",
                onAction = { viewModel.newSession() }
            )
        }
        else -> ChatContent(
            data = uiState.data,
            onSendMessage = { viewModel.sendMessage(it) },
            onNewSession = { viewModel.newSession() },
            onSelectFilter = { viewModel.selectFilter(it) }
        )
    }
}

@Composable
private fun ChatContent(
    data: ChatData,
    onSendMessage: (String) -> Unit,
    onNewSession: () -> Unit,
    onSelectFilter: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBoyBg),
    ) {
        TerminalHeader(modelName = data.sessions.firstOrNull()?.title ?: "AI TERMINAL")

        PipBoyDivider(modifier = Modifier.padding(bottom = 4.dp))

        if (data.messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO MESSAGES — START A NEW SESSION",
                    color = PipBoyGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(data.messages, key = { it.id }) { message ->
                    MessageBubble(
                        isUser = message.role == "user",
                        text = message.content
                    )
                }
                if (data.isStreaming) {
                    item { TypingIndicator() }
                }
            }
        }

        Column {
            PipBoyDivider(color = PipBoyGreen)

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
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PipBoyTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "Enter query...",
                    singleLine = true,
                )

                Spacer(modifier = Modifier.width(8.dp))

                PipBoyButton(
                    text = "SEND",
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
private fun TerminalHeader(modelName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        PipBoyText(
            text = "ROBCO INDUSTRIES (TM) TERMLINK PROTOCOL",
            color = PipBoyGreenDim,
            glow = false,
            style = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
        )
        PipBoyText(
            text = modelName.uppercase(),
            color = PipBoyGreen,
            glow = true,
            style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
private fun MessageBubble(isUser: Boolean, text: String) {
    val borderColor = if (isUser) PipBoyGreen else PipBoyAmber
    val prefix = if (isUser) "> QUERY:" else "RESPONSE:"
    val prefixColor = if (isUser) PipBoyGreen else PipBoyAmber

    PipBoyCard(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 3.dp.toPx(),
                )
            },
    ) {
        PipBoyText(
            text = prefix,
            color = prefixColor,
            glow = true,
            style = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
        )
        Spacer(modifier = Modifier.height(4.dp))
        PipBoyText(
            text = text,
            color = if (isUser) PipBoyGreen else PipBoyAmber,
            glow = false,
            style = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotCount by infiniteTransition.animateIntAsState(
        initialValue = 1,
        targetValue = 4,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dotCount",
    )

    val dots = ".".repeat(((dotCount - 1) % 3) + 1)

    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PipBoyText(
            text = "PROCESSING$dots",
            color = PipBoyAmber,
            glow = true,
            style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
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
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filters.forEach { label ->
            val isSelected = label == selectedFilter
            val borderColor = if (isSelected) PipBoyGreen else PipBoyGreenDim
            val bgColor = if (isSelected) PipBoyGreen.copy(alpha = 0.15f) else PipBoySurface
            val textColor = if (isSelected) PipBoyGreen else PipBoyGreenDim

            Box(
                modifier = Modifier
                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    .background(bgColor, RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onFilterSelected(label) },
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
