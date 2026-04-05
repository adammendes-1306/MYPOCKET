package com.example.mypocket.data

import androidx.room.*          // Import everything
import com.example.mypocket.entity.TaskEntity

@Dao
interface TaskDao {

    @Query("Select * FROM tasks WHERE dateEpochDay = :epochDay ORDER BY isCompleted ASC, createdAtMillis DESC")
    suspend fun getTasksForDay(epochDay: Long): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, taskId DESC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("UPDATE tasks SET isCompleted = :completed WHERE taskId = :taskId")
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean)

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dateEpochDay ASC")
    suspend fun getIncompleteTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE taskId = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?

}