package com.nomad.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nomad.android.data.local.dao.ChatMessageDao
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.dao.LocationSavedPointDao
import com.nomad.android.data.local.dao.LocationSnapshotDao
import com.nomad.android.data.local.dao.SearchHistoryDao
import com.nomad.android.data.local.dao.SettingsDao
import com.nomad.android.data.local.entity.ChatMessageEntity
import com.nomad.android.data.local.entity.ChatSessionEntity
import com.nomad.android.data.local.entity.ContentPackEntity
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.data.local.entity.LocationSnapshotEntity
import com.nomad.android.data.local.entity.SearchHistoryEntity
import com.nomad.android.data.local.entity.SettingsEntity

@Database(
    entities = [
        ContentPackEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        SearchHistoryEntity::class,
        SettingsEntity::class,
        LocationSnapshotEntity::class,
        LocationSavedPointEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class NomadDatabase : RoomDatabase() {
    abstract fun contentPackDao(): ContentPackDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun locationSnapshotDao(): LocationSnapshotDao
    abstract fun locationSavedPointDao(): LocationSavedPointDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS location_snapshots (
                        id TEXT NOT NULL PRIMARY KEY,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        altitude REAL NOT NULL,
                        accuracy REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isTracking INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS location_saved_points (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        altitude REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        notes TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
