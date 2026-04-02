package com.nomad.android.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomad.android.ui.theme.PipBoyAmber
import com.nomad.android.ui.theme.PipBoyButton
import com.nomad.android.ui.theme.PipBoyCard
import com.nomad.android.ui.theme.PipBoyDivider
import com.nomad.android.ui.theme.PipBoyGreen
import com.nomad.android.ui.theme.PipBoyGreenDim
import com.nomad.android.ui.theme.PipBoyProgressBar

@Composable
fun SettingsScreen() {
    val selectedTheme = remember { mutableStateOf("CRT GREEN") }
    val themes = listOf("CRT GREEN", "AMBER PHOSPHOR", "BLUE SCREEN")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ROBCO INDUSTRIES (TM)",
            color = PipBoyGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Text(
            text = "SYSTEM CONFIGURATION",
            color = PipBoyGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
        PipBoyDivider()
        Spacer(modifier = Modifier.height(16.dp))

        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle("AI ENGINE")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Engine: On-Device (Gemma)",
                color = PipBoyGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "Model: Gemma 4 E2B",
                color = PipBoyGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .border(1.dp, PipBoyGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STATUS: READY",
                    color = PipBoyGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle("CONTENT PACKS")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Essentials Pack .......... DOWNLOADED",
                color = PipBoyGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(
                text = "Wikipedia Mini ............ AVAILABLE",
                color = PipBoyGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PipBoyButton(onClick = { }) {
                    Text("DOWNLOAD", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                PipBoyButton(onClick = { }) {
                    Text("DELETE", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle("STORAGE")
            Spacer(modifier = Modifier.height(8.dp))
            PipBoyProgressBar(progress = 0.1f, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "3.2 GB / 32 GB USED",
                color = PipBoyGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle("DISPLAY")
            Spacer(modifier = Modifier.height(8.dp))
            themes.forEach { theme ->
                val isSelected = selectedTheme.value == theme
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTheme.value = theme }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectorChar = if (isSelected) "[X]" else "[ ]"
                    Text(
                        text = "$selectorChar $theme",
                        color = if (isSelected) PipBoyGreen else PipBoyGreenDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle("ABOUT")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NOMAD v0.1.0",
                color = PipBoyGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "VAULT-TEC SURVIVAL SYSTEMS",
                color = PipBoyGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "Powered by Gemma 4",
                color = PipBoyGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(
                text = "Open source — Apache 2.0 License",
                color = PipBoyGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = PipBoyAmber,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp
    )
}
