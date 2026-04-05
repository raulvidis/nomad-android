package com.nomad.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_snapshots")
data class LocationSnapshotEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val isTracking: Boolean
)
