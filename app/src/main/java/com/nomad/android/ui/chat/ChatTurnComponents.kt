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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomad.android.ui.theme.BackgroundDark
import com.nomad.android.ui.theme.PhosphorGreen
import com.nomad.android.ui.theme.PhosphorGreenDim
import com.nomad.android.ui.theme.SurfaceContainerLow
import com.nomad.android.ui.theme.TerminalDanger
import com.nomad.android.ui.theme.TertiaryAmber

/**
 * Collapsible reasoning section ("thinking"). While the model is still reasoning
 * (no answer yet) it shows a live "THINKING…" header; once the answer starts it
 * becomes a tappable "THOUGHT FOR A MOMENT" toggle. Pip-Boy styled: dim phosphor,
 * monospace-ish, square corners.
 */
@Composable
fun ThinkingSection(
    thinking: String,
    expanded: Boolean,
    streaming: Boolean,
    onToggle: () -> Unit,
) {
    val hasThinking = thinking.isNotBlank()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = hasThinking,
                    onClick = onToggle,
                )
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    streaming && expanded -> "▼ THINKING…"
                    streaming -> "▶ THINKING…"
                    expanded -> "▼ THOUGHT FOR A MOMENT"
                    else -> "▶ THOUGHT FOR A MOMENT"
                },
                color = PhosphorGreenDim,
                fontFamily = SpaceGroteskSemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            )
        }
        if (expanded && hasThinking) {
            Text(
                text = thinking,
                color = PhosphorGreenDim,
                fontFamily = SpaceGroteskRegular,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = PhosphorGreenDim,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                    .padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}

/**
 * Interleaved tool-call card. Auto-run (no approve/deny): shows the tool name +
 * arguments, a status (running / ✓ duration / ✗ duration), and a collapsible
 * result body once finished. Left border encodes status colour.
 */
@Composable
fun ToolCallCard(
    tool: ToolCallUi,
    onToggle: () -> Unit,
) {
    val statusColor = when (tool.status) {
        ToolStatus.Running -> TertiaryAmber
        ToolStatus.Ok -> PhosphorGreen
        ToolStatus.Err -> TerminalDanger
    }
    val statusLabel = when (tool.status) {
        ToolStatus.Running -> "running…"
        ToolStatus.Ok -> "✓ ${tool.durationMs}ms"
        ToolStatus.Err -> "✗ ${tool.durationMs}ms"
    }
    val hasResult = tool.status != ToolStatus.Running && tool.resultText.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow, RoundedCornerShape(0.dp))
            .drawBehind {
                drawLine(
                    color = statusColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 3.dp.toPx(),
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = hasResult,
                onClick = onToggle,
            )
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🔧 ${tool.name}",
                color = PhosphorGreen,
                fontFamily = SpaceGroteskSemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = statusLabel,
                color = statusColor,
                fontFamily = SpaceGroteskRegular,
                fontSize = 10.sp,
            )
        }
        if (tool.args.isNotBlank() && tool.args != "{}") {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tool.args,
                color = PhosphorGreenDim,
                fontFamily = SpaceGroteskRegular,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (hasResult) {
            Spacer(modifier = Modifier.height(4.dp))
            if (tool.isResultExpanded) {
                Text(
                    text = tool.resultText,
                    color = PhosphorGreenDim,
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            } else {
                Text(
                    text = "show result ▼",
                    color = PhosphorGreenDim,
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

/** Small "jump to latest" affordance shown when the user has scrolled up mid-stream. */
@Composable
fun ScrollToBottomButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(BackgroundDark, RoundedCornerShape(0.dp))
            .border(2.dp, PhosphorGreen, RoundedCornerShape(0.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "▼",
            color = PhosphorGreen,
            fontFamily = SpaceGroteskBold,
            fontSize = 16.sp,
        )
    }
}
