package com.example.mypocket.entity

import androidx.room.Entity         // New entity
import androidx.room.PrimaryKey     // To set Primary Key

@Entity(tableName = "events")
data class EventEntity (
    @PrimaryKey(autoGenerate = true)
    val eventId: Long = 0L,

    val title: String,
    val description: String,

    // Store millis (easy for date/time comparisons)
    val startAt: Long,
    val endAt: Long,

    val allDay: Boolean,        // Switch Yes or No

    // Reminder minutes BEFORE start time: 10, 30, 60, etc, 0 = none
    val reminderMinutes: Int,

    // True = Busy, False = Free
    val isBusy: Boolean,

    val createdAt: Long = System.currentTimeMillis()
)
