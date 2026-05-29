package com.nomad.android.data.content

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.entity.ContentPackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ContentPackManagerTest {

    private lateinit var context: Context
    private lateinit var modelsDir: File
    private lateinit var contentPacksDir: File
    private lateinit var manager: ContentPackManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        modelsDir = File(context.filesDir, "models").also { it.mkdirs() }
        contentPacksDir = File(context.filesDir, "contentPacks").also { it.mkdirs() }
        manager = ContentPackManager(context, FakeContentPackDao(), OkHttpClient())
    }

    @After
    fun tearDown() {
        modelsDir.deleteRecursively()
        contentPacksDir.deleteRecursively()
    }

    @Test
    fun `isPackDownloaded returns false when model file does not exist`() {
        assertFalse(manager.isPackDownloaded("ai_minicpm5"))
    }

    @Test
    fun `isPackDownloaded returns false when model file is too small`() {
        val modelFile = File(modelsDir, "MiniCPM5-1B-Q4_K_M.gguf")
        modelFile.writeText("tiny") // 4 bytes — way below 1MB threshold
        assertFalse(manager.isPackDownloaded("ai_minicpm5"))
    }

    @Test
    fun `isPackDownloaded returns false when model file is exactly 1MB`() {
        val modelFile = File(modelsDir, "MiniCPM5-1B-Q4_K_M.gguf")
        modelFile.writeBytes(ByteArray(1_000_000)) // Exactly 1MB — NOT > 1MB
        assertFalse(manager.isPackDownloaded("ai_minicpm5"))
    }

    @Test
    fun `isPackDownloaded returns true when model file exceeds 1MB`() {
        val modelFile = File(modelsDir, "MiniCPM5-1B-Q4_K_M.gguf")
        modelFile.writeBytes(ByteArray(1_000_001)) // Just over 1MB
        assertTrue(manager.isPackDownloaded("ai_minicpm5"))
    }

    @Test
    fun `isPackDownloaded returns true for non-AI pack when file exists`() {
        val packFile = File(contentPacksDir, "wiki_mini")
        packFile.writeText("content")
        assertTrue(manager.isPackDownloaded("wiki_mini"))
    }

    @Test
    fun `isPackDownloaded returns false for non-AI pack when file does not exist`() {
        assertFalse(manager.isPackDownloaded("nonexistent_pack"))
    }

    private class FakeContentPackDao : ContentPackDao {
        override fun getAll(): Flow<List<ContentPackEntity>> = flowOf(emptyList())
        override fun getByType(type: String): Flow<List<ContentPackEntity>> = flowOf(emptyList())
        override fun getByStatus(status: String): Flow<List<ContentPackEntity>> = flowOf(emptyList())
        override suspend fun insert(contentPack: ContentPackEntity) {}
        override suspend fun insertAll(contentPacks: List<ContentPackEntity>) {}
        override suspend fun update(contentPack: ContentPackEntity) {}
        override suspend fun delete(contentPack: ContentPackEntity) {}
        override suspend fun deleteById(id: String) {}
    }
}
