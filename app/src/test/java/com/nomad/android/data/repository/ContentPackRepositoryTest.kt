package com.nomad.android.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.entity.ContentPackEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ContentPackRepositoryTest {

    private lateinit var fakeDao: FakeContentPackDao
    private lateinit var context: Context
    private lateinit var contentPacksDir: File
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: ContentPackRepository

    private val testPack = ContentPackEntity(
        id = "pack-1", name = "Test Pack", type = "wikipedia",
        sizeBytes = 1024L, status = "downloaded", downloadedAt = 1000L,
        version = "1.0", description = "A test pack"
    )

    private val testPack2 = ContentPackEntity(
        id = "pack-2", name = "Maps Pack", type = "maps",
        sizeBytes = 2048L, status = "pending", downloadedAt = null,
        version = "2.0", description = "A maps pack"
    )

    @Before
    fun setUp() {
        fakeDao = FakeContentPackDao()
        context = ApplicationProvider.getApplicationContext()
        contentPacksDir = File(context.filesDir, "contentPacks").also { it.mkdirs() }
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // The repo enforces HTTPS scheme on download URLs, so we need the mock server
        // to serve over HTTPS. Use a trust-all client to accept the self-signed cert.
        val trustAllManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustAllManager), java.security.SecureRandom())
        }
        // Tell MockWebServer to use HTTPS
        mockWebServer.useHttps(sslContext.socketFactory, false)

        okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .build()
        repository = ContentPackRepository(fakeDao, context, okHttpClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        contentPacksDir.deleteRecursively()
    }

    // --- getAllPacks ---

    @Test
    fun `getAllPacks returns success with packs`() = runTest {
        fakeDao.packs = listOf(testPack, testPack2)
        val results = repository.getAllPacks().toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals(listOf(testPack, testPack2), results[0].getOrNull())
    }

    @Test
    fun `getAllPacks returns error when DAO throws`() = runTest {
        fakeDao.shouldFail = true
        val results = repository.getAllPacks().toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isError)
    }

    // --- getPacksByType ---

    @Test
    fun `getPacksByType returns filtered packs`() = runTest {
        fakeDao.packs = listOf(testPack, testPack2)
        val results = repository.getPacksByType("wikipedia").toList()
        assertTrue(results[0].isSuccess)
        assertEquals(listOf(testPack), results[0].getOrNull())
    }

    @Test
    fun `getPacksByType returns empty for non-existent type`() = runTest {
        fakeDao.packs = listOf(testPack, testPack2)
        val results = repository.getPacksByType("nonexistent").toList()
        assertTrue(results[0].isSuccess)
        assertEquals(emptyList<ContentPackEntity>(), results[0].getOrNull())
    }

    // --- getPacksByStatus ---

    @Test
    fun `getPacksByStatus returns filtered packs`() = runTest {
        fakeDao.packs = listOf(testPack, testPack2)
        val results = repository.getPacksByStatus("pending").toList()
        assertTrue(results[0].isSuccess)
        assertEquals(listOf(testPack2), results[0].getOrNull())
    }

    @Test
    fun `getPacksByStatus returns error when DAO throws`() = runTest {
        fakeDao.shouldFail = true
        val results = repository.getPacksByStatus("invalid").toList()
        assertTrue(results[0].isError)
    }

    // --- insertPack ---

    @Test
    fun `insertPack returns success`() = runTest {
        val result = repository.insertPack(testPack)
        assertTrue(result.isSuccess)
        assertEquals(listOf(testPack), fakeDao.insertedPacks)
    }

    @Test
    fun `insertPack returns error when DAO throws`() = runTest {
        fakeDao.shouldFailSuspend = true
        val result = repository.insertPack(testPack)
        assertTrue(result.isError)
    }

    // --- updatePack ---

    @Test
    fun `updatePack returns success`() = runTest {
        val result = repository.updatePack(testPack)
        assertTrue(result.isSuccess)
        assertEquals(listOf(testPack), fakeDao.updatedPacks)
    }

    @Test
    fun `updatePack returns error when DAO throws`() = runTest {
        fakeDao.shouldFailSuspend = true
        val result = repository.updatePack(testPack)
        assertTrue(result.isError)
    }

    // --- deletePack ---

    @Test
    fun `deletePack returns success and deletes file`() = runTest {
        val packFile = File(contentPacksDir, "pack-1")
        packFile.createNewFile()
        val result = repository.deletePack(testPack)
        assertTrue(result.isSuccess)
        assertFalse(packFile.exists())
    }

    @Test
    fun `deletePack returns error when DAO throws`() = runTest {
        fakeDao.shouldFailSuspend = true
        val result = repository.deletePack(testPack)
        assertTrue(result.isError)
    }

    // --- downloadPack ---

    @Test
    fun `downloadPack emits progress and creates file`() = runTest {
        val bodyBytes = ByteArray(2048) { it.toByte() }
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(okio.Buffer().write(bodyBytes))
                .setHeader("Content-Length", bodyBytes.size.toLong())
        )
        val url = mockWebServer.url("/pack.zip").toString()
        val emissions = repository.downloadPack(testPack, url).toList()

        assertEquals(0f, emissions.first(), 0.01f)
        assertEquals(1f, emissions.last(), 0.01f)
        val downloadedFile = File(contentPacksDir, "pack-1")
        assertTrue("Downloaded file should exist", downloadedFile.exists())
        assertEquals(bodyBytes.size.toLong(), downloadedFile.length())
    }

    @Test
    fun `downloadPack throws on HTTP error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        val url = mockWebServer.url("/pack.zip").toString()
        try {
            repository.downloadPack(testPack, url).toList()
            fail("Should have thrown")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("Download failed") == true)
        }
    }

    // --- Fake DAO ---

    class FakeContentPackDao : ContentPackDao {
        var packs: List<ContentPackEntity> = emptyList()
        var shouldFail = false
        var shouldFailSuspend = false
        val insertedPacks = mutableListOf<ContentPackEntity>()
        val updatedPacks = mutableListOf<ContentPackEntity>()

        override fun getAll(): Flow<List<ContentPackEntity>> =
            if (shouldFail) flow { throw RuntimeException("DB error") } else flowOf(packs)

        override fun getByType(type: String): Flow<List<ContentPackEntity>> =
            if (shouldFail) flow { throw RuntimeException("DB error") } else flowOf(packs.filter { it.type == type })

        override fun getByStatus(status: String): Flow<List<ContentPackEntity>> =
            if (shouldFail) flow { throw RuntimeException("DB error") } else flowOf(packs.filter { it.status == status })

        override suspend fun insert(contentPack: ContentPackEntity) {
            if (shouldFailSuspend) throw RuntimeException("Insert failed")
            insertedPacks.add(contentPack)
        }

        override suspend fun insertAll(contentPacks: List<ContentPackEntity>) {
            if (shouldFailSuspend) throw RuntimeException("Insert all failed")
            insertedPacks.addAll(contentPacks)
        }

        override suspend fun update(contentPack: ContentPackEntity) {
            if (shouldFailSuspend) throw RuntimeException("Update failed")
            updatedPacks.add(contentPack)
        }

        override suspend fun delete(contentPack: ContentPackEntity) {
            if (shouldFailSuspend) throw RuntimeException("Delete failed")
        }

        override suspend fun deleteById(id: String) {
            if (shouldFailSuspend) throw RuntimeException("Delete by id failed")
        }
    }
}
