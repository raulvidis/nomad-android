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
import androidx.compose.runtime.mutableIntStateOf
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
import com.nomad.android.ui.components.PipBoyAmber
import com.nomad.android.ui.components.PipBoyBg
import com.nomad.android.ui.components.PipBoyButton
import com.nomad.android.ui.components.PipBoyCard
import com.nomad.android.ui.components.PipBoyDivider
import com.nomad.android.ui.components.PipBoyGreen
import com.nomad.android.ui.components.PipBoyGreenDim
import com.nomad.android.ui.components.PipBoySurface
import com.nomad.android.ui.components.PipBoyText
import com.nomad.android.ui.components.PipBoyTextField
import androidx.compose.material3.Text

private data class ChatMessage(
    val isUser: Boolean,
    val text: String,
)

private val mockMessages = listOf(
    ChatMessage(
        isUser = true,
        text = "How do I purify water in the wild?",
    ),
    ChatMessage(
        isUser = false,
        text = "Water purification in wilderness conditions requires a multi-layered approach. " +
            "PRIMARY METHODS: (1) Boiling — bring water to a rolling boil for at least 1 minute " +
            "(3 minutes at altitudes above 6,500ft). This eliminates bacteria, viruses, and " +
            "parasites. (2) Chemical treatment — use iodine tablets (2 per quart, wait 30 min) " +
            "or chlorine dioxide drops. Effective against most pathogens but does not remove " +
            "sediment. (3) Filtration — portable filters with 0.2 micron pores remove bacteria " +
            "and protozoa. Combine with chemical treatment for virus protection. " +
            "SECONDARY METHODS: UV light pens (e.g., SteriPEN), solar disinfection (SODIS — " +
            "6 hours in clear plastic bottle under direct sun). Always pre-filter turbid water " +
            "through cloth or sediment settling before treatment. Cache purified water in clean, " +
            "sealed containers. Dehydration kills faster than most wasteland hazards.",
    ),
    ChatMessage(
        isUser = true,
        text = "What about boiling?",
    ),
)

private val contextFilters = listOf("WIKIPEDIA", "SURVIVAL", "FIRST AID", "ALL")

@Composable
fun ChatScreen() {
    var inputText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(contextFilters.lastIndex) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBoyBg),
    ) {
        TerminalHeader()

        PipBoyDivider(modifier = Modifier.padding(bottom = 4.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(mockMessages) { message ->
                MessageBubble(message)
            }
            item {
                TypingIndicator()
            }
        }

        Column {
            PipBoyDivider(color = PipBoyGreen)

            Spacer(modifier = Modifier.height(8.dp))

            ContextFilterRow(
                filters = contextFilters,
                selectedIndex = selectedFilter,
                onFilterSelected = { selectedFilter = it },
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
                    onClick = { },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TerminalHeader() {
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
            text = "AI TERMINAL v4.0 — GEMMA 4 E2B",
            color = PipBoyGreen,
            glow = true,
            style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val borderColor = if (message.isUser) PipBoyGreen else PipBoyAmber
    val prefix = if (message.isUser) "> QUERY:" else "RESPONSE:"
    val prefixColor = if (message.isUser) PipBoyGreen else PipBoyAmber

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
            text = message.text,
            color = if (message.isUser) PipBoyGreen else PipBoyAmber,
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
    selectedIndex: Int,
    onFilterSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filters.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
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
                        onClick = { onFilterSelected(index) },
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
