package com.nomad.android.data.repository

import android.content.Context
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.entity.ContentPackEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ContentPackRepositoryTest {

    @Mock
    private lateinit var contentPackDao: ContentPackDao

    @Mock
    private lateinit var context: Context

    private lateinit var okHttpClient: OkHttpClient
    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: ContentPackRepository

    private val testPack = ContentPackEntity(
        id = "pack-1",
        name = "Test Pack",
        type = "wikipedia",
        sizeBytes = 1024L,
        status = "downloaded",
        downloadedAt = 1000L,
        version = "1.0",
        description = "A test pack"
    )

    private val testPack2 = ContentPackEntity(
        id = "pack-2",
        name = "Maps Pack",
        type = "maps",
        sizeBytes = 2048L,
        status = "pending",
        downloadedAt = null,
        version = "2.0",
        description = "A maps pack"
    )

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        okHttpClient = OkHttpClient.Builder().build()

        // Set up mock context to return a temp dir for filesDir
        val tempDir = createTempDir()
        `when`(context.filesDir).thenReturn(tempDir)

        repository = ContentPackRepository(contentPackDao, context, okHttpClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // --- getAllPacks ---

    @Test
    fun `getAllPacks returns success with packs from DAO`() = runTest {
        val packs = listOf(testPack, testPack2)
        `when`(contentPackDao.getAll()).thenReturn(flow { emit(packs) })

        val results = repository.getAllPacks().toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals(packs, results[0].getOrNull())
    }

    @Test
    fun `getAllPacks returns error when DAO throws inside flow`() = runTest {
        `when`(contentPackDao.getAll()).thenReturn(flow {
            throw RuntimeException("DB error")
        })

        val results = repository.getAllPacks().toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isError)
        assertTrue(results[0].exceptionOrNull() is RuntimeException)
    }

    // --- getPacksByType ---

    @Test
    fun `getPacksByType returns filtered packs`() = runTest {
        val typePacks = listOf(testPack)
        `when`(contentPackDao.getByType("wikipedia")).thenReturn(flow { emit(typePacks) })

        val results = repository.getPacksByType("wikipedia").toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals(typePacks, results[0].getOrNull())
    }

    @Test
    fun `getPacksByType returns empty for non-existent type`() = runTest {
        `when`(contentPackDao.getByType("nonexistent")).thenReturn(flow { emit(emptyList()) })

        val results = repository.getPacksByType("nonexistent").toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals(emptyList<ContentPackEntity>(), results[0].getOrNull())
    }

    // --- getPacksByStatus ---

    @Test
    fun `getPacksByStatus returns filtered packs`() = runTest {
        val statusPacks = listOf(testPack2)
        `when`(contentPackDao.getByStatus("pending")).thenReturn(flow { emit(statusPacks) })

        val results = repository.getPacksByStatus("pending").toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals(statusPacks, results[0].getOrNull())
    }

    @Test
    fun `getPacksByStatus returns error when DAO throws inside flow`() = runTest {
        `when`(contentPackDao.getByStatus("invalid")).thenReturn(flow {
            throw RuntimeException("DB error")
        })

        val results = repository.getPacksByStatus("invalid").toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isError)
    }

    // --- insertPack ---

    @Test
    fun `insertPack returns success`() = runTest {
        repository.insertPack(testPack)

        verify(contentPackDao).insert(testPack)
    }

    @Test
    fun `insertPack returns error when DAO throws`() = runTest {
        `when`(contentPackDao.insert(testPack)).thenThrow(RuntimeException("Insert failed"))

        val result = repository.insertPack(testPack)

        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    // --- updatePack ---

    @Test
    fun `updatePack returns success`() = runTest {
        repository.updatePack(testPack)

        verify(contentPackDao).update(testPack)
    }

    @Test
    fun `updatePack returns error when DAO throws`() = runTest {
        `when`(contentPackDao.update(testPack)).thenThrow(RuntimeException("Update failed"))

        val result = repository.updatePack(testPack)

        assertTrue(result.isError)
    }

    // --- deletePack ---

    @Test
    fun `deletePack returns success`() = runTest {
        repository.deletePack(testPack)

        verify(contentPackDao).delete(testPack)
    }

    @Test
    fun `deletePack returns error when DAO throws`() = runTest {
        `when`(contentPackDao.delete(testPack)).thenThrow(RuntimeException("Delete failed"))

        val result = repository.deletePack(testPack)

        assertTrue(result.isError)
    }

    // --- downloadPack ---

    @Test
    fun `downloadPack emits progress and completes successfully`() = runTest {
        val bodyBytes = ByteArray(1024) { it.toByte() }
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(okio.Buffer().write(bodyBytes))
                .addHeader("Content-Length", bodyBytes.size.toLong())
        )

        val url = mockWebServer.url("/packs/${testPack.id}.zip").toString()
        val progressValues = repository.downloadPack(testPack, url).toList()

        // Should emit at least 0f and 1f
        assertTrue("Expected at least 2 emissions, got ${progressValues.size}", progressValues.size >= 2)
        assertEquals(0f, progressValues.first(), 0.01f)
        assertEquals(1f, progressValues.last(), 0.01f)
        // All progress values should be between 0 and 1 inclusive
        progressValues.forEach { progress ->
            assertTrue(progress in 0f..1f)
        }
    }

    @Test
    fun `downloadPack throws on non-success HTTP response`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val url = mockWebServer.url("/packs/nonexistent.zip").toString()

        // downloadPack is a Flow<Float>, the exception occurs when collecting
        try {
            repository.downloadPack(testPack, url).toList()
            fail("Expected an exception to be thrown")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("Download failed") == true)
        }
    }

    @Test
    fun `downloadPack throws on empty response body`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Length", 0)
        )

        val url = mockWebServer.url("/packs/empty.zip").toString()

        try {
            repository.downloadPack(testPack, url).toList()
            fail("Expected an exception to be thrown")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("Empty response body") == true)
        }
    }
}
