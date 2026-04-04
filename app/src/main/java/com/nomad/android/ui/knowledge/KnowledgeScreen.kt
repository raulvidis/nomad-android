package com.nomad.android.ui.knowledge

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState as rememberHScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.ui.components.PipBoyBg
import com.nomad.android.ui.components.PipBoyDivider
import com.nomad.android.ui.components.PipBoyEmptyScreen
import com.nomad.android.ui.components.PipBoyErrorScreen
import com.nomad.android.ui.components.PipBoyGreen
import com.nomad.android.ui.components.PipBoyGreenDim
import com.nomad.android.ui.components.PipBoyLoadingScreen
import com.nomad.android.ui.components.PipBoyText
import com.nomad.android.ui.components.PipBoyTextField

@Composable
fun KnowledgeScreen(
    viewModel: KnowledgeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> PipBoyLoadingScreen("INDEXING ARCHIVES...")
        uiState.error != null -> PipBoyErrorScreen(uiState.error) { viewModel.loadArticles() }
        uiState.data.articles.isEmpty() -> PipBoyEmptyScreen(
            message = "No articles in this category. Download content packs in Settings."
        )
        else -> KnowledgeContent(
            data = uiState.data,
            onSearch = { viewModel.search(it) },
            onSelectCategory = { viewModel.selectCategory(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it) }
        )
    }
}

@Composable
private fun KnowledgeContent(
    data: KnowledgeData,
    onSearch: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }

    val categoryColor = mapOf(
        "Survival" to PipBoyGreen,
        "First Aid" to Color(0xFFFFB000),
        "Navigation" to Color(0xFF64B5F6),
        "Shelter" to Color(0xFFCE93D8),
        "All" to PipBoyGreen,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBoyBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PipBoyText(
            text = "ROBCO INDUSTRIES (TM) TERMLINK PROTOCOL",
            style = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
            color = PipBoyGreenDim,
        )

        PipBoyText(
            text = "ARCHIVES — OFFLINE KNOWLEDGE BASE",
            style = TextStyle(fontSize = 16.sp, fontFamily = FontFamily.Monospace),
            color = PipBoyGreen,
        )

        PipBoyDivider()

        PipBoyTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = "SEARCH ARCHIVES...",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberHScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            data.categories.forEach { category ->
                val isSelected = category == data.selectedCategory
                val bgColor = if (isSelected) PipBoyGreen else Color.Transparent
                val textColor = if (isSelected) PipBoyBg else PipBoyGreenDim
                val borderColor = if (isSelected) PipBoyGreen else PipBoyGreenDim

                Box(
                    modifier = Modifier
                        .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                        .background(bgColor, RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectCategory(category) },
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = category,
                        color = textColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(data.articles, key = { it.id }) { article ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = article.category,
                                color = categoryColor[article.category] ?: PipBoyGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                            )
                            if (article.isFavorite) {
                                Text(
                                    text = "★",
                                    color = Color(0xFFFFB000),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = article.title,
                            color = PipBoyGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                        )
                    }
                    Text(
                        text = ">",
                        color = PipBoyGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                    )
                }
            }
        }

        Text(
            text = "${data.articles.size} ARTICLES AVAILABLE",
            color = PipBoyGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
    }
}
