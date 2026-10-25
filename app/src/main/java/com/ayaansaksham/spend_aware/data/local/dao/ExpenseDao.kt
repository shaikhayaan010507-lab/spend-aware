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

    @Query("SELECT * FROM expenses WHERE profileId = :profileId ORDER BY dateMillis DESC")
    fun getAllExpenses(profileId: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId AND profileId = :profileId ORDER BY dateMillis DESC")
    fun getExpensesByCategory(categoryId: Int, profileId: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE profileId = :profileId AND dateMillis BETWEEN :startMillis AND :endMillis ORDER BY dateMillis DESC")
    fun getExpensesBetweenDates(startMillis: Long, endMillis: Long, profileId: Int): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE profileId = :profileId AND dateMillis BETWEEN :startMillis AND :endMillis")
    suspend fun getTotalSpentBetweenDates(startMillis: Long, endMillis: Long, profileId: Int): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE categoryId = :categoryId AND profileId = :profileId AND dateMillis BETWEEN :startMillis AND :endMillis")
    suspend fun getTotalSpentByCategoryBetweenDates(categoryId: Int, startMillis: Long, endMillis: Long, profileId: Int): Double?

    @Query("SELECT amount FROM expenses WHERE categoryId = :categoryId AND profileId = :profileId ORDER BY dateMillis DESC")
    suspend fun getCategoryAmounts(categoryId: Int, profileId: Int): List<Double>

    @Query("SELECT amount FROM expenses WHERE categoryId = :categoryId AND profileId = :profileId AND expenseId != :excludeExpenseId ORDER BY dateMillis DESC")
    suspend fun getCategoryAmountsExcluding(categoryId: Int, profileId: Int, excludeExpenseId: Int): List<Double>
}