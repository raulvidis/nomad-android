package com.nomad.android.data.maps

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch

/**
 * Regression coverage for the silent-missing-tiles bug in `downloadRegion`.
 *
 * Root cause: the downloading region's [MBTilesDatabase] handle is captured once
 * and used for the whole tile loop *outside* the manager lock. The `databases`
 * map is an access-order LRU (`MAX_OPEN_DATABASES == 5`) whose `removeEldestEntry`
 * `close()`s evicted handles. If another caller opens enough other region handles
 * mid-download, the in-use handle is evicted and closed; afterwards `insertTile`
 * is a silent no-op (`db?.compileStatement(...)` on a nulled handle) and the flow
 * still emits `isComplete = true` with zero stored tiles.
 *
 * Fix under test: pin the active region's key so `removeEldestEntry` keeps it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class OfflineTileManagerTest {

    private lateinit var context: Context
    private lateinit var tilesDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tilesDir = File(context.filesDir, "tiles").also { it.mkdirs() }
    }

    @After
    fun tearDown() {
        tilesDir.deleteRecursively()
    }

    @Test
    fun downloadRegion_pinsActiveHandle_soLruEvictionDoesNotDropTiles() = runBlocking {
        val tiles = listOf(
            TileCalculator.TileCoord(0, 0, 0),
            TileCalculator.TileCoord(0, 0, 1),
            TileCalculator.TileCoord(1, 0, 1)
        )
        val tileCalc = mock<TileCalculator>().apply {
            whenever(getTilesForBounds(any(), any(), any(), any(), any(), any())).thenReturn(tiles)
            whenever(tileUrl(any(), any(), any())).thenReturn("https://example/tile")
        }

        // The mocked HTTP body is gated: downloads block until the collector has
        // triggered eviction, guaranteeing every insertTile() runs AFTER the
        // in-use handle has been (attempted to be) evicted. This removes the race
        // that `flowOn(Dispatchers.IO)` would otherwise introduce.
        val evictionGate = CountDownLatch(1)
        val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            evictionGate.await()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("ok")
                .body(byteArrayOf(1, 2, 3, 4).toResponseBody("image/png".toMediaType()))
                .build()
        }.build()

        val manager = OfflineTileManager(tilesDir, tileCalc, httpClient)

        val regionA = manager.createRegion("region-a", 1.0, 0.0, 1.0, 0.0, 0, 1)

        var triggeredEviction = false
        manager.downloadRegion(regionA, 1.0, 0.0, 1.0, 0.0, 0, 1).collect { _ ->
            if (!triggeredEviction) {
                triggeredEviction = true
                // Simulate the Manage Maps screen (or any caller) opening other
                // region handles mid-download. Five extra handles + regionA = 6 >
                // MAX_OPEN_DATABASES, so the LRU overflows; regionA is the eldest
                // entry and would be evicted + closed by the buggy build.
                repeat(5) { i ->
                    manager.createRegion("filler-$i", 1.0, 0.0, 1.0, 0.0, 0, 1)
                }
                evictionGate.countDown()
            }
        }

        // Buggy build: regionA's handle was closed mid-download, so every
        // insertTile was a silent no-op -> 0 stored tiles. Fixed build: the
        // handle is pinned and all 3 tiles land.
        assertEquals(3, manager.getStoredTileCount(regionA))
    }
}
