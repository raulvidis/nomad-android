package com.nomad.android.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.ui.components.PipBoyButton
import com.nomad.android.ui.components.PipBoyProgressBar
import kotlinx.coroutines.delay

private val TerminalGreen = Color(0xFF14FF14)
private val DimGreen = Color(0xFF0A7A0A)
private val Amber = Color(0xFFFFB000)
private val DarkBg = Color(0xFF0A0A0A)
private val CardBg = Color(0xFF0D1A0D)
private val BrightGreen = Color(0xFF00FF41)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isComplete by viewModel.isOnboardingComplete.collectAsStateWithLifecycle(initial = false)

    if (isComplete) {
        onComplete()
        return
    }

    var currentStep by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        when (currentStep) {
            0 -> BootSequenceStep { currentStep = 1 }
            1 -> DeviceScanStep { currentStep = 2 }
            2 -> ModelSelectionStep(
                selectedModel = uiState.data.selectedModel,
                onSelectModel = { viewModel.selectModel(it) }
            ) { currentStep = 3 }
            3 -> DownloadPackStep { currentStep = 4 }
            else -> WelcomeStep { viewModel.completeOnboarding() }
        }
    }
}

    var currentStep by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        when (currentStep) {
            0 -> BootSequenceStep { currentStep = 1 }
            1 -> DeviceScanStep { currentStep = 2 }
            2 -> ModelSelectionStep(
                selectedModel = uiState.data.selectedModel,
                onSelectModel = { viewModel.selectModel(it) }
            ) { currentStep = 3 }
            3 -> DownloadPackStep { currentStep = 4 }
            else -> WelcomeStep { viewModel.completeOnboarding() }
        }
    }
}

@Composable
private fun BootSequenceStep(onAdvance: () -> Unit) {
    val lines = listOf(
        "ROBCO INDUSTRIES (TM) TERMLINK PROTOCOL",
        "INITIATING VAULT SURVIVAL SYSTEM...",
        "FIRMWARE v4.0.1 LOADED",
        "SYSTEM CHECK: PASS",
        "POWER ON: [OK]"
    )

    var visibleLines by remember { mutableIntStateOf(0) }
    var currentText by remember { mutableStateOf("") }
    var charIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        for (lineIndex in lines.indices) {
            currentText = ""
            charIndex = 0
            while (charIndex < lines[lineIndex].length) {
                currentText = lines[lineIndex].substring(0, charIndex + 1)
                charIndex++
                delay(25)
            }
            currentText = lines[lineIndex]
            visibleLines = lineIndex + 1
            delay(300)
        }
        delay(1500)
        onAdvance()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        for (i in 0 until visibleLines) {
            val isLastLine = i == visibleLines - 1
            Text(
                text = if (isLastLine) currentText else lines[i],
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (visibleLines > 0 && currentText.length < (lines.getOrNull(visibleLines - 1)?.length ?: 0)) {
            Text(
                text = "_",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun DeviceScanStep(onAdvance: () -> Unit) {
    val specs = listOf(
        "RAM: 8192 MB [OK]" to 500L,
        "STORAGE: 32.0 GB AVAILABLE [OK]" to 400L,
        "AI CAPABILITY: AICORE DETECTED [OK]" to 600L,
        "GPU: ADRENO 750 [OK]" to 350L,
        "NPU: AVAILABLE [OK]" to 300L
    )

    var visibleCount by remember { mutableIntStateOf(0) }
    var headerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)
        headerVisible = true
        delay(600)
        for (i in specs.indices) {
            delay(specs[i].second)
            visibleCount = i + 1
        }
        delay(1000)
        onAdvance()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = headerVisible,
            enter = fadeIn(tween(300))
        ) {
            Text(
                text = "SCANNING HARDWARE...",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        for (i in 0 until visibleCount) {
            AnimatedVisibility(
                visible = i < visibleCount,
                enter = slideInVertically(tween(200)) + fadeIn(tween(200))
            ) {
                val text = specs[i].first
                val annotated = buildAnnotatedString {
                    val okIndex = text.lastIndexOf("[OK]")
                    if (okIndex >= 0) {
                        append(text.substring(0, okIndex))
                        withStyle(SpanStyle(color = BrightGreen)) {
                            append("[OK]")
                        }
                    } else {
                        append(text)
                    }
                }
                Text(
                    text = annotated,
                    color = DimGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ModelSelectionStep(
    selectedModel: String,
    onSelectModel: (String) -> Unit,
    onAdvance: () -> Unit
) {
    val models = remember {
        listOf(
            Triple("GEMMA 4 E2B", "3.0 GB", "Full capability model — recommended for 6GB+ RAM devices"),
            Triple("GEMMA 3 1B", "1.0 GB", "Lightweight model — works on most devices"),
            Triple("FALLBACK", "N/A", "Rule-based offline responses — no download required")
        )
    }

    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SELECT AI MODEL:",
            color = Amber,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        models.forEachIndexed { index, (name, size, desc) ->
            val isSelected = index == selectedIndex
            val borderColor = if (isSelected) BrightGreen else DimGreen
            val textColor = if (isSelected) TerminalGreen else DimGreen

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = borderColor
                    )
                    .background(CardBg)
                    .clickable { selectedIndex = index }
                    .padding(16.dp)
                    .padding(bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = textColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "— $size",
                        color = if (isSelected) Amber else DimGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    if (index == 0 && isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[RECOMMENDED]",
                            color = Amber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    color = if (isSelected) DimGreen else Color(0xFF063F06),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        PipBoyButton(
            text = "CONFIRM SELECTION",
            onClick = { onAdvance() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DownloadPackStep(onAdvance: () -> Unit) {
    val files = listOf(
        "first_aid.json" to 0.1f,
        "survival.json" to 0.3f,
        "maps_region.pmtiles" to 0.7f
    )

    var progress by remember { mutableFloatStateOf(0f) }
    var visibleFiles by remember { mutableIntStateOf(0) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "downloadProgress"
    )

    LaunchedEffect(Unit) {
        for ((index, fileProgress) in files.withIndex()) {
            progress = fileProgress
            delay(800)
            visibleFiles = index + 1
        }
        progress = 1f
        delay(800)
        onAdvance()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "INITIAL DATA PACK",
            color = Amber,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Downloading essential survival content...",
            color = DimGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        PipBoyProgressBar(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        for (i in 0 until visibleFiles) {
            val annotated = buildAnnotatedString {
                append(files[i].first + " ")
                withStyle(SpanStyle(color = BrightGreen)) {
                    append("[OK]")
                }
            }
            Text(
                text = annotated,
                color = DimGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun WelcomeStep(onComplete: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(800))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NOMAD",
                color = BrightGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 48.sp,
                letterSpacing = 8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "VAULT SURVIVAL SYSTEM READY",
                color = Amber,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "All systems operational. Stay safe out there, Wanderer.",
                color = DimGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            PipBoyButton(
                text = "ENTER THE WASTELAND",
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}
