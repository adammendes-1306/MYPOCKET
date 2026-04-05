package com.example.mypocket.data

import com.example.mypocket.entity.TaskEntity

sealed class TaskListRow {
    data class Header(val text: String): TaskListRow()
    data class TaskItem(val task: TaskEntity): TaskListRow()
}