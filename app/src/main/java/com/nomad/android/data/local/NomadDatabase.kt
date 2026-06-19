package com.nomad.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nomad.android.data.local.dao.ChatMessageDao
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.dao.LocationSavedPointDao
import com.nomad.android.data.local.dao.LocationSnapshotDao
import com.nomad.android.data.local.dao.NoteDao
import com.nomad.android.data.local.dao.SearchHistoryDao
import com.nomad.android.data.local.dao.SettingsDao
import com.nomad.android.data.local.dao.TrackRouteDao
import com.nomad.android.data.local.entity.ChatMessageEntity
import com.nomad.android.data.local.entity.ChatSessionEntity
import com.nomad.android.data.local.entity.ContentPackEntity
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.data.local.entity.LocationSnapshotEntity
import com.nomad.android.data.local.entity.NoteEntity
import com.nomad.android.data.local.entity.SearchHistoryEntity
import com.nomad.android.data.local.entity.SettingsEntity
import com.nomad.android.data.local.entity.TrackRouteEntity

@Database(
    entities = [
        ContentPackEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        SearchHistoryEntity::class,
        SettingsEntity::class,
        LocationSnapshotEntity::class,
        LocationSavedPointEntity::class,
        TrackRouteEntity::class,
        NoteEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class NomadDatabase : RoomDatabase() {
    abstract fun contentPackDao(): ContentPackDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun locationSnapshotDao(): LocationSnapshotDao
    abstract fun locationSavedPointDao(): LocationSavedPointDao
    abstract fun trackRouteDao(): TrackRouteDao
    abstract fun noteDao(): NoteDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS track_routes (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        startLat REAL NOT NULL,
                        startLon REAL NOT NULL,
                        endLat REAL,
                        endLon REAL,
                        pointCount INTEGER NOT NULL,
                        totalDistanceMeters REAL NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE location_snapshots ADD COLUMN routeId TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN imageUri TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS location_snapshots_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        altitude REAL NOT NULL,
                        accuracy REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isTracking INTEGER NOT NULL,
                        routeId TEXT,
                        FOREIGN KEY (routeId) REFERENCES track_routes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO location_snapshots_new (id, latitude, longitude, altitude, accuracy, timestamp, isTracking, routeId)
                    SELECT id, latitude, longitude, altitude, accuracy, timestamp, isTracking, routeId FROM location_snapshots
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE location_snapshots")
                db.execSQL("ALTER TABLE location_snapshots_new RENAME TO location_snapshots")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_snapshots_routeId ON location_snapshots(routeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_snapshots_timestamp ON location_snapshots(timestamp)")
            }
        }
    }
}
