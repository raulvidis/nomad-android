package com.nomad.android.ui.knowledge

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.content.KiwixManager
import com.nomad.android.data.local.entity.SearchHistoryEntity
import com.nomad.android.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val filteredArticles: List<Article> = emptyList(),
    val categories: List<String> = listOf("All", "Survival", "First Aid", "Navigation", "Shelter", "Fire", "Food"),
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
    @ApplicationContext private val context: Context,
    private val searchRepository: SearchRepository,
    private val kiwixManager: KiwixManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeUiState(isLoading = true))
    val uiState: StateFlow<KnowledgeUiState> = _uiState.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val bundledArticles = loadBundledContent()
            val zimArticles = loadZimArticles()
            val allArticles = bundledArticles + zimArticles

            val categories = listOf("All") + allArticles.map { it.category }.distinct().sorted()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    data = KnowledgeData(
                        articles = allArticles,
                        filteredArticles = allArticles,
                        categories = categories
                    )
                )
            }
        }
    }

    private fun loadBundledContent(): List<Article> {
        return try {
            val json = context.resources.openRawResource(
                context.resources.getIdentifier("survival_content", "raw", context.packageName)
            ).bufferedReader().use { it.readText() }

            val parser = org.json.JSONObject(json)
            val articlesArray = parser.getJSONArray("articles")
            val articles = mutableListOf<Article>()

            for (i in 0 until articlesArray.length()) {
                val obj = articlesArray.getJSONObject(i)
                articles.add(
                    Article(
                        id = "bundled_$i",
                        title = obj.optString("title", "Unknown"),
                        category = obj.optString("category", "Survival"),
                        content = obj.optString("content", ""),
                        isFavorite = false
                    )
                )
            }
            articles
        } catch (e: Exception) {
            // Fallback to hardcoded content if JSON fails
            getFallbackArticles()
        }
    }

    private suspend fun loadZimArticles(): List<Article> {
        return try {
            val archives = kiwixManager.getLoadedArchives()
            val articles = mutableListOf<Article>()

            for (archive in archives) {
                kiwixManager.searchArticles("", archive).collect { results ->
                    results.forEachIndexed { index, result ->
                        articles.add(
                            Article(
                                id = "zim_${archive}_$index",
                                title = result.title,
                                category = "ZIM Archive",
                                content = result.snippet,
                                isFavorite = false
                            )
                        )
                    }
                }
            }
            articles
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getFallbackArticles(): List<Article> = listOf(
        Article("fb_1", "CPR Basics", "First Aid", "To perform CPR: 1) Check responsiveness. 2) Call emergency services. 3) Push hard and fast in the center of the chest at 100-120 compressions per minute. 4) Give rescue breaths if trained. 5) Continue until help arrives."),
        Article("fb_2", "Water Purification", "Survival", "To purify water: 1) Boil for at least 1 minute (3 minutes above 6,500 ft). 2) Use water purification tablets. 3) Filter through a clean cloth, then boil. 4) Solar disinfection: fill clear bottle and leave in direct sunlight for 6 hours."),
        Article("fb_3", "Fire Starting", "Fire", "To start a fire: 1) Gather tinder, kindling, and fuel. 2) Create a fire lay (teepee or log cabin). 3) Use matches, lighter, or friction method. 4) Shield from wind. 5) Never leave unattended. 6) Fully extinguish before leaving."),
        Article("fb_4", "Shelter Building", "Shelter", "Emergency shelter: 1) Find natural windbreaks. 2) Use branches and leaves for insulation. 3) Build a lean-to or debris hut. 4) Insulate the ground. 5) Keep shelter small to retain body heat. 6) Ensure ventilation if using fire nearby."),
        Article("fb_5", "Navigation Without Tools", "Navigation", "Navigation without tools: 1) Sun rises in east, sets in west. 2) North Star (Polaris) indicates north. 3) Moss grows thicker on north side of trees (northern hemisphere). 4) Follow waterways downstream to civilization. 5) Note landmarks."),
        Article("fb_6", "Essential Knots", "Survival", "Essential knots: 1) Bowline - secure loop that won't slip. 2) Clove hitch - quick attachment to posts. 3) Square knot - joining two ropes. 4) Taut-line hitch - adjustable tension. 5) Figure-eight - stopper knot."),
        Article("fb_7", "Edible Plants", "Food", "Edible plant identification: 1) Only eat plants you can positively identify. 2) Universal Edibility Test: test one part at a time. 3) Safe bets: dandelion, clover, cattail, pine needles (tea). 4) Avoid: milky sap, umbrella-shaped flowers, almond scent."),
        Article("fb_8", "SOS Signals", "Survival", "SOS signals: 1) Three of anything (fires, whistle blasts, flashes). 2) Ground-to-air signals: use contrasting materials, minimum 10ft tall. 3) Mirror signaling: flash toward aircraft. 4) Smoke: white smoke for daylight, bright fires at night."),
        Article("fb_9", "First Aid Basics", "First Aid", "First aid basics: 1) Stop bleeding first. 2) Treat for shock (lay down, elevate legs, keep warm). 3) Splint fractures with rigid materials. 4) Burns: cool with water, cover with clean dressing. 5) Never give food/water to unconscious person."),
        Article("fb_10", "Severe Bleeding", "First Aid", "For severe bleeding: 1) Apply direct pressure with a clean cloth. 2) Elevate the wound above the heart if possible. 3) Apply a pressure bandage. 4) Do not remove soaked cloths - add more on top. 5) Seek medical help immediately.")
    )

    fun search(query: String) {
        _uiState.update { it.copy(data = it.data.copy(searchQuery = query)) }

        viewModelScope.launch {
            if (query.isNotBlank()) {
                searchRepository.insertHistoryEntry(
                    SearchHistoryEntity(query = query, source = "knowledge", timestamp = System.currentTimeMillis())
                )
            }

            applyFilters()
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(data = it.data.copy(selectedCategory = category)) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value.data
        var filtered = state.articles

        if (state.selectedCategory != "All") {
            filtered = filtered.filter { it.category == state.selectedCategory }
        }

        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter { article ->
                article.title.lowercase().contains(query) ||
                article.content.lowercase().contains(query) ||
                article.category.lowercase().contains(query)
            }
        }

        _uiState.update { it.copy(data = state.copy(filteredArticles = filtered)) }
    }

    fun toggleFavorite(articleId: String) {
        val updated = _uiState.value.data.articles.map { article ->
            if (article.id == articleId) article.copy(isFavorite = !article.isFavorite) else article
        }
        _uiState.update {
            it.copy(data = it.data.copy(articles = updated))
        }
        applyFilters()
    }
}
