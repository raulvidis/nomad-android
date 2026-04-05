package com.nomad.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomad.android.R
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalBorder
import com.nomad.android.ui.theme.TerminalDanger
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim
import com.nomad.android.ui.theme.TerminalSurface
import com.nomad.android.ui.theme.TerminalAmber

enum class TerminalButtonVariant { NORMAL, DANGER, AMBER, DISABLED }

enum class TerminalButtonSize { LARGE, MEDIUM, SMALL }

@Composable
fun TerminalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: TerminalButtonVariant = TerminalButtonVariant.NORMAL,
    size: TerminalButtonSize = TerminalButtonSize.MEDIUM,
    tintColor: Color? = null,
) {
    val resolvedVariant = if (!enabled) TerminalButtonVariant.DISABLED else variant

    val defaultColor = when (resolvedVariant) {
        TerminalButtonVariant.NORMAL -> tintColor ?: TerminalGreen
        TerminalButtonVariant.DANGER -> TerminalDanger
        TerminalButtonVariant.AMBER -> TerminalAmber
        TerminalButtonVariant.DISABLED -> Color(0xFF444444)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = if (isPressed && resolvedVariant != TerminalButtonVariant.DISABLED) {
        defaultColor
    } else {
        TerminalBg
    }

    val contentColor = if (isPressed && resolvedVariant != TerminalButtonVariant.DISABLED) {
        TerminalBg
    } else {
        defaultColor
    }

    val minHeight = when (size) {
        TerminalButtonSize.LARGE -> 48.dp
        TerminalButtonSize.MEDIUM -> 40.dp
        TerminalButtonSize.SMALL -> 32.dp
    }

    val fontSize = when (size) {
        TerminalButtonSize.LARGE -> 14.sp
        TerminalButtonSize.MEDIUM -> 13.sp
        TerminalButtonSize.SMALL -> 11.sp
    }

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .border(2.dp, defaultColor, RoundedCornerShape(8.dp))
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
            ),
            fontSize = fontSize,
        )
    }
}

@Composable
fun TerminalCard(
    modifier: Modifier = Modifier,
    header: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalSurface, RoundedCornerShape(8.dp))
            .border(2.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        if (header != null) {
            Text(
                text = header.uppercase(),
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
                fontSize = 14.sp,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TerminalGreenDim.copy(alpha = 0.3f)),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        content()
    }
}

@Composable
fun TerminalText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = TerminalGreen,
    glow: Boolean = false,
) {
    val mergedStyle = style.merge(
        TextStyle(
            color = color,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            shadow = if (glow) {
                Shadow(
                    color = color,
                    blurRadius = 2f,
                    offset = Offset(0f, 0f),
                )
            } else {
                null
            },
        ),
    )

    Text(
        text = text,
        modifier = modifier,
        style = mergedStyle,
    )
}

@Composable
fun TerminalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    singleLine: Boolean = false,
    enabled: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) TerminalGreen else TerminalGreenDim

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                color = TerminalGreenDim,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(2.dp, borderColor, RoundedCornerShape(6.dp))
                .background(TerminalBg, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .padding(12.dp)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 14.sp,
            ),
            singleLine = singleLine,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = TerminalGreenDim,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                            ),
                            fontSize = 14.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
fun TerminalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = TerminalGreen,
) {
    val segmentCount = 12
    val segmentGap = 2.dp
    val filledSegments = (progress.coerceIn(0f, 1f) * segmentCount).toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .background(TerminalGreenDim.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(segmentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until segmentCount) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        if (i < filledSegments) color else Color.Transparent,
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

@Composable
fun TerminalStatusIndicator(
    label: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    val dotColor = if (isOnline) TerminalGreen else TerminalGreenDim

    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOnline) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blinkAlpha",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    dotColor.copy(alpha = blinkAlpha),
                    CircleShape,
                ),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = TerminalGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 12.sp,
        )
    }
}

@Composable
fun TerminalDivider(
    modifier: Modifier = Modifier,
    color: Color = TerminalGreenDim,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@Composable
fun TerminalListTile(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                ),
                fontSize = 14.sp,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
            }
        }
        Text(
            text = ">",
            color = TerminalGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 16.sp,
        )
    }
}

@Composable
fun TerminalSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = text.uppercase(),
            color = TerminalGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
            ),
            fontSize = 16.sp,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TerminalGreenDim.copy(alpha = 0.3f)),
        )
    }
}

@Composable
fun TerminalLoadingScreen(
    message: String = "INITIALIZING...",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = TerminalGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
            ),
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "[████████░░░░░░░░░░░░]",
            color = TerminalGreenDim,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 14.sp,
        )
    }
}

@Composable
fun TerminalErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "ERROR: $message",
            color = TerminalDanger,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TerminalButton(text = "RETRY", onClick = onRetry, variant = TerminalButtonVariant.DANGER)
    }
}

@Composable
fun TerminalEmptyScreen(
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "NO DATA FOUND",
            color = TerminalGreenDim,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = TerminalGreenDim,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 12.sp,
        )
        if (action != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TerminalButton(text = action, onClick = onAction)
        }
    }
}
