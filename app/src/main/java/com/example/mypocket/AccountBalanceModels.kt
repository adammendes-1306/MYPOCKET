package com.example.mypocket.model

sealed class AccountBalanceModels {
    data class GroupHeader(
        val groupId: Long,
        val groupName: String,
        val groupTotal: Double
    ) : AccountBalanceModels()

    data class AccountChild(
        val accountId: Long,
        val accountName: String,
        val accountBalance: Double
    ) : AccountBalanceModels()
}

data class AccountBalancesUiState(
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val totalNetWorth: Double = 0.0,
    val items: List<AccountBalanceModels> = emptyList()
)

data class BalanceSummary(
    val totalAssets: Double,
    val totalLiabilities: Double
) {
    val totalNetWorth: Double
        get() = totalAssets - totalLiabilities
}