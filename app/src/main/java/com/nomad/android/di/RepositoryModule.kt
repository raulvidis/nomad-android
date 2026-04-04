package com.nomad.android.di

import android.content.Context
import com.nomad.android.data.content.ContentPackManager
import com.nomad.android.data.content.KiwixManager
import com.nomad.android.data.content.KiwixManager
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.repository.ChatRepository
import com.nomad.android.data.repository.ContentPackRepository
import com.nomad.android.data.repository.MapsRepository
import com.nomad.android.data.repository.SearchRepository
import com.nomad.android.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideContentPackManager(
        @ApplicationContext context: Context,
        contentPackDao: ContentPackDao
    ): ContentPackManager = ContentPackManager(context, contentPackDao)

    @Provides
    @Singleton
    fun provideKiwixManager(
        @ApplicationContext context: Context
    ): KiwixManager = KiwixManager(context)

    @Provides
    @Singleton
    fun provideChatRepository(
        chatMessageDao: com.nomad.android.data.local.dao.ChatMessageDao
    ): ChatRepository = ChatRepository(chatMessageDao)

    @Provides
    @Singleton
    fun provideContentPackRepository(
        contentPackDao: ContentPackDao,
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ContentPackRepository = ContentPackRepository(contentPackDao, context, okHttpClient)

    @Provides
    @Singleton
    fun provideSearchRepository(
        searchHistoryDao: com.nomad.android.data.local.dao.SearchHistoryDao
    ): SearchRepository = SearchRepository(searchHistoryDao)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsDao: com.nomad.android.data.local.dao.SettingsDao,
        contentPackDao: ContentPackDao,
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepository(settingsDao, contentPackDao, context)

    @Provides
    @Singleton
    fun provideMapsRepository(
        @ApplicationContext context: Context
    ): MapsRepository = MapsRepository(context)
}
