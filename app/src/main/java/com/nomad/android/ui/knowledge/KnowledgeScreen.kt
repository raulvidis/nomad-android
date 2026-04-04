package com.nomad.android.ui.knowledge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.nomad.android.ui.theme.PipBoyAmber
import com.nomad.android.ui.theme.PipBoySurface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.ui.theme.PipBoyBg
import com.nomad.android.ui.components.PipBoyDivider
import com.nomad.android.ui.components.PipBoyEmptyScreen
import com.nomad.android.ui.components.PipBoyErrorScreen
import com.nomad.android.ui.theme.PipBoyGreen
import com.nomad.android.ui.theme.PipBoyGreenDim
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
        uiState.error != null -> PipBoyErrorScreen(message = uiState.error ?: "Unknown error", onRetry = { viewModel.loadArticles() })
        uiState.data.articles.isEmpty() -> PipBoyEmptyScreen(
            message = "No articles available. Download content packs in Settings."
        )
        uiState.data.filteredArticles.isEmpty() && uiState.data.searchQuery.isNotBlank() -> PipBoyEmptyScreen(
            message = "No articles match '${uiState.data.searchQuery}'"
        )
        uiState.data.filteredArticles.isEmpty() -> PipBoyEmptyScreen(
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

    LaunchedEffect(searchText) {
        delay(300)
        onSearch(searchText)
    }

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

        var expandedArticleId by remember { mutableStateOf<String?>(null) }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(data.filteredArticles, key = { it.id }) { article ->
                val isExpanded = expandedArticleId == article.id

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isExpanded) PipBoySurface else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .then(
                            if (isExpanded) Modifier.border(1.dp, PipBoyGreenDim, RoundedCornerShape(4.dp))
                            else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                expandedArticleId = if (isExpanded) null else article.id
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                            text = if (isExpanded) "[-]" else "[+]",
                            color = PipBoyGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(PipBoyGreenDim.copy(alpha = 0.3f))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = article.content,
                                color = PipBoyAmber,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "${data.filteredArticles.size} / ${data.articles.size} ARTICLES",
            color = PipBoyGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
    }
}
