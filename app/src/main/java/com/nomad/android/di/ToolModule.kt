package com.nomad.android.di

import com.nomad.android.data.ai.DefaultLlamaBridgeHandle
import com.nomad.android.data.ai.KnowledgeBase
import com.nomad.android.data.ai.LlamaBridgeHandle
import com.nomad.android.data.ai.tools.ChatToolRegistry
import com.nomad.android.data.ai.tools.SearchKnowledgeBaseTool
import com.nomad.android.data.ai.tools.SearchNotesTool
import com.nomad.android.data.repository.NoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the registry of model-callable chat tools. Read-only KB/notes lookups,
 * so no approval gate — see the design spec.
 */
@Module
@InstallIn(SingletonComponent::class)
object ToolModule {

    @Provides
    @Singleton
    fun provideChatToolRegistry(
        knowledgeBase: KnowledgeBase,
        noteRepository: NoteRepository,
    ): ChatToolRegistry = ChatToolRegistry(
        listOf(
            SearchKnowledgeBaseTool(knowledgeBase),
            SearchNotesTool(noteRepository),
        ),
    )

    @Provides
    @Singleton
    fun provideLlamaBridgeHandle(): LlamaBridgeHandle = DefaultLlamaBridgeHandle()
}
