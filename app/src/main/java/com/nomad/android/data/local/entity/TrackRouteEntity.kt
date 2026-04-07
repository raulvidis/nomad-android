package com.nomad.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_routes")
data class TrackRouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startLat: Double,
    val startLon: Double,
    val endLat: Double? = null,
    val endLon: Double? = null,
    val pointCount: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val createdAt: Long,
    val isActive: Boolean = true
)
