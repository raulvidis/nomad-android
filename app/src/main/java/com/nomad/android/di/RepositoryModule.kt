package com.nomad.android.di

import android.app.Application
import android.content.Context
import com.nomad.android.data.content.ContentPackManager
import com.nomad.android.data.content.KiwixManager
import com.nomad.android.data.local.dao.ChatMessageDao
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.dao.LocationSavedPointDao
import com.nomad.android.data.local.dao.LocationSnapshotDao
import com.nomad.android.data.local.dao.SearchHistoryDao
import com.nomad.android.data.local.dao.SettingsDao
import com.nomad.android.data.local.dao.TrackRouteDao
import com.nomad.android.data.repository.ChatRepository
import com.nomad.android.data.repository.ContentPackRepository
import com.nomad.android.data.repository.LocationRepository
import com.nomad.android.data.repository.MapsRepository
import com.nomad.android.data.repository.SearchRepository
import com.nomad.android.data.repository.SettingsRepository
import com.nomad.android.util.LocationSnapshotDb
import com.nomad.android.util.LocationTrackerService
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
        contentPackDao: ContentPackDao,
        okHttpClient: OkHttpClient
    ): ContentPackManager = ContentPackManager(context, contentPackDao, okHttpClient)

    @Provides
    @Singleton
    fun provideKiwixManager(
        @ApplicationContext context: Context
    ): KiwixManager = KiwixManager(context)

    @Provides
    @Singleton
    fun provideChatRepository(
        chatMessageDao: ChatMessageDao
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
        searchHistoryDao: SearchHistoryDao
    ): SearchRepository = SearchRepository(searchHistoryDao)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsDao: SettingsDao,
        contentPackDao: ContentPackDao,
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepository(settingsDao, contentPackDao, context)

    @Provides
    @Singleton
    fun provideMapsRepository(
        @ApplicationContext context: Context,
        contentPackManager: ContentPackManager
    ): MapsRepository = MapsRepository(context, contentPackManager)

    @Provides
    @Singleton
    fun provideLocationTrackerService(
        application: Application,
        locationSnapshotDao: LocationSnapshotDao
    ): LocationTrackerService {
        val snapshotDb = object : LocationSnapshotDb {
            override suspend fun saveSnapshot(snapshot: com.nomad.android.data.local.entity.LocationSnapshotEntity) {
                locationSnapshotDao.insert(snapshot)
            }
        }
        return LocationTrackerService(application, snapshotDb)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        locationSnapshotDao: LocationSnapshotDao,
        locationSavedPointDao: LocationSavedPointDao,
        trackRouteDao: TrackRouteDao,
        locationTrackerService: LocationTrackerService
    ): LocationRepository = LocationRepository(locationSnapshotDao, locationSavedPointDao, trackRouteDao, locationTrackerService)
}
