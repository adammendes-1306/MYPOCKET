package com.example.mypocket.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = AccountGroupEntity::class,
            parentColumns = ["accountGroupId"],
            childColumns = ["accountGroupId"],
            onDelete = ForeignKey.CASCADE // delete accounts if group deleted
        )
    ],
    indices = [Index(value = ["accountGroupId"])]
)
data class AccountEntity(

    @PrimaryKey(autoGenerate = true)
    val accountId: Long = 0L,

    // FK to AccountGroupEntity
    val accountGroupId: Long,

    val name: String,

    val amount: Double = 0.0,

    val description: String? = null
)