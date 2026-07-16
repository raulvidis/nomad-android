package com.nomad.android.ui.chat

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalButton
import com.nomad.android.ui.components.TerminalButtonSize
import com.nomad.android.ui.components.TerminalButtonVariant
import com.nomad.android.ui.components.TerminalDivider
import com.nomad.android.ui.components.TerminalErrorScreen
import com.nomad.android.ui.components.TerminalLoadingScreen
import com.nomad.android.ui.components.TerminalTextField
import com.nomad.android.ui.theme.TertiaryAmber
import com.nomad.android.ui.theme.BackgroundDark
import com.nomad.android.ui.theme.PhosphorGreen
import com.nomad.android.ui.theme.PhosphorGreenDim
import com.nomad.android.ui.theme.SurfaceContainerLow
import com.nomad.android.ui.theme.TerminalDanger
import java.io.File

internal val SpaceGroteskBold = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_bold, FontWeight.Bold),
)
internal val SpaceGroteskSemiBold = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_semi_bold, FontWeight.Medium),
)
internal val SpaceGroteskRegular = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
)

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
            onSendMessage = { text, image -> viewModel.sendMessage(text, image) },
            onNewSession = { viewModel.newSession() },
            onCompact = { viewModel.compactContext() },
            onSetPendingImage = { viewModel.setPendingImage(it) },
            onRemoveFromQueue = { viewModel.removeFromQueue(it) },
            onToggleThinking = { viewModel.toggleThinking(it) },
            onToggleToolResult = { viewModel.toggleToolResult(it) },
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
                fontFamily = SpaceGroteskBold,
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
                    fontFamily = SpaceGroteskRegular,
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
                            fontFamily = SpaceGroteskRegular,
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

@Composable
private fun ChatContent(
    data: ChatData,
    onSendMessage: (String, String?) -> Unit,
    onNewSession: () -> Unit,
    onCompact: () -> Unit,
    onSetPendingImage: (String?) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onToggleThinking: (Long) -> Unit,
    onToggleToolResult: (Long) -> Unit,
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var autoFollowBottom by remember { mutableStateOf(true) }
    var showAttachmentOptions by remember { mutableStateOf(false) }

    // Camera: create a temp file and get its URI
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImagePath by remember { mutableStateOf<String?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val file = File(context.filesDir, "chat_images").apply { mkdirs() }
                val dest = File(file, "${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Cannot open input stream for selected image")
                if (dest.exists() && dest.length() > 0) {
                    onSetPendingImage(dest.absolutePath)
                    imageError = null
                }
            } catch (e: Exception) {
                Log.e("ChatScreen", "Failed to copy selected image", e)
                imageError = "Failed to attach image"
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImagePath != null && File(cameraImagePath!!).exists()) {
            onSetPendingImage(cameraImagePath)
        } else {
            cameraImagePath?.let { File(it).delete() }
            cameraImagePath = null
        }
    }

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
                fontFamily = SpaceGroteskBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${data.contextTokenCount}tk",
                color = PhosphorGreenDim,
                fontFamily = SpaceGroteskRegular,
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
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 13.sp,
                )
            }
        } else {
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()

            // Stick-to-bottom: true while the last item's end is visible. Translated
            // from opendroid's RecyclerView pin logic (ChatFragment.pinStreamingBottom).
            val atBottom by remember {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()
                    last == null || (last.index == info.totalItemsCount - 1 &&
                        last.offset + last.size <= info.viewportEndOffset + 8)
                }
            }
            // Disengage following only on a user drag (programmatic scrolls don't emit
            // drag interactions); re-engage when the user settles back at the bottom.
            LaunchedEffect(listState) {
                listState.interactionSource.interactions.collect { interaction ->
                    if (interaction is DragInteraction.Start) autoFollowBottom = false
                }
            }
            LaunchedEffect(listState.isScrollInProgress) {
                if (!listState.isScrollInProgress && atBottom) autoFollowBottom = true
            }
            // While following, keep the latest content pinned as tokens stream in.
            // Keyed on both answer and thinking length so an expanded, still-streaming
            // thinking block is followed too. Waits a frame so the new text is measured,
            // then scrolls by the exact overflow — small per-token deltas read as a
            // smooth continuous follow without restarting an animation per token.
            val last = data.messages.lastOrNull()
            val lastContentLen = (last?.content?.length ?: 0) +
                (if (last?.isThinkingExpanded == true) last.thinkingText.length else 0)
            LaunchedEffect(data.messages.size, lastContentLen, last?.isThinkingExpanded, autoFollowBottom) {
                if (!autoFollowBottom || data.messages.isEmpty()) return@LaunchedEffect
                val lastIndex = data.messages.size - 1
                if (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index != lastIndex) {
                    listState.scrollToItem(lastIndex)
                }
                withFrameNanos { }
                val info = listState.layoutInfo
                val visibleLast = info.visibleItemsInfo.lastOrNull() ?: return@LaunchedEffect
                val delta = (visibleLast.offset + visibleLast.size) - info.viewportEndOffset
                if (delta > 0) listState.scrollBy(delta.toFloat())
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(data.messages, key = { it.id }) { message ->
                        when {
                            message.kind == MessageKind.ToolCall && message.toolCall != null ->
                                ToolCallCard(
                                    tool = message.toolCall,
                                    onToggle = { onToggleToolResult(message.id) },
                                )
                            message.role == "user" -> {
                                if (message.content.isNotEmpty() || message.imageUri != null) {
                                    MessageBubble(isUser = true, text = message.content, imageUri = message.imageUri)
                                }
                            }
                            else -> AssistantMessage(
                                message = message,
                                onToggleThinking = { onToggleThinking(message.id) },
                            )
                        }
                    }
                }

                if (!autoFollowBottom) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                    ) {
                        ScrollToBottomButton(
                            onClick = {
                                autoFollowBottom = true
                                scope.launch { listState.scrollToItem(data.messages.size - 1) }
                            },
                        )
                    }
                }
            }
        }

        // Bottom controls
        Column {
            // Message queue display
            if (data.messageQueue.isNotEmpty()) {
                TerminalDivider(color = PhosphorGreenDim)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(data.messageQueue) { index, queued ->
                        Row(
                            modifier = Modifier
                                .background(SurfaceContainerLow)
                                .border(1.dp, TertiaryAmber.copy(alpha = 0.5f))
                                .clickable { onRemoveFromQueue(index) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (queued.imagePath != null) {
                                AsyncImage(
                                    model = File(queued.imagePath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(0.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            Text(
                                text = queued.content.take(30) + if (queued.content.length > 30) "..." else "",
                                color = TertiaryAmber,
                                fontFamily = SpaceGroteskRegular,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "X",
                                color = TerminalDanger,
                                    fontFamily = SpaceGroteskBold,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            }

            TerminalDivider(color = PhosphorGreen)
            Spacer(modifier = Modifier.height(6.dp))

            if (data.messages.size > 6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TerminalButton(
                        text = "Compact",
                        onClick = onCompact,
                        size = TerminalButtonSize.SMALL,
                        tintColor = PhosphorGreenDim,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Pending image preview
            if (data.pendingImagePath != null) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box {
                        AsyncImage(
                            model = File(data.pendingImagePath),
                            contentDescription = "Attached image",
                            modifier = Modifier
                                .size(56.dp)
                                .border(1.dp, PhosphorGreen)
                                .clip(RoundedCornerShape(0.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        // X button overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .background(BackgroundDark.copy(alpha = 0.8f))
                                .border(1.dp, TerminalDanger)
                                .clickable { onSetPendingImage(null) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "X",
                                color = TerminalDanger,
                                fontSize = 9.sp,
                                    fontFamily = SpaceGroteskBold,
                            )
                        }
                    }
                    Text(
                        text = "IMAGE ATTACHED",
                        color = PhosphorGreenDim,
                                    fontFamily = SpaceGroteskRegular,
                        fontSize = 10.sp,
                    )
                }
            }

            if (imageError != null) {
                Text(
                    text = imageError!!,
                    color = TerminalDanger,
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }

            // Attachment options popup
            if (showAttachmentOptions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TerminalButton(
                        text = "GALLERY",
                        onClick = {
                            showAttachmentOptions = false
                            galleryLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        size = TerminalButtonSize.SMALL,
                    )
                    TerminalButton(
                        text = "CAMERA",
                        onClick = {
                            showAttachmentOptions = false
                            try {
                                val imageDir = File(context.filesDir, "chat_images").apply { mkdirs() }
                                val imageFile = File(imageDir, "${System.currentTimeMillis()}.jpg")
                                cameraImagePath = imageFile.absolutePath
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    imageFile
                                )
                                cameraImageUri = uri
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                Log.e("ChatScreen", "Failed to prepare camera image", e)
                                cameraImagePath = null
                                cameraImageUri = null
                            }
                        },
                        size = TerminalButtonSize.SMALL,
                    )
                    TerminalButton(
                        text = "CANCEL",
                        onClick = { showAttachmentOptions = false },
                        size = TerminalButtonSize.SMALL,
                        variant = TerminalButtonVariant.DANGER,
                    )
                }
            }

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Attachment button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
                        .background(SurfaceContainerLow, RoundedCornerShape(0.dp))
                        .clickable { showAttachmentOptions = !showAttachmentOptions },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+",
                        color = PhosphorGreen,
                        fontFamily = SpaceGroteskBold,
                        fontSize = 16.sp,
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                TerminalTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = if (data.isStreaming) "Queue a message..." else "Enter query...",
                    singleLine = true,
                )

                Spacer(modifier = Modifier.width(8.dp))

                TerminalButton(
                    text = if (data.isStreaming) "Queue" else "Send",
                    onClick = {
                        if (inputText.isNotBlank() || data.pendingImagePath != null) {
                            autoFollowBottom = true
                            onSendMessage(inputText, null)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() || data.pendingImagePath != null,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AssistantMessage(message: ChatMessage, onToggleThinking: () -> Unit) {
    val reasoning = message.isStreaming && message.content.isEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (message.thinkingText.isNotBlank() || reasoning) {
            ThinkingSection(
                thinking = message.thinkingText,
                expanded = message.isThinkingExpanded,
                streaming = reasoning,
                onToggle = onToggleThinking,
            )
        }
        when {
            message.content.isNotEmpty() ->
                MessageBubble(isUser = false, text = message.content, imageUri = message.imageUri)
            // Still streaming with no thinking shown yet → generic processing indicator.
            message.isStreaming && message.thinkingText.isBlank() ->
                StreamingMessageBubble(isStreaming = true)
        }
    }
}

@Composable
private fun MessageBubble(isUser: Boolean, text: String, imageUri: String? = null) {
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
            fontFamily = SpaceGroteskSemiBold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
        if (imageUri != null) {
            Spacer(modifier = Modifier.height(6.dp))
            AsyncImage(
                model = File(imageUri),
                contentDescription = "Attached image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(1.dp, borderColor.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(0.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        if (text.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = if (isUser) PhosphorGreen else TertiaryAmber,
                fontFamily = SpaceGroteskRegular,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
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


