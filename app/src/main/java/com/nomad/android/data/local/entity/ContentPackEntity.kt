package com.nomad.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_packs")
data class ContentPackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val sizeBytes: Long,
    val status: String,
    val downloadedAt: Long?,
    val version: String,
    val description: String
)
