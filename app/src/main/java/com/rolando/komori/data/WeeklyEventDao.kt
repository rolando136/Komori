package com.rolando.komori.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WeeklyEventDao {
    @Query("SELECT * FROM weekly_events")
    fun getAllEvents(): Flow<List<WeeklyEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: WeeklyEvent)

    @Delete
    suspend fun deleteEvent(event: WeeklyEvent)
    // --- ADD THESE 3 FUNCTIONS ---

    @Query("DELETE FROM weekly_events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Int)

    @Query("DELETE FROM weekly_events WHERE groupId = :groupId AND startDate >= :date")
    suspend fun deleteFutureEvents(groupId: Long, date: java.time.LocalDate)

    @Query("DELETE FROM weekly_events WHERE groupId = :groupId")
    suspend fun deleteAllEventsInSeries(groupId: Long)

    @Query("UPDATE weekly_events SET excludedDates = CASE WHEN excludedDates = '' THEN :newDate ELSE excludedDates || ',' || :newDate END WHERE id = :id")
    suspend fun addExclusionDate(id: Int, newDate: String)

    @Query("UPDATE weekly_events SET recurrenceEnd = :endDate WHERE id = :id")
    suspend fun updateRecurrenceEnd(id: Int, endDate: LocalDate)


}

