package com.nomad.android.ui.emergency

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.ui.components.PipBoyAmber
import com.nomad.android.ui.components.PipBoyBg
import com.nomad.android.ui.components.PipBoyCard
import com.nomad.android.ui.components.PipBoyDanger
import com.nomad.android.ui.components.PipBoyDivider
import com.nomad.android.ui.components.PipBoyGreen
import com.nomad.android.ui.components.PipBoyGreenDim
import com.nomad.android.ui.components.PipBoySurface
import com.nomad.android.ui.components.PipBoyText

private data class EmergencyQuickAccess(
    val emoji: String,
    val title: String,
    val subtitle: String,
)

@Composable
fun EmergencyScreen(
    viewModel: EmergencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val data = uiState.data

    val infiniteTransition = rememberInfiniteTransition(label = "emergency")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "flashAlpha",
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val quickAccessItems = listOf(
        EmergencyQuickAccess("\u2695\uFE0F", "FIRST AID", "Conditions A-Z"),
        EmergencyQuickAccess("\uD83C\uDD98", "SOS SIGNALS", "Visual reference"),
        EmergencyQuickAccess("\uD83C\uDFE5", "NEAREST HOSPITAL", "From offline map"),
        EmergencyQuickAccess("\uD83C\uDF92", "SURVIVAL CHECKLIST", "Essential items"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBoyBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "WARNING",
            color = PipBoyDanger.copy(alpha = flashAlpha),
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
        )

        Text(
            text = "EMERGENCY PROTOCOLS ACTIVE",
            color = PipBoyDanger,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
        )

        PipBoyDivider(color = PipBoyDanger)

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .border(3.dp, PipBoyDanger, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "SOS",
                    color = PipBoyDanger,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 40.sp,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickAccessItems.take(2).forEach { item ->
                    EmergencyCard(
                        item = item,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickAccessItems.drop(2).forEach { item ->
                    EmergencyCard(
                        item = item,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        PipBoyText(
            text = "EMERGENCY CONTACTS",
            style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            color = PipBoyAmber,
        )

        data.contacts.forEach { contact ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\uD83D\uDCDE",
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        color = PipBoyAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = contact.number,
                        color = PipBoyGreenDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ALL EMERGENCY CONTENT AVAILABLE OFFLINE \u2014 NO DOWNLOAD REQUIRED",
                color = PipBoyGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun EmergencyCard(
    item: EmergencyQuickAccess,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(PipBoySurface, RoundedCornerShape(4.dp))
            .border(1.dp, PipBoyAmber, RoundedCornerShape(4.dp))
            .padding(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.emoji,
                fontSize = 20.sp,
            )
            Text(
                text = item.title,
                color = PipBoyAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
            Text(
                text = item.subtitle,
                color = PipBoyGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
    }
}
