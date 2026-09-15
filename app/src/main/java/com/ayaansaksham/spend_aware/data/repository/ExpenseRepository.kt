package com.ayaansaksham.spend_aware.data.repository

import com.ayaansaksham.spend_aware.data.local.dao.ExpenseDao
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun getAllExpenses(profileId: Int): Flow<List<Expense>> = expenseDao.getAllExpenses(profileId)

    suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    fun getExpensesByCategory(categoryId: Int, profileId: Int): Flow<List<Expense>> {
        return expenseDao.getExpensesByCategory(categoryId, profileId)
    }

    fun getExpensesBetweenDates(startMillis: Long, endMillis: Long, profileId: Int): Flow<List<Expense>> {
        return expenseDao.getExpensesBetweenDates(startMillis, endMillis, profileId)
    }

    suspend fun getCurrentMonthTotal(profileId: Int): Double {
        val (startOfMonth, now) = getCurrentMonthRange()
        return expenseDao.getTotalSpentBetweenDates(startOfMonth, now, profileId) ?: 0.0
    }

    suspend fun getCurrentMonthTotalForCategory(categoryId: Int, profileId: Int): Double {
        val (startOfMonth, now) = getCurrentMonthRange()
        return expenseDao.getTotalSpentByCategoryBetweenDates(categoryId, startOfMonth, now, profileId) ?: 0.0
    }

    suspend fun getTotalSpentBetweenDates(startMillis: Long, endMillis: Long, profileId: Int): Double {
        return expenseDao.getTotalSpentBetweenDates(startMillis, endMillis, profileId) ?: 0.0
    }

    suspend fun getTotalSpentByCategoryBetweenDates(
        categoryId: Int,
        startMillis: Long,
        endMillis: Long,
        profileId: Int
    ): Double {
        return expenseDao.getTotalSpentByCategoryBetweenDates(categoryId, startMillis, endMillis, profileId) ?: 0.0
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        return Pair(startOfMonth, now)
    }
}