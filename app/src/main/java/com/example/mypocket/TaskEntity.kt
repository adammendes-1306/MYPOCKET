package com.example.mypocket.entity

import androidx.room.Entity     // Using Entity
import androidx.room.PrimaryKey // Using Primary Key

@Entity(tableName = "tasks")
data class TaskEntity (
    @PrimaryKey(autoGenerate = true)
    val taskId: Long = 0L,     // OL for Long, 0 for Int
    val title: String,
    val note: String? = null,       // Can be null

    val dateEpochDay: Long,         // LocalDate.toEpochDay()

    val isCompleted: Boolean = false,    // Task in default is not completed yet

    val createdAtMillis: Long = System.currentTimeMillis()
)