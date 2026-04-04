package com.nomad.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nomad.android.data.local.dao.ChatMessageDao
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.dao.SearchHistoryDao
import com.nomad.android.data.local.dao.SettingsDao
import com.nomad.android.data.local.entity.ChatMessageEntity
import com.nomad.android.data.local.entity.ChatSessionEntity
import com.nomad.android.data.local.entity.ContentPackEntity
import com.nomad.android.data.local.entity.SearchHistoryEntity
import com.nomad.android.data.local.entity.SettingsEntity

@Database(
    entities = [
        ContentPackEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        SearchHistoryEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NomadDatabase : RoomDatabase() {
    abstract fun contentPackDao(): ContentPackDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun settingsDao(): SettingsDao
}
