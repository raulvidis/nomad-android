package com.nomad.android.data.content

import android.content.Context
import com.nomad.android.data.Result
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

    fun searchArticles(query: String, archiveName: String? = null): Flow<List<ZimSearchResult>> = flow {
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
            return@withContext Result.error("Archive not found: $archiveName")
        }

        Result.success(
            ZimArticle(
                path = path,
                title = "Article: $path",
                content = "<html><body><h1>$path</h1><p>Full article content will be loaded from ZIM archive via libkiwix JNI.</p></body></html>",
                mimeType = "text/html"
            )
        )
    }

    fun downloadArchive(url: String, name: String): Flow<Float> = flow {
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

    private fun searchBundledContent(query: String): List<ZimSearchResult> =
        BundledContentSearch.search(query)

    private fun mockSearchResults(query: String): List<ZimSearchResult> =
        ZimMockSearch.search(query)

    companion object
}
