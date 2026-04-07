package com.nomad.android.di

import android.app.ActivityManager
import android.content.Context
import com.nomad.android.data.ai.AIEngine
import com.nomad.android.data.ai.AIEngineStatus
import com.nomad.android.data.ai.AIEngineType
import com.nomad.android.data.ai.FallbackEngine
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
    fun provideAIEngine(@ApplicationContext context: Context): AIEngine {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamMB = memoryInfo.totalMem / (1024 * 1024)

        val variant = LiteRTLMEngine.recommendedVariant(totalRamMB)
        return LiteRTLMEngine(context, variant, totalRamMB)
    }

    @Provides
    @Singleton
    fun provideRAGEngine(engine: AIEngine): RAGEngine = RAGEngine(engine)

    @Provides
    @Singleton
    fun provideAIEngineStatus(engine: AIEngine): AIEngineStatus {
        val deviceInfo = engine.getDeviceInfo()
        return AIEngineStatus(
            engineType = when (engine) {
                is LiteRTLMEngine -> AIEngineType.LITERTLM_E2B
                else -> AIEngineType.FALLBACK
            },
            isReady = false,
            modelName = engine.getModelName(),
            ramRequired = "${deviceInfo.totalRamMB}MB total",
            modelSize = if (engine is LiteRTLMEngine) "${engine.getModelSizeMB()} MB" else "N/A"
        )
    }
}
