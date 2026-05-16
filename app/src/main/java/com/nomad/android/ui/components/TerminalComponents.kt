package com.nomad.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomad.android.R
import com.nomad.android.ui.theme.PhosphorGreen
import com.nomad.android.ui.theme.PhosphorGreenDim
import com.nomad.android.ui.theme.BackgroundDark
import com.nomad.android.ui.theme.SurfaceContainerLowest
import com.nomad.android.ui.theme.SurfaceContainerLow
import com.nomad.android.ui.theme.SurfaceContainerHighest
import com.nomad.android.ui.theme.TerminalDanger
import com.nomad.android.ui.theme.TertiaryAmber
import com.nomad.android.ui.theme.OutlineVariant

private val SpaceGroteskBold = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_bold, FontWeight.Bold),
)
private val SpaceGroteskSemiBold = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold),
)
private val SpaceGroteskRegular = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
)

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
        TerminalButtonVariant.NORMAL -> tintColor ?: PhosphorGreen
        TerminalButtonVariant.DANGER -> TerminalDanger
        TerminalButtonVariant.AMBER -> TertiaryAmber
        TerminalButtonVariant.DISABLED -> Color(0xFF444444)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = if (isPressed && resolvedVariant != TerminalButtonVariant.DISABLED) {
        defaultColor
    } else {
        BackgroundDark
    }

    val contentColor = if (isPressed && resolvedVariant != TerminalButtonVariant.DISABLED) {
        BackgroundDark
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

    val shape = RoundedCornerShape(0.dp)

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .then(
                if (resolvedVariant == TerminalButtonVariant.NORMAL && !isPressed) {
                    Modifier.shadow(4.dp, ambientColor = PhosphorGreen.copy(alpha = 0.4f), spotColor = PhosphorGreen.copy(alpha = 0.4f), shape = shape)
                } else {
                    Modifier
                }
            )
            .border(2.dp, defaultColor, shape)
            .background(backgroundColor, shape)
            .clip(shape)
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
            fontFamily = SpaceGroteskSemiBold,
            fontSize = fontSize,
        )
    }
}

@Composable
fun TerminalCard(
    modifier: Modifier = Modifier,
    header: String? = null,
    moduleLabel: String? = null,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow, RoundedCornerShape(0.dp)),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxSize()
                .background(PhosphorGreen),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
        ) {
            if (header != null || moduleLabel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (header != null) {
                        Text(
                            text = header.uppercase(),
                            color = PhosphorGreen,
                            fontFamily = SpaceGroteskBold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (moduleLabel != null) {
                        Text(
                            text = moduleLabel,
                            color = PhosphorGreenDim,
                            fontFamily = SpaceGroteskRegular,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PhosphorGreenDim.copy(alpha = 0.3f)),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            content()
        }
    }
}

@Composable
fun TerminalText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = PhosphorGreen,
    glow: Boolean = false,
) {
    val mergedStyle = style.merge(
        TextStyle(
            color = color,
            fontFamily = SpaceGroteskRegular,
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
    onSubmit: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) PhosphorGreen else PhosphorGreen.copy(alpha = 0.2f)
    val shape = RoundedCornerShape(0.dp)

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                color = PhosphorGreenDim,
                        fontFamily = SpaceGroteskRegular,
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
                .then(
                    if (isFocused) {
                        Modifier.shadow(4.dp, ambientColor = PhosphorGreen.copy(alpha = 0.3f), spotColor = PhosphorGreen.copy(alpha = 0.3f), shape = shape)
                    } else {
                        Modifier
                    }
                )
                .border(2.dp, borderColor, shape)
                .background(SurfaceContainerLowest, shape)
                .clip(shape)
                .padding(12.dp)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(
                color = PhosphorGreen,
                fontFamily = SpaceGroteskRegular,
                fontSize = 14.sp,
                shadow = Shadow(
                    color = PhosphorGreen.copy(alpha = 0.3f),
                    blurRadius = 1f,
                    offset = Offset(0f, 0f),
                ),
            ),
            singleLine = singleLine,
            keyboardOptions = if (onSubmit != null) KeyboardOptions(imeAction = ImeAction.Search) else KeyboardOptions.Default,
            keyboardActions = if (onSubmit != null) KeyboardActions(onSearch = { onSubmit() }) else KeyboardActions.Default,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = PhosphorGreenDim,
                            fontFamily = SpaceGroteskRegular,
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
    color: Color = PhosphorGreen,
) {
    val segmentCount = 12
    val segmentGap = 2.dp
    val filledSegments = (progress.coerceIn(0f, 1f) * segmentCount).toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .background(SurfaceContainerHighest, RoundedCornerShape(0.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(segmentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until segmentCount) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .then(
                        if (i < filledSegments) {
                            Modifier.shadow(
                                2.dp,
                                ambientColor = PhosphorGreen.copy(alpha = 0.4f),
                                spotColor = PhosphorGreen.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(0.dp),
                            )
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        if (i < filledSegments) color else Color.Transparent,
                        RoundedCornerShape(0.dp),
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
    val dotColor = if (isOnline) PhosphorGreen else PhosphorGreenDim

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
            color = PhosphorGreen,
            fontFamily = SpaceGroteskRegular,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun TerminalDivider(
    modifier: Modifier = Modifier,
    color: Color = OutlineVariant.copy(alpha = 0.2f),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TerminalListTile(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    index: Int = 0,
    onLongClick: (() -> Unit)? = null,
) {
    val bgColor = if (index % 2 == 1) SurfaceContainerLowest else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(bgColor)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onLongClick = onLongClick,
                        onClick = onClick,
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                }
            )
            .padding(horizontal = 12.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PhosphorGreen,
                    fontFamily = SpaceGroteskSemiBold,
                fontSize = 14.sp,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = PhosphorGreenDim,
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 12.sp,
                )
            }
        }
        Text(
            text = ">",
            color = PhosphorGreen,
            fontFamily = SpaceGroteskRegular,
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
            color = PhosphorGreen,
                        fontFamily = SpaceGroteskBold,
            fontSize = 16.sp,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .drawBehind {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                PhosphorGreen.copy(alpha = 0.4f),
                                Color.Transparent,
                            ),
                        ),
                    )
                },
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
            color = PhosphorGreen,
            fontFamily = SpaceGroteskSemiBold,
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "[████████░░░░░░░░░░░░]",
            color = PhosphorGreenDim,
            fontFamily = SpaceGroteskRegular,
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
            fontFamily = SpaceGroteskRegular,
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
            color = PhosphorGreenDim,
            fontFamily = SpaceGroteskRegular,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = PhosphorGreenDim,
            fontFamily = SpaceGroteskRegular,
            fontSize = 12.sp,
        )
        if (action != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TerminalButton(text = action, onClick = onAction)
        }
    }
}
