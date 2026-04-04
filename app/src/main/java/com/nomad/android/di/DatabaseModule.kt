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
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NomadDatabase {
        return Room.databaseBuilder(
            context,
            NomadDatabase::class.java,
            "nomad.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideContentPackDao(db: NomadDatabase) = db.contentPackDao()
    @Provides fun provideChatMessageDao(db: NomadDatabase) = db.chatMessageDao()
    @Provides fun provideSearchHistoryDao(db: NomadDatabase) = db.searchHistoryDao()
    @Provides fun provideSettingsDao(db: NomadDatabase) = db.settingsDao()
}

    @Provides fun provideContentPackDao(db: NomadDatabase) = db.contentPackDao()
    @Provides fun provideChatMessageDao(db: NomadDatabase) = db.chatMessageDao()
    @Provides fun provideSearchHistoryDao(db: NomadDatabase) = db.searchHistoryDao()
    @Provides fun provideSettingsDao(db: NomadDatabase) = db.settingsDao()
}
