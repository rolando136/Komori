package com.rolando.komori.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyEventDao {
    @Query("SELECT * FROM weekly_events")
    fun getAllEvents(): Flow<List<WeeklyEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: WeeklyEvent)

    @Delete
    suspend fun deleteEvent(event: WeeklyEvent)
}