package com.example.mypocket.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction
import com.example.mypocket.entity.AccountGroupEntity
import com.example.mypocket.model.AccountGroupWithAccounts

@Dao
interface AccountGroupDao {

    @Query("SELECT * FROM account_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<AccountGroupEntity>>

    @Insert
    suspend fun insertGroup(group: AccountGroupEntity): Long

    @Insert
    suspend fun insertGroups(groups: List<AccountGroupEntity>): List<Long>

    @Update
    suspend fun updateGroup(group: AccountGroupEntity)

    @Delete
    suspend fun deleteGroup(group: AccountGroupEntity)

    @Query("DELETE FROM account_groups")
    suspend fun deleteAllGroups()

    // Update order after drag & drop
    @Update
    suspend fun updateGroups(groups: List<AccountGroupEntity>)

    // Useful for initialization check
    @Query("SELECT COUNT(*) FROM account_groups")
    suspend fun getCount(): Int

    @Transaction
    @Query("SELECT * FROM account_groups ORDER BY name ASC")
    fun getGroupsWithAccounts(): Flow<List<AccountGroupWithAccounts>>
}