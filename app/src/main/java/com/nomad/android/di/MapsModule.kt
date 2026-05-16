package com.nomad.android.di

import android.content.Context
import com.nomad.android.data.maps.OfflineTileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapsModule {

    @Provides
    @Singleton
    fun provideOfflineTileManager(@ApplicationContext context: Context): OfflineTileManager {
        val tilesDir = File(context.filesDir, "mapTiles")
        return OfflineTileManager(tilesDir = tilesDir)
    }
}
