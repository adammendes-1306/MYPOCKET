package com.example.mypocket.model

data class AccountBalanceRow(
    val groupId: Long,
    val groupName: String,
    val accountId: Long,
    val accountName: String,
    val accountBalance: Double
)