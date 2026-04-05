package com.example.mypocket.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mypocket.entity.AccountEntity
import com.example.mypocket.entity.AccountGroupEntity
import com.example.mypocket.entity.CategoryEntity
import com.example.mypocket.entity.EventEntity
import com.example.mypocket.Converters
import com.example.mypocket.entity.TaskEntity
import com.example.mypocket.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TaskEntity::class,
        EventEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        AccountGroupEntity::class,
        AccountEntity::class],
    version = 13,
    exportSchema = false)   // For migration tracking

/* // Bump version 1 -> 2 -> 3 (TransactionEntity added)
* -> 4 (CategoryEntity and AccountEntity) -> 5 (Update Database)
* -> 6 (Add order on CategoryEntity) -> 7 (order attribute renamed to sortOrder)
* -> 8 (val to var sortOrder) -> 9 (AccountGroup and Account)
* -> 10 (remove sortOrder on AccountGroupEntity) -> 11 (Changes name to id on TransactionEntity)
* -> 11 (imageUri becomes List) -> 13 (fields rename)
 */

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountGroupDao(): AccountGroupDao
    abstract fun accountDao(): AccountDao
    abstract fun accountBalancesDao(): AccountBalancesDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        const val DB_VERSION = 13  // For maintainability

         fun getInstance(context: Context): AppDatabase {
             return INSTANCE ?: synchronized(this) {
                 val instance = Room.databaseBuilder(
                     context.applicationContext,
                     AppDatabase::class.java,
                     "mypocket_db"
                 )
                     // This method requires parameter in the latest Room version
                     //.fallbackToDestructiveMigration(true)
                     .build()
                 INSTANCE = instance
                 instance
             }
         }

        // AppDatabase.kt (inside companion object)
        fun prepopulateDefaults(context: Context) {
            val db = getInstance(context)
            val categoryDao = db.categoryDao()
            val accountGroupDao = db.accountGroupDao()
            val accountDao = db.accountDao()

            CoroutineScope(Dispatchers.IO).launch {
                // Prepopulate categories
                if (categoryDao.getAllCategories().isEmpty()) {
                    DefaultData.incomeCategories.forEach { categoryDao.insertCategory(it) }
                    DefaultData.expenseCategories.forEach { categoryDao.insertCategory(it) }
                }

                // Prepopulate account groups
                if (accountGroupDao.getCount() == 0) {
                    // Insert groups and get generated IDs
                    val accountGroupIds = accountGroupDao.insertGroups(DefaultData.accountGroups)

                    // Prepopulate one default account per group
                    if (accountDao.getAllAccountsList().isEmpty()) {
                        val accountsWithGroupId = DefaultData.accounts.mapIndexed { index, account ->
                            account.copy(accountGroupId = accountGroupIds[index])
                        }
                        accountsWithGroupId.forEach { accountDao.insertAccount(it) }
                    }
                }
            }
        }
    }
}