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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalEmptyScreen
import com.nomad.android.ui.components.TerminalErrorScreen
import com.nomad.android.ui.components.TerminalLoadingScreen
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.components.TerminalTextField
import com.nomad.android.ui.theme.OutlineVariant
import com.nomad.android.ui.theme.TertiaryAmber
import com.nomad.android.ui.theme.BackgroundDark
import com.nomad.android.ui.theme.PhosphorGreen
import com.nomad.android.ui.theme.PhosphorGreenDim
import com.nomad.android.ui.theme.SurfaceContainerLow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val SpaceGroteskBold = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_bold, FontWeight.Bold),
)
private val SpaceGroteskSemiBold = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_semi_bold, FontWeight.Medium),
)
private val SpaceGroteskRegular = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
)

@Composable
fun KnowledgeScreen(
    viewModel: KnowledgeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> TerminalLoadingScreen("INDEXING ARCHIVES...")
        uiState.error != null -> TerminalErrorScreen(message = uiState.error ?: "Unknown error", onRetry = { viewModel.loadArticles() })
        uiState.data.articles.isEmpty() -> TerminalEmptyScreen(
            message = "No articles available. Download content packs in Settings.",
        )
        uiState.data.filteredArticles.isEmpty() && uiState.data.searchQuery.isNotBlank() -> TerminalEmptyScreen(
            message = "No articles match '${uiState.data.searchQuery}'",
        )
        uiState.data.filteredArticles.isEmpty() -> TerminalEmptyScreen(
            message = "No articles in this category. Download content packs in Settings.",
        )
        else -> KnowledgeContent(
            data = uiState.data,
            onSearch = { viewModel.search(it) },
            onSearchSubmit = { viewModel.submitSearch(it) },
            onSelectCategory = { viewModel.selectCategory(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
        )
    }
}

@Composable
private fun KnowledgeContent(
    data: KnowledgeData,
    onSearch: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(searchText) {
        delay(300)
        onSearch(searchText)
    }

    val categoryColor = mapOf(
        "Survival" to PhosphorGreen,
        "First Aid" to TertiaryAmber,
        "Navigation" to Color(0xFF64B5F6),
        "Shelter" to Color(0xFFCE93D8),
        "All" to PhosphorGreen,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TerminalText(
            text = "Offline Knowledge Base",
            color = PhosphorGreen,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontFamily = SpaceGroteskBold,
            ),
        )

        TerminalTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = "Search archives...",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            onSubmit = { onSearchSubmit(searchText) },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberHScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            data.categories.forEach { category ->
                val isSelected = category == data.selectedCategory
                val bgColor = if (isSelected) PhosphorGreen else Color.Transparent
                val textColor = if (isSelected) BackgroundDark else PhosphorGreenDim
                val borderColor = if (isSelected) PhosphorGreen else PhosphorGreenDim

                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor, RoundedCornerShape(0.dp))
                        .background(bgColor, RoundedCornerShape(0.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectCategory(category) },
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = category,
                        color = textColor,
                        fontFamily = SpaceGroteskSemiBold,
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
                            if (isExpanded) SurfaceContainerLow else Color.Transparent,
                            RoundedCornerShape(0.dp),
                        )
                        .then(
                            if (isExpanded) Modifier.border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
                            else Modifier,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                expandedArticleId = if (isExpanded) null else article.id
                            },
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                    color = categoryColor[article.category] ?: PhosphorGreen,
                                    fontFamily = SpaceGroteskRegular,
                                    fontSize = 11.sp,
                                )
                                if (article.isFavorite) {
                                    Text(
                                        text = "\u2605",
                                        color = TertiaryAmber,
                                        fontFamily = SpaceGroteskRegular,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = article.title,
                                color = PhosphorGreen,
                            fontFamily = SpaceGroteskSemiBold,
                                fontSize = 14.sp,
                            )
                        }
                        Text(
                            text = if (isExpanded) "[-]" else "[+]",
                            color = PhosphorGreen,
                            fontFamily = SpaceGroteskRegular,
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
                                    .background(OutlineVariant.copy(alpha = 0.2f)),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = article.content,
                                color = TertiaryAmber,
                                fontFamily = SpaceGroteskRegular,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "${data.filteredArticles.size} / ${data.articles.size} articles",
            color = PhosphorGreenDim,
            fontFamily = SpaceGroteskRegular,
            fontSize = 11.sp,
        )
    }
}
