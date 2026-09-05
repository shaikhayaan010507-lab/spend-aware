package com.ayaansaksham.spend_aware.data.local.dao

import androidx.room.*
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY dateMillis DESC")
    fun getExpensesByCategory(categoryId: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE dateMillis BETWEEN :startMillis AND :endMillis ORDER BY dateMillis DESC")
    fun getExpensesBetweenDates(startMillis: Long, endMillis: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE dateMillis BETWEEN :startMillis AND :endMillis")
    suspend fun getTotalSpentBetweenDates(startMillis: Long, endMillis: Long): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE categoryId = :categoryId AND dateMillis BETWEEN :startMillis AND :endMillis")
    suspend fun getTotalSpentByCategoryBetweenDates(categoryId: Int, startMillis: Long, endMillis: Long): Double?
}