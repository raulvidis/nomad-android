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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomad.android.ui.theme.PipBoyGreen
import com.nomad.android.ui.theme.PipBoyGreenDim
import com.nomad.android.ui.theme.PipBoyAmber
import com.nomad.android.ui.theme.PipBoyDanger
import com.nomad.android.ui.theme.PipBoyBg
import com.nomad.android.ui.theme.PipBoySurface

enum class PipBoyButtonVariant { NORMAL, DANGER, AMBER, DISABLED }

@Composable
fun PipBoyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: PipBoyButtonVariant = PipBoyButtonVariant.NORMAL,
) {
    val resolvedVariant = if (!enabled) PipBoyButtonVariant.DISABLED else variant

    val borderColor = when (resolvedVariant) {
        PipBoyButtonVariant.NORMAL -> PipBoyGreen
        PipBoyButtonVariant.DANGER -> PipBoyDanger
        PipBoyButtonVariant.AMBER -> PipBoyAmber
        PipBoyButtonVariant.DISABLED -> Color(0xFF444444)
    }

    val textColor = when (resolvedVariant) {
        PipBoyButtonVariant.NORMAL -> PipBoyGreen
        PipBoyButtonVariant.DANGER -> PipBoyDanger
        PipBoyButtonVariant.AMBER -> PipBoyAmber
        PipBoyButtonVariant.DISABLED -> Color(0xFF666666)
    }

    val pressedFillColor = when (resolvedVariant) {
        PipBoyButtonVariant.NORMAL -> PipBoyGreen
        PipBoyButtonVariant.DANGER -> PipBoyDanger
        PipBoyButtonVariant.AMBER -> PipBoyAmber
        PipBoyButtonVariant.DISABLED -> Color.Transparent
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = if (isPressed && resolvedVariant != PipBoyButtonVariant.DISABLED) {
        pressedFillColor
    } else {
        PipBoyBg
    }

    val contentColor = if (isPressed && resolvedVariant != PipBoyButtonVariant.DISABLED) {
        PipBoyBg
    } else {
        textColor
    }

    Box(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun PipBoyCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(PipBoySurface, RoundedCornerShape(4.dp))
            .border(1.dp, PipBoyGreen, RoundedCornerShape(4.dp))
            .padding(8.dp),
    ) {
        content()
    }
}

@Composable
fun PipBoyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = PipBoyGreen,
    glow: Boolean = true,
) {
    val mergedStyle = style.merge(
        TextStyle(
            color = color,
            fontFamily = FontFamily.Monospace,
            shadow = if (glow) {
                Shadow(
                    color = color,
                    blurRadius = 4f,
                    offset = Offset(0f, 0f),
                )
            } else {
                null
            },
        )
    )

    Text(
        text = text,
        modifier = modifier,
        style = mergedStyle,
    )
}

@Composable
fun PipBoyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .background(PipBoyBg)
            .drawBehind {
                drawLine(
                    color = PipBoyGreen,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        textStyle = TextStyle(
            color = PipBoyGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
        ),
        singleLine = singleLine,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = PipBoyGreenDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
fun PipBoyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = PipBoyGreen,
) {
    val segmentCount = 20
    val segmentGap = 2.dp
    val filledSegments = (progress.coerceIn(0f, 1f) * segmentCount).toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .background(PipBoyGreenDim.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
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
fun PipBoyStatusIndicator(
    label: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    val dotColor = if (isOnline) PipBoyGreen else PipBoyGreenDim

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
            color = PipBoyGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun PipBoyDivider(
    modifier: Modifier = Modifier,
    color: Color = PipBoyGreenDim,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@Composable
fun PipBoyListTile(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = PipBoyGreen),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PipBoyGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = PipBoyGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }
        Text(
            text = ">",
            color = PipBoyGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
        )
    }
}
