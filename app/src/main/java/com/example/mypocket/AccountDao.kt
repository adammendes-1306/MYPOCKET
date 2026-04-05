package com.example.mypocket.data

import androidx.room.*
import com.example.mypocket.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    // Get all accounts (for picker)
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsList(): List<AccountEntity>

    // Get accounts by group (useful for grouping UI)
    @Query("SELECT * FROM accounts WHERE accountGroupId = :groupId")
    fun getAccountsByGroup(groupId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE accountId = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT accountId FROM accounts WHERE name = :name LIMIT 1")
    suspend fun getIdByName(name: String): Long? // helper to resolve name → ID

    @Insert
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    // Update by ID instead of name
    @Query("UPDATE accounts SET amount = amount + :delta WHERE accountId = :accountId")
    suspend fun updateBalance(accountId: Long, delta: Double)
}