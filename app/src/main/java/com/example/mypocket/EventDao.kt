package com.example.mypocket.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.mypocket.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Query("SELECT * FROM events ORDER BY startAt ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("DELETE FROM events WHERE eventId = :id")
    suspend fun deleteById(id: Int)

    // A. Events for a day (full screen list)
    @Query("""
    SELECT * FROM events
    WHERE startAt < :dayEnd AND endAt >= :dayStart
    ORDER BY startAt ASC
    """)
    fun observeEventsForDay(dayStart: Long, dayEnd: Long): kotlinx.coroutines.flow.Flow<List<EventEntity>>

    // B. Events in a month (to mark cells/show titles)
    @Query("""
    SELECT * FROM events
    WHERE startAt < :rangeEnd AND endAt >= :rangeStart
    ORDER BY startAt ASC
    """)
    fun observeEventsInRange(rangeStart: Long, rangeEnd: Long): kotlinx.coroutines.flow.Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE eventId = :id LIMIT 1")
    suspend fun getById(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE eventId = :id LIMIT 1")
    fun observeById(id: Long): kotlinx.coroutines.flow.Flow<EventEntity?>

    @Update
    suspend fun update(event: EventEntity)

    @Delete
    suspend fun delete(event: EventEntity)
    // OR if you prefer by id:
    // @Query("DELETE FROM events WHERE id = :id")
    // suspend fun deleteById(id: Long)

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    @Query("SELECT * FROM events WHERE reminderMinutes > 0")
    fun getAllEventsWithReminder(): List<EventEntity>
}