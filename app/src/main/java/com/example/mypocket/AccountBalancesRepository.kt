package com.example.mypocket.data

import com.example.mypocket.model.AccountBalanceItem
import com.example.mypocket.model.BalanceSummary
import com.example.mypocket.model.GroupedAccountBalance

class AccountBalancesRepository(
    private val dao: AccountBalancesDao
) {

    suspend fun getBalanceSummary(): BalanceSummary {
        val totalAssets = dao.getTotalIncome()
        val totalLiabilities = dao.getTotalExpense()

        return BalanceSummary(
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities
        )
    }

    suspend fun getGroupedAccountBalances(): List<GroupedAccountBalance> {
        val rows = dao.getAccountBalanceRows()

        return rows
            .groupBy { it.groupId }
            .map { (_, groupRows) ->
                val firstRow = groupRows.first()

                val accounts = groupRows.map { row ->
                    AccountBalanceItem(
                        accountId = row.accountId,
                        accountName = row.accountName,
                        accountBalance = row.accountBalance
                    )
                }

                GroupedAccountBalance(
                    groupId = firstRow.groupId,
                    groupName = firstRow.groupName,
                    groupTotal = accounts.sumOf { it.accountBalance },
                    accounts = accounts
                )
            }
    }
}