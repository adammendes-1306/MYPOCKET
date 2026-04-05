package com.example.mypocket.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(

    @PrimaryKey(autoGenerate = true)
    val categoryId: Long = 0L,

    val name: String, // Display like this

    val type: String, // INCOME or EXPENSE

    var sortOrder: Int = 0   // Position index; 0,1,2,3,...
)