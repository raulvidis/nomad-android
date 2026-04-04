package com.nomad.android.data.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

data class ZimArticle(
    val path: String,
    val title: String,
    val content: String,
    val mimeType: String
)

data class ZimSearchResult(
    val path: String,
    val title: String,
    val snippet: String
)

class KiwixManager(private val context: Context) {

    private val zimDir by lazy { File(context.filesDir, "zim").also { it.mkdirs() } }

    fun getLoadedArchives(): List<String> {
        return zimDir.listFiles()?.filter { it.extension == "zim" }?.map { it.name } ?: emptyList()
    }

    fun getArchiveSize(name: String): Long {
        return File(zimDir, name).length()
    }

    fun hasArchive(name: String): Boolean = File(zimDir, name).exists()

    suspend fun searchArticles(query: String, archiveName: String? = null): Flow<List<ZimSearchResult>> = flow {
        // Try searching actual ZIM archives if available
        val archives = archiveName?.let { listOf(File(zimDir, it)) }
            ?: (zimDir.listFiles()?.filter { it.extension == "zim" } ?: emptyList())

        if (archives.isNotEmpty()) {
            // TODO: Implement with libkiwix JNI
            // val searcher = Searcher()
            // archives.forEach { searcher.addArchive(Archive(it.absolutePath)) }
            // val results = searcher.search(query)
            // emit(results.map { ZimSearchResult(it.path, it.title, it.snippet) })
            emit(mockSearchResults(query))
        } else {
            // Fall back to bundled JSON content search
            val bundledResults = searchBundledContent(query)
            emit(bundledResults)
        }
    }

    suspend fun getArticle(path: String, archiveName: String): Result<ZimArticle> = withContext(Dispatchers.IO) {
        val archiveFile = File(zimDir, archiveName)
        if (!archiveFile.exists()) {
            return@withContext Result.failure(IllegalStateException("Archive not found: $archiveName"))
        }

        // TODO: Implement with libkiwix JNI
        // val archive = Archive(archiveFile.absolutePath)
        // val entry = archive.getEntryByPath(path)
        // val content = entry.getItem(true).data.data.let { String(it, Charsets.UTF_8) }
        Result.success(
            ZimArticle(
                path = path,
                title = "Article: $path",
                content = "<html><body><h1>$path</h1><p>Full article content will be loaded from ZIM archive via libkiwix JNI.</p></body></html>",
                mimeType = "text/html"
            )
        )
    }

    suspend fun downloadArchive(url: String, name: String): Flow<Float> = flow {
        val targetFile = File(zimDir, name)
        if (targetFile.exists()) {
            emit(1f)
            return@flow
        }

        emit(0f)
        // TODO: Implement with WorkManager + OkHttp for large ZIM downloads
        // For now, simulate progress
        val steps = 20
        for (i in 1..steps) {
            delay(200)
            emit(i.toFloat() / steps)
        }
        emit(1f)
    }

    fun deleteArchive(name: String): Boolean {
        val file = File(zimDir, name)
        return if (file.exists()) file.delete() else false
    }

    fun getAvailableZimPacks(): List<ContentPack> = listOf(
        ContentPack("wiki_mini_en", "Wikipedia Mini (English)", "wikipedia", 536870912L, "Top 100 articles with images", PackStatus.AVAILABLE),
        ContentPack("wiki_nopic_en", "Wikipedia No-Images (English)", "wikipedia", 10737418240L, "All articles, no images", PackStatus.AVAILABLE),
        ContentPack("wiki_all_en", "Wikipedia Full (English)", "wikipedia", 107374182400L, "All articles with images", PackStatus.AVAILABLE),
        ContentPack("gutenberg_top", "Project Gutenberg Top 100", "books", 1073741824L, "Classic literature", PackStatus.AVAILABLE),
        ContentPack("wikimed_medical", "Medical Encyclopedia", "wikipedia", 2147483648L, "Medical reference articles", PackStatus.AVAILABLE),
        ContentPack("stackexchange_survival", "Survival Stack Exchange", "wikipedia", 536870912L, "Community Q&A", PackStatus.AVAILABLE),
    )

    private fun searchBundledContent(query: String): List<ZimSearchResult> {
        val lowercaseQuery = query.lowercase()
        return bundledKnowledge
            .filter { it.title.lowercase().contains(lowercaseQuery) || it.content.lowercase().contains(lowercaseQuery) }
            .map { ZimSearchResult("A/${it.title}", it.title, it.content.take(100) + "...") }
            .ifEmpty { listOf(ZimSearchResult("A/Search", "Search: $query", "No matches in bundled content. Try downloading knowledge packs in Settings.")) }
    }

    private fun mockSearchResults(query: String): List<ZimSearchResult> = listOf(
        ZimSearchResult("A/Water", "Water", "Water is essential for survival. The human body can survive only 3 days without water..."),
        ZimSearchResult("A/Water_purification", "Water purification", "Water purification is the process of removing undesirable chemicals..."),
        ZimSearchResult("A/Solar_still", "Solar still", "A solar still is a device that uses solar energy to distill water..."),
    ).filter { it.title.contains(query, ignoreCase = true) || it.snippet.contains(query, ignoreCase = true) }
        .ifEmpty { listOf(ZimSearchResult("A/Search", "Search: $query", "No exact matches found. Try broader terms.")) }

    companion object {
        private val bundledKnowledge = listOf(
            "CPR Basics" to "To perform CPR: Check responsiveness, call emergency services, push hard and fast in the center of the chest at 100-120 compressions per minute, give rescue breaths if trained.",
            "Water Purification" to "Boil water for at least 1 minute (3 minutes above 6,500 ft). Use purification tablets or chlorine dioxide drops. Solar disinfection: clear bottle in direct sunlight for 6 hours.",
            "Fire Starting" to "Gather tinder, kindling, and fuel. Create a fire lay. Use matches, lighter, or friction method. Shield from wind. Never leave unattended.",
            "Shelter Building" to "Find natural windbreaks. Use branches and leaves for insulation. Build a lean-to or debris hut. Insulate the ground. Keep shelter small to retain body heat.",
            "Navigation" to "Sun rises in east, sets in west. North Star (Polaris) indicates north. Follow waterways downstream to civilization. Note landmarks.",
            "Edible Plants" to "Only eat plants you can positively identify. Safe bets: dandelion, clover, cattail, pine needles (tea). Avoid: milky sap, umbrella-shaped flowers, almond scent.",
            "SOS Signals" to "Three of anything (fires, whistle blasts, flashes). Ground-to-air signals: use contrasting materials, minimum 10ft tall. Mirror signaling: flash toward aircraft.",
            "First Aid" to "Stop bleeding first. Treat for shock (lay down, elevate legs, keep warm). Splint fractures with rigid materials. Burns: cool with water, cover with clean dressing.",
            "Knots" to "Essential knots: Bowline (secure loop), Clove hitch (quick attachment), Square knot (joining ropes), Taut-line hitch (adjustable tension), Figure-eight (stopper knot)."
        ).map { (title, content) ->
            object {
                val title = title
                val content = content
            }
        }
    }
}
