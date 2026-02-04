package com.shouvick.mapup.feature.location.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {

    @Insert
    suspend fun insertSession(session: TrackingSession): Long

    @Insert
    suspend fun insertLocation(point: LocationPoint)

    @Update
    suspend fun updateSession(session: TrackingSession)

    @Query("SELECT * FROM session_table ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<TrackingSession>>

    @Query("SELECT * FROM location_table WHERE sessionOwnerId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsForSession(sessionId: Long): List<LocationPoint>

    @Query("SELECT * FROM session_table WHERE sessionId = :id")
    suspend fun getSessionById(id: Long): TrackingSession?
}