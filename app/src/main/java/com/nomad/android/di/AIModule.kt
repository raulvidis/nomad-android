package com.nomad.android.di

import android.app.ActivityManager
import android.content.Context
import com.nomad.android.data.ai.AIEngine
import com.nomad.android.data.ai.AIEngineManager
import com.nomad.android.data.ai.AIEngineStatus
import com.nomad.android.data.ai.AIEngineType
import com.nomad.android.data.ai.FallbackEngine
import com.nomad.android.data.ai.LlamaCppEngine
import com.nomad.android.data.ai.RAGEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideFallbackEngine(): FallbackEngine = FallbackEngine()

    @Provides
    @Singleton
    fun provideAIEngineManager(
        @ApplicationContext context: Context,
        fallbackEngine: FallbackEngine
    ): AIEngineManager {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamMB = memoryInfo.totalMem / (1024 * 1024)

        val modelsDir = java.io.File(context.filesDir, "models")
        val downloadedVariant = LlamaCppEngine.ModelVariant.entries.firstOrNull { variant ->
            java.io.File(modelsDir, variant.fileName).let { it.exists() && it.length() > 1_000_000 }
        }
        val variant = downloadedVariant ?: LlamaCppEngine.recommendedVariant(totalRamMB)
        return AIEngineManager(context, totalRamMB, variant, fallbackEngine)
    }

    @Provides
    @Singleton
    fun provideAIEngine(manager: AIEngineManager): AIEngine = manager

    @Provides
    @Singleton
    fun provideRAGEngine(engine: AIEngine): RAGEngine = RAGEngine(engine)

    @Provides
    fun provideAIEngineStatus(manager: AIEngineManager): AIEngineStatus = manager.engineStatus.value
}
