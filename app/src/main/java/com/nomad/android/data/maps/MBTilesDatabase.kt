package com.nomad.android.data.maps

import android.database.sqlite.SQLiteDatabase
import java.io.File

class MBTilesDatabase(private val path: String) {

    private var db: SQLiteDatabase? = null

    fun open() {
        val file = File(path)
        file.parentFile?.mkdirs()
        db = SQLiteDatabase.openOrCreateDatabase(file, null).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS metadata (
                    name TEXT PRIMARY KEY,
                    value TEXT
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS tiles (
                    zoom_level INTEGER,
                    tile_column INTEGER,
                    tile_row INTEGER,
                    tile_data BLOB,
                    PRIMARY KEY (zoom_level, tile_column, tile_row)
                )
                """.trimIndent()
            )
        }
    }

    fun close() {
        db?.close()
        db = null
    }

    fun insertTile(z: Int, x: Int, y: Int, data: ByteArray) {
        val tmsY = (1 shl z) - 1 - y
        db?.compileStatement(
            "INSERT OR REPLACE INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)"
        )?.use { stmt ->
            stmt.bindLong(1, z.toLong())
            stmt.bindLong(2, x.toLong())
            stmt.bindLong(3, tmsY.toLong())
            stmt.bindBlob(4, data)
            stmt.executeInsert()
        }
    }

    fun getTile(z: Int, x: Int, y: Int): ByteArray? {
        val tmsY = (1 shl z) - 1 - y
        val cursor = db?.rawQuery(
            "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?",
            arrayOf(z.toString(), x.toString(), tmsY.toString())
        )
        return cursor?.use {
            if (it.moveToFirst()) it.getBlob(0) else null
        }
    }

    fun setMetadata(name: String, value: String) {
        db?.execSQL(
            "INSERT OR REPLACE INTO metadata (name, value) VALUES (?, ?)",
            arrayOf(name, value)
        )
    }

    fun getMetadata(name: String): String? {
        val cursor = db?.rawQuery(
            "SELECT value FROM metadata WHERE name = ?",
            arrayOf(name)
        )
        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    fun getTileCount(): Int {
        val cursor = db?.rawQuery("SELECT COUNT(*) FROM tiles", null)
        return cursor?.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        } ?: 0
    }
}
