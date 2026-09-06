package com.ayaansaksham.spend_aware.data.repository

import com.ayaansaksham.spend_aware.data.local.dao.BudgetDao
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }

    suspend fun getOverallBudget(): Budget? {
        return budgetDao.getOverallBudget()
    }

    suspend fun getBudgetForCategory(categoryId: Int): Budget? {
        return budgetDao.getBudgetForCategory(categoryId)
    }
}