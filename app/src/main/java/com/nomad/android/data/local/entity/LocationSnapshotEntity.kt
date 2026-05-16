package com.nomad.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = TrackRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("routeId"),
        Index("timestamp")
    ]
)
data class LocationSnapshotEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val isTracking: Boolean,
    val routeId: String? = null
)
