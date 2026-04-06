package com.nomad.android.data.maps

import org.junit.Assert.*
import org.junit.Test

class TileCalculatorTest {

    @Test
    fun latLngToTile_at_zoom_0_returns_0_0() {
        val (x, y) = TileCalculator.latLngToTile(0.0, 0.0, 0)
        assertEquals(0, x)
        assertEquals(0, y)
    }

    @Test
    fun latLngToTile_at_zoom_1_for_known_location() {
        val (x, y) = TileCalculator.latLngToTile(48.8566, 2.3522, 1)
        assertEquals(1, x)
        assertEquals(1, y)
    }

    @Test
    fun latLngToTile_equator_prime_meridian_zoom_2() {
        val (x, y) = TileCalculator.latLngToTile(0.0, 0.0, 2)
        assertEquals(2, x)
        assertEquals(2, y)
    }

    @Test
    fun getTilesForBounds_returns_correct_count_for_small_area() {
        val tiles = TileCalculator.getTilesForBounds(
            north = 48.87,
            south = 48.85,
            east = 2.36,
            west = 2.34,
            minZoom = 14,
            maxZoom = 14
        )
        assertFalse(tiles.isEmpty())
        assertTrue(tiles.size > 1)
    }

    @Test
    fun getTilesForBounds_across_zoom_levels_increases_tile_count() {
        val tilesZ14 = TileCalculator.getTilesForBounds(
            north = 48.87, south = 48.85, east = 2.36, west = 2.34,
            minZoom = 14, maxZoom = 14
        )
        val tilesZ14to15 = TileCalculator.getTilesForBounds(
            north = 48.87, south = 48.85, east = 2.36, west = 2.34,
            minZoom = 14, maxZoom = 15
        )
        assertTrue(tilesZ14to15.size > tilesZ14.size)
    }

    @Test
    fun estimateSizeBytes_is_reasonable() {
        val size = TileCalculator.estimateSizeBytes(100)
        assertEquals(100 * 15_000L, size)
    }

    @Test
    fun latLngToTile_clamps_latitude_at_85_degrees() {
        val (_, y1) = TileCalculator.latLngToTile(89.0, 0.0, 10)
        val (_, y2) = TileCalculator.latLngToTile(85.0511, 0.0, 10)
        assertEquals(y1, y2)
    }
}
