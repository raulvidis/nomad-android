package com.nomad.android.di

import android.app.ActivityManager
import android.content.Context
import com.nomad.android.data.ai.*
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
        val totalRamMB = activityManager.memoryClass.toLong()

        return when {
            AICoreEngine.isSupported(context) -> AICoreEngine(context)
            totalRamMB >= 256 -> LiteRTLMEngine(context, LiteRTLMEngine.ModelVariant.E2B)
            else -> LiteRTLMEngine(context, LiteRTLMEngine.ModelVariant.ONE_B)
        }
    }

    @Provides
    @Singleton
    fun provideAIEngineStatus(@ApplicationContext context: Context, engine: AIEngine): AIEngineStatus {
        return AIEngineStatus(
            engineType = when (engine) {
                is AICoreEngine -> AIEngineType.AICORE
                is LiteRTLMEngine -> if (engine.modelVariant == LiteRTLMEngine.ModelVariant.E2B) AIEngineType.LITERTLM_E2B else AIEngineType.LITERTLM_1B
                else -> AIEngineType.NONE
            },
            isReady = engine.isAvailable(),
            modelName = engine.getModelName(),
            ramRequired = engine.getDeviceInfo(),
            modelSize = if (engine is LiteRTLMEngine) "${engine.getModelSizeMB()} MB" else "System managed"
        )
    }
}
