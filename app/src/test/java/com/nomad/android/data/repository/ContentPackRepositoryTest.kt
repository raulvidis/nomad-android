package com.nomad.android.data.repository

import android.content.Context
import app.cash.turbine.test
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.entity.ContentPackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ContentPackRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val tempFolder = TemporaryFolder()
    private lateinit var mockContext: Context
    private lateinit var contentPacksDir: File
    private lateinit var dao: FakeContentPackDao
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tempFolder.create()
        contentPacksDir = File(tempFolder.root, "contentPacks")
        contentPacksDir.mkdirs()

        mockContext = mock()
        whenever(mockContext.filesDir).thenReturn(tempFolder.root)

        dao = FakeContentPackDao()
        okHttpClient = OkHttpClient()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempFolder.delete()
    }

    private fun createRepo() = ContentPackRepository(dao, mockContext, okHttpClient)

    // --- getAllPacks ---

    @Test
    fun `getAllPacks returns success with packs from DAO`() = runTest {
        dao.packs = listOf(
            testPack("pack1", type = "survival", status = "downloaded"),
            testPack("pack2", type = "maps", status = "available")
        )
        val repo = createRepo()

        repo.getAllPacks().test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrNull()?.size)
            assertEquals("pack1", result.getOrNull()?.get(0)?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllPacks returns error when DAO throws`() = runTest {
        dao = FakeContentPackDao(shouldFail = true)
        val repo = createRepo()

        repo.getAllPacks().test {
            val result = awaitItem()
            assertTrue(result.isError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getPacksByType ---

    @Test
    fun `getPacksByType filters by type`() = runTest {
        dao.packs = listOf(
            testPack("pack1", type = "survival"),
            testPack("pack2", type = "maps")
        )
        val repo = createRepo()

        repo.getPacksByType("survival").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val filtered = result.getOrNull()
            assertEquals(1, filtered?.size)
            assertEquals("survival", filtered?.get(0)?.type)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getPacksByStatus ---

    @Test
    fun `getPacksByStatus filters by status`() = runTest {
        dao.packs = listOf(
            testPack("pack1", status = "downloaded"),
            testPack("pack2", status = "available")
        )
        val repo = createRepo()

        repo.getPacksByStatus("downloaded").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val filtered = result.getOrNull()
            assertEquals(1, filtered?.size)
            assertEquals("downloaded", filtered?.get(0)?.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- insertPack ---

    @Test
    fun `insertPack succeeds and delegates to DAO`() = runTest {
        val repo = createRepo()
        val pack = testPack("pack1")

        val result = repo.insertPack(pack)
        assertTrue(result.isSuccess)
        assertEquals(pack, dao.insertedPacks.first())
    }

    @Test
    fun `insertPack returns error when DAO throws`() = runTest {
        dao = FakeContentPackDao(shouldFail = true)
        val repo = createRepo()

        val result = repo.insertPack(testPack("pack1"))
        assertTrue(result.isError)
    }

    // --- updatePack ---

    @Test
    fun `updatePack succeeds and delegates to DAO`() = runTest {
        val repo = createRepo()
        val pack = testPack("pack1", name = "Updated")

        val result = repo.updatePack(pack)
        assertTrue(result.isSuccess)
        assertEquals(pack, dao.updatedPacks.first())
    }

    // --- deletePack ---

    @Test
    fun `deletePack removes file and DAO entry`() = runTest {
        val packFile = File(contentPacksDir, "pack1")
        packFile.createNewFile()
        assertTrue(packFile.exists())

        val repo = createRepo()
        val pack = testPack("pack1")

        val result = repo.deletePack(pack)
        assertTrue(result.isSuccess)
        assertFalse("File should be deleted", packFile.exists())
        assertEquals(pack, dao.deletedPacks.first())
    }

    @Test
    fun `deletePack does not crash if file doesn't exist`() = runTest {
        val repo = createRepo()

        val result = repo.deletePack(testPack("nonexistent"))
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deletePack returns error when DAO throws`() = runTest {
        dao = FakeContentPackDao(shouldFail = true)
        File(contentPacksDir, "pack1").createNewFile()

        val repo = createRepo()
        val result = repo.deletePack(testPack("pack1"))
        assertTrue(result.isError)
    }

    // --- deletePackById ---

    @Test
    fun `deletePackById removes file and DAO entry`() = runTest {
        val packFile = File(contentPacksDir, "pack1")
        packFile.createNewFile()

        val repo = createRepo()
        val result = repo.deletePackById("pack1")
        assertTrue(result.isSuccess)
        assertFalse(packFile.exists())
        assertEquals("pack1", dao.deletedByIds.first())
    }

    // --- getPackFile ---

    @Test
    fun `getPackFile returns file when it exists`() {
        File(contentPacksDir, "pack1").createNewFile()
        val repo = createRepo()

        val result = repo.getPackFile("pack1")
        assertNotNull(result)
        assertEquals("pack1", result?.name)
    }

    @Test
    fun `getPackFile returns null when not found`() {
        val repo = createRepo()
        assertNull(repo.getPackFile("nonexistent"))
    }

    // --- downloadPack ---

    @Test
    fun `downloadPack emits progress and creates file`() = runTest {
        val server = MockWebServer()
        server.start()
        val bodyBytes = ByteArray(256) { it.toByte() }
        server.enqueue(
            MockResponse()
                .setBody(Buffer().write(bodyBytes))
                .setHeader("Content-Length", bodyBytes.size.toLong())
        )

        try {
            val url = server.url("/test-pack").toString()
            val pack = testPack("test-pack")
            val repo = createRepo()

            repo.downloadPack(pack, url).test {
                assertEquals(0f, awaitItem(), 0.01f)
                cancelAndConsumeRemainingEvents()
                val downloadedFile = File(contentPacksDir, "test-pack")
                assertTrue("Downloaded file should exist", downloadedFile.exists())
                assertTrue("File should have content", downloadedFile.length() > 0)
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `downloadPack throws on HTTP error`() = runTest {
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setResponseCode(500))

        try {
            val url = server.url("/bad-pack").toString()
            val pack = testPack("bad-pack")
            val repo = createRepo()

            repo.downloadPack(pack, url).test {
                awaitItem() // 0f
                val error = awaitError()
                assertTrue(
                    "Expected 'Download failed', got: ${error.message}",
                    error.message?.contains("Download failed") == true
                )
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `downloadPack cleans up tmp file on failure`() = runTest {
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setResponseCode(403))

        try {
            val url = server.url("/forbidden").toString()
            val pack = testPack("fail-pack")
            val repo = createRepo()

            repo.downloadPack(pack, url).test {
                awaitItem()
                awaitError()
                val tmpFiles = contentPacksDir.listFiles()?.filter { it.name.endsWith(".tmp") }
                assertTrue("No tmp files should remain after failure", tmpFiles.isNullOrEmpty())
            }
        } finally {
            server.shutdown()
        }
    }

    // --- Helpers ---

    private fun testPack(
        id: String,
        name: String = "Test Pack",
        type: String = "survival",
        status: String = "available",
        sizeBytes: Long = 1000L
    ) = ContentPackEntity(
        id = id,
        name = name,
        type = type,
        sizeBytes = sizeBytes,
        status = status,
        downloadedAt = if (status == "downloaded") 1000L else null,
        version = "1.0",
        description = "Test pack $id"
    )
}

class FakeContentPackDao(
    private val shouldFail: Boolean = false
) : ContentPackDao {

    var packs: List<ContentPackEntity> = emptyList()
    val insertedPacks = mutableListOf<ContentPackEntity>()
    val updatedPacks = mutableListOf<ContentPackEntity>()
    val deletedPacks = mutableListOf<ContentPackEntity>()
    val deletedByIds = mutableListOf<String>()

    private fun failIfNeeded() {
        if (shouldFail) throw RuntimeException("DB error")
    }

    override fun getAll() =
        if (shouldFail) throw RuntimeException("DB error")
        else flowOf(packs)

    override fun getByType(type: String) =
        if (shouldFail) throw RuntimeException("DB error")
        else flowOf(packs.filter { it.type == type })

    override fun getByStatus(status: String) =
        if (shouldFail) throw RuntimeException("DB error")
        else flowOf(packs.filter { it.status == status })

    override suspend fun insert(contentPack: ContentPackEntity) {
        failIfNeeded()
        insertedPacks.add(contentPack)
    }

    override suspend fun insertAll(contentPacks: List<ContentPackEntity>) {
        failIfNeeded()
        insertedPacks.addAll(contentPacks)
    }

    override suspend fun update(contentPack: ContentPackEntity) {
        failIfNeeded()
        updatedPacks.add(contentPack)
    }

    override suspend fun delete(contentPack: ContentPackEntity) {
        failIfNeeded()
        deletedPacks.add(contentPack)
    }

    override suspend fun deleteById(id: String) {
        failIfNeeded()
        deletedByIds.add(id)
    }
}
