package com.example.mypocket.model

data class GroupedAccountBalance(
    val groupId: Long,
    val groupName: String,
    val groupTotal: Double,
    val accounts: List<AccountBalanceItem>
)

data class AccountBalanceItem(
    val accountId: Long,
    val accountName: String,
    val accountBalance: Double
)
