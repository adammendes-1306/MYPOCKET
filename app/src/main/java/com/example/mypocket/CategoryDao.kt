package com.example.mypocket.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.mypocket.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // --- ADD THIS ---
    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("SELECT * FROM categories WHERE categoryId = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity

    @Query("SELECT * FROM categories WHERE categoryId = :id LIMIT 1")
    suspend fun getCategoryByIdNullable(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    // For one-time fetch
    @Query("SELECT * FROM categories WHERE type = :type")
    suspend fun getCategoriesByType(type: String): List<CategoryEntity>

    // For live updates
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC")
    fun getCategoriesByTypeFlow(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT MAX(sortOrder) FROM categories WHERE type = :type")
    suspend fun getMaxOrder(type: String): Int?

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC")
    fun getCategoriesByTypeOrdered(type: String): List<CategoryEntity>

    // Resolve category name → id
    @Query("SELECT categoryId FROM categories WHERE name = :name LIMIT 1")
    suspend fun getIdByName(name: String): Long?
}