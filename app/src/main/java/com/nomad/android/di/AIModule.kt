package com.nomad.android.di

import android.app.ActivityManager
import android.content.Context
import com.nomad.android.data.ai.AIEngine
import com.nomad.android.data.ai.AIEngineManager
import com.nomad.android.data.ai.AIEngineStatus
import com.nomad.android.data.ai.AIEngineType
import com.nomad.android.data.ai.LiteRTLMEngine
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
    fun provideAIEngineManager(@ApplicationContext context: Context): AIEngineManager {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamMB = memoryInfo.totalMem / (1024 * 1024)

        val modelsDir = java.io.File(context.filesDir, "models")
        val downloadedVariant = LiteRTLMEngine.ModelVariant.entries.firstOrNull { variant ->
            java.io.File(modelsDir, variant.fileName).let { it.exists() && it.length() > 1_000_000 }
        }
        val variant = downloadedVariant ?: LiteRTLMEngine.recommendedVariant(totalRamMB)
        return AIEngineManager(context, totalRamMB, variant)
    }

    @Provides
    @Singleton
    fun provideAIEngine(manager: AIEngineManager): AIEngine = manager

    @Provides
    @Singleton
    fun provideRAGEngine(engine: AIEngine): RAGEngine = RAGEngine(engine)

    @Provides
    @Singleton
    fun provideAIEngineStatus(manager: AIEngineManager): AIEngineStatus {
        val deviceInfo = manager.getDeviceInfo()
        return AIEngineStatus(
            engineType = AIEngineType.LITERTLM_E2B,
            isReady = false,
            modelName = manager.getModelName(),
            ramRequired = "${deviceInfo.totalRamMB}MB total",
            modelSize = "${manager.getModelSizeMB()} MB"
        )
    }
}
