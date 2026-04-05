package com.example.mypocket.data

import androidx.room.Dao
import androidx.room.Query
import com.example.mypocket.model.AccountBalanceRow

@Dao
interface AccountBalancesDao {

    @Query(
        """
        SELECT
            ag.accountGroupId AS groupId,
            ag.name AS groupName,
            a.accountId AS accountId,
            a.name AS accountName,
            a.amount AS accountBalance
        FROM account_groups ag
        INNER JOIN accounts a ON a.accountGroupId = ag.accountGroupId
        ORDER BY ag.name, a.name
        """
    )
    suspend fun getAccountBalanceRows(): List<AccountBalanceRow>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0)
        FROM transactions
        WHERE type = 'INCOME'
        """
    )
    suspend fun getTotalIncome(): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0)
        FROM transactions
        WHERE type = 'EXPENSE'
        """
    )
    suspend fun getTotalExpense(): Double
}