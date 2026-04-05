package com.example.mypocket

import androidx.room.TypeConverter
import com.example.mypocket.model.TransactionType

// Room can't store enums automatically
// This converter stores them as String
class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    // Room cannot store List<String> directly, so convert it
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")
    }
    @TypeConverter
    fun fromString(value: String?): List<String> {
        return value?.split("|") ?: emptyList()
    }

    @TypeConverter
    fun listToString(list: List<String>?): String {
        return list?.joinToString("|") ?: ""
    }
}