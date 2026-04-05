package com.example.mypocket.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_groups")
data class AccountGroupEntity(

    @PrimaryKey(autoGenerate = true)
    val accountGroupId: Long = 0L,

    val name: String,

    // Identify default groups (Cash, Bank, Card)
    val isDefault: Boolean = false
)