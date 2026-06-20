package com.nomad.android.di

import android.content.Context
import androidx.room.Room
import com.nomad.android.data.local.NomadDatabase
import com.nomad.android.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.MINUTES)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NomadDatabase {
        return Room.databaseBuilder(
            context,
            NomadDatabase::class.java,
            "nomad.db"
        )
            .addMigrations(NomadDatabase.MIGRATION_1_2)
            .addMigrations(NomadDatabase.MIGRATION_2_3)
            .addMigrations(NomadDatabase.MIGRATION_3_4)
            .addMigrations(NomadDatabase.MIGRATION_4_5)
            .addMigrations(NomadDatabase.MIGRATION_5_6)
            .build()
    }

    @Provides fun provideContentPackDao(db: NomadDatabase) = db.contentPackDao()
    @Provides fun provideChatMessageDao(db: NomadDatabase) = db.chatMessageDao()
    @Provides fun provideSearchHistoryDao(db: NomadDatabase) = db.searchHistoryDao()
    @Provides fun provideSettingsDao(db: NomadDatabase) = db.settingsDao()
    @Provides fun provideLocationSnapshotDao(db: NomadDatabase) = db.locationSnapshotDao()
    @Provides fun provideLocationSavedPointDao(db: NomadDatabase) = db.locationSavedPointDao()
    @Provides fun provideTrackRouteDao(db: NomadDatabase) = db.trackRouteDao()
    @Provides fun provideNoteDao(db: NomadDatabase) = db.noteDao()
}
