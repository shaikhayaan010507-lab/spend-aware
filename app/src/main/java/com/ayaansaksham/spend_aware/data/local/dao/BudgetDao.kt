package com.ayaansaksham.spend_aware.data.local.dao

import androidx.room.*
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("SELECT * FROM budgets WHERE profileId = :profileId")
    fun getAllBudgets(profileId: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE categoryId IS NULL AND profileId = :profileId LIMIT 1")
    suspend fun getOverallBudget(profileId: Int): Budget?

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND profileId = :profileId LIMIT 1")
    suspend fun getBudgetForCategory(categoryId: Int, profileId: Int): Budget?
}