package com.example.mypocket.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.example.mypocket.entity.TransactionEntity
import com.example.mypocket.model.TransactionType
import kotlinx.coroutines.flow.Flow

data class CategoryStatRow(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: Double
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity): Long

    @Update
    suspend fun update(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE transactionId = :id")
    suspend fun deleteById(id: Long)

    // --- ADD THIS ---
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    // Wallet list (latest first)
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    // Filter by type
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun observeByType(type: TransactionType): Flow<List<TransactionEntity>>

    // Monthly range query (useful for Month-Year screen
    @Query("""
        SELECT * FROM transactions
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        ORDER BY timestamp DESC
    """)
    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    // Monthly totals (income/expense only)
    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS incomeTotal,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expenseTotal
        FROM transactions
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
    """)
    fun observeTotalsInRange(startMillis: Long, endMillis: Long): Flow<Totals>

    @Query("SELECT * FROM transactions WHERE transactionId = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    // ============================
    // NEW: Balance-safe insert
    // ============================

    @Transaction
    suspend fun insertWithBalanceUpdate(tx: TransactionEntity, accountDao: AccountDao) {

        // 1. Insert transaction first
        insert(tx)

        // 2. Apply balance changes based on type using IDs
        when (tx.type) {

            TransactionType.INCOME -> {
                tx.accountId?.let { accountDao.updateBalance(it, tx.amount) }
            }

            TransactionType.EXPENSE -> {
                tx.accountId?.let { accountDao.updateBalance(it, -tx.amount) }
            }

            TransactionType.TRANSFER -> {
                val fromId = tx.fromAccountId
                val toId = tx.toAccountId
                val fee = tx.feeAmount ?: 0.0

                if (fromId != null && toId != null) {
                    if (fromId == toId) {
                        // Same account: only apply fee
                        accountDao.updateBalance(fromId, -fee)
                    } else {
                        // Normal transfer: subtract from sender (amount + fee), add to receiver
                        accountDao.updateBalance(fromId, -tx.amount - fee)
                        accountDao.updateBalance(toId, tx.amount)
                    }
                }
            }
        }
    }

    @Query("""
    SELECT COALESCE(SUM(amount), 0)
    FROM transactions
    WHERE type = :type
      AND timestamp >= :startTimestamp AND timestamp < :endTimestamp
""")
    suspend fun getTotalByType(
        type: TransactionType,
        startTimestamp: Long,
        endTimestamp: Long
    ): Double

    @Query("""
    SELECT
        c.categoryId AS categoryId,
        c.name AS categoryName,
        COALESCE(SUM(t.amount), 0) AS totalAmount
    FROM transactions t
    INNER JOIN categories c ON t.categoryId = c.categoryId
    WHERE t.type = :type
      AND t.timestamp >= :startTimestamp AND t.timestamp < :endTimestamp
      AND t.categoryId IS NOT NULL
    GROUP BY c.categoryId, c.name
    HAVING totalAmount > 0
    ORDER BY totalAmount DESC
""")
    suspend fun getCategoryStatsByType(
        type: TransactionType,
        startTimestamp: Long,
        endTimestamp: Long
    ): List<CategoryStatRow>
}