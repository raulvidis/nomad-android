package com.nomad.android.data.maps

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan
import kotlin.math.cos

object TileCalculator {

    data class TileCoord(val x: Int, val y: Int, val z: Int)

    fun latLngToTile(lat: Double, lng: Double, zoom: Int): Pair<Int, Int> {
        val clampedLat = lat.coerceIn(-85.0511, 85.0511)
        val n = 2.0.pow(zoom.toDouble())
        val x = floor((lng + 180.0) / 360.0 * n).toInt().coerceIn(0, n.toInt() - 1)
        val latRad = Math.toRadians(clampedLat)
        val y = floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n).toInt()
            .coerceIn(0, n.toInt() - 1)
        return x to y
    }

    fun getTilesForBounds(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int,
        maxZoom: Int
    ): List<TileCoord> {
        val tiles = mutableListOf<TileCoord>()
        for (z in minZoom..maxZoom) {
            val (minX, minY) = latLngToTile(north, west, z)
            val (maxX, maxY) = latLngToTile(south, east, z)
            for (x in minOf(minX, maxX)..maxOf(minX, maxX)) {
                for (y in minOf(minY, maxY)..maxOf(minY, maxY)) {
                    tiles.add(TileCoord(x, y, z))
                }
            }
        }
        return tiles
    }

    fun estimateSizeBytes(tileCount: Int, avgTileBytes: Int = 15_000): Long {
        return tileCount.toLong() * avgTileBytes
    }

    fun tileUrl(x: Int, y: Int, z: Int): String =
        "https://tile.openstreetmap.org/$z/$x/$y.png"
}
