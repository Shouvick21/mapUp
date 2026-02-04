package com.shouvick.mapup.feature.location.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


@Entity(tableName = "session_table")
data class TrackingSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val totalDistance: Float = 0f // In meters
)

@Entity(
    tableName = "location_table",
    foreignKeys = [
        ForeignKey(
            entity = TrackingSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LocationPoint(
    @PrimaryKey(autoGenerate = true) val pointId: Long = 0,
    val sessionOwnerId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float
)