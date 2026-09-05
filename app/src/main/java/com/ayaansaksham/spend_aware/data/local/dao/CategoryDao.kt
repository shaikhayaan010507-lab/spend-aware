package com.ayaansaksham.spend_aware.data.local.dao

import androidx.room.*
import com.ayaansaksham.spend_aware.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE categoryId = :categoryId")
    suspend fun getCategoryById(categoryId: Int): Category?
}