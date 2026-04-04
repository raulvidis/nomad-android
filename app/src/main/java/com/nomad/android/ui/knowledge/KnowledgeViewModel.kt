package com.nomad.android.ui.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.local.entity.SearchHistoryEntity
import com.nomad.android.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Article(
    val id: String,
    val title: String,
    val category: String,
    val content: String,
    val isFavorite: Boolean = false
)

data class KnowledgeData(
    val articles: List<Article> = emptyList(),
    val categories: List<String> = listOf("All", "Survival", "First Aid", "Navigation", "Shelter"),
    val selectedCategory: String = "All",
    val searchQuery: String = ""
)

data class KnowledgeUiState(
    val data: KnowledgeData = KnowledgeData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeUiState(isLoading = true))
    val uiState: StateFlow<KnowledgeUiState> = _uiState.asStateFlow()

    init {
        loadArticles()
    }

    private fun loadArticles() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            // Load from bundled content and database
            val mockArticles = listOf(
                Article("1", "CPR Basics", "First Aid", "How to perform CPR in emergency situations..."),
                Article("2", "Water Purification", "Survival", "Methods for purifying water in the wild..."),
                Article("3", "Shelter Building", "Shelter", "Emergency shelter construction techniques..."),
                Article("4", "Fire Starting", "Survival", "Various methods for starting a fire without matches..."),
                Article("5", "Navigation by Stars", "Navigation", "Using celestial bodies for navigation...")
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    data = it.data.copy(articles = mockArticles)
                )
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(data = it.data.copy(searchQuery = query)) }

        viewModelScope.launch {
            searchRepository.insertHistoryEntry(
                SearchHistoryEntity(query = query, source = "knowledge", timestamp = System.currentTimeMillis())
            )

            val filtered = _uiState.value.data.articles.filter { article ->
                article.title.contains(query, ignoreCase = true) ||
                article.content.contains(query, ignoreCase = true)
            }
            _uiState.update { it.copy(data = it.data.copy(articles = filtered)) }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(data = it.data.copy(selectedCategory = category)) }
        loadArticles()
    }

    fun toggleFavorite(articleId: String) {
        val updated = _uiState.value.data.articles.map { article ->
            if (article.id == articleId) article.copy(isFavorite = !article.isFavorite) else article
        }
        _uiState.update { it.copy(data = it.data.copy(articles = updated)) }
    }
}
