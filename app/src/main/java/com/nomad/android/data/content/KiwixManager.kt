package com.nomad.android.data.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
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

    private fun getZimDir(): File = File(context.filesDir, "zim").also { it.mkdirs() }

    fun getLoadedArchives(): List<String> {
        return getZimDir().listFiles()?.filter { it.extension == "zim" }?.map { it.name } ?: emptyList()
    }

    fun getArchiveSize(name: String): Long {
        return File(getZimDir(), name).length()
    }

    suspend fun searchArticles(query: String, archiveName: String? = null): Flow<List<ZimSearchResult>> = flow {
        // TODO: Implement with libkiwix JNI
        // val archive = zimArchiveCache.getOrPut(archiveName) { Archive(File(getZimDir(), archiveName).absolutePath) }
        // val searcher = Searcher().apply { addArchive(archive) }
        // val search = searcher.search(query)
        // emit(search.results.map { ZimSearchResult(it.path, it.title, it.snippet) })
        emit(mockSearchResults(query))
    }

    suspend fun getArticle(path: String, archiveName: String): Result<ZimArticle> = withContext(Dispatchers.IO) {
        // TODO: Implement with libkiwix JNI
        // val archive = Archive(File(getZimDir(), archiveName).absolutePath)
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
        // TODO: Implement with WorkManager + OkHttp
        val targetFile = File(getZimDir(), name)
        emit(0f)
        // Download logic here
        emit(1f)
    }

    fun getAvailableZimPacks(): List<ContentPack> = listOf(
        ContentPack("wiki_mini_en", "Wikipedia Mini (English)", "wikipedia", 536870912L, "Top 100 articles with images", PackStatus.AVAILABLE),
        ContentPack("wiki_nopic_en", "Wikipedia No-Images (English)", "wikipedia", 10737418240L, "All articles, no images", PackStatus.AVAILABLE),
        ContentPack("wiki_all_en", "Wikipedia Full (English)", "wikipedia", 107374182400L, "All articles with images", PackStatus.AVAILABLE),
        ContentPack("gutenberg_top", "Project Gutenberg Top 100", "books", 1073741824L, "Classic literature", PackStatus.AVAILABLE),
        ContentPack("wikimed_medical", "Medical Encyclopedia", "wikipedia", 2147483648L, "Medical reference articles", PackStatus.AVAILABLE),
        ContentPack("stackexchange_survival", "Survival Stack Exchange", "wikipedia", 536870912L, "Community Q&A", PackStatus.AVAILABLE),
    )

    private fun mockSearchResults(query: String): List<ZimSearchResult> = listOf(
        ZimSearchResult("A/Water", "Water", "Water is essential for survival. The human body can survive only 3 days without water..."),
        ZimSearchResult("A/Water_purification", "Water purification", "Water purification is the process of removing undesirable chemicals..."),
        ZimSearchResult("A/Solar_still", "Solar still", "A solar still is a device that uses solar energy to distill water..."),
    ).filter { it.title.contains(query, ignoreCase = true) || it.snippet.contains(query, ignoreCase = true) }
        .ifEmpty { listOf(ZimSearchResult("A/Search", "Search: $query", "No exact matches found. Try broader terms.")) }
}
