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

        return when {
            totalRamMB >= 6144 -> LiteRTLMEngine(context, LiteRTLMEngine.ModelVariant.E2B)
            totalRamMB >= 2048 -> LiteRTLMEngine(context, LiteRTLMEngine.ModelVariant.ONE_B)
            else -> FallbackEngine(context)
        }
    }

    @Provides
    @Singleton
    fun provideRAGEngine(engine: AIEngine): RAGEngine = RAGEngine(engine)

    @Provides
    @Singleton
    fun provideAIEngineStatus(engine: AIEngine): AIEngineStatus {
        return AIEngineStatus(
            engineType = when (engine) {
                is LiteRTLMEngine -> if (engine.modelVariant == LiteRTLMEngine.ModelVariant.E2B) AIEngineType.LITERTLM_E2B else AIEngineType.LITERTLM_1B
                is FallbackEngine -> AIEngineType.FALLBACK
                else -> AIEngineType.NONE
            },
            isReady = false,
            modelName = engine.getModelName(),
            ramRequired = engine.getDeviceInfo().let { "${it.totalRamMB}MB total" },
            modelSize = if (engine is LiteRTLMEngine) "${engine.getModelSizeMB()} MB" else "N/A"
        )
    }
}
