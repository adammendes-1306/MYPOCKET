package com.example.mypocket.entity

import androidx.room.Entity         // Used to create Entity/Table
import androidx.room.PrimaryKey     // Used to declare Primary Key
import com.example.mypocket.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity (
    @PrimaryKey(autoGenerate = true)
    val transactionId: Long = 0L,

    // Data type is from TransactionType
    val type: TransactionType,

    val timestamp: Long,              // Store datetime as millis
    val amount: Double,              // Always positive in DB
    val feeAmount: Double? = null,   // Transfer fee (optional)

    // For INCOME/EXPENSE
    val categoryId: Long? = null,
    val accountId: Long? = null,

    // For TRANSFER
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null,

    // Optional - String? = null
    val note: String? = null,
    val description: String? = null,

    // Optional images (max 3 URIs stored as JSON string)
    val imageUris: List<String>? = null
)