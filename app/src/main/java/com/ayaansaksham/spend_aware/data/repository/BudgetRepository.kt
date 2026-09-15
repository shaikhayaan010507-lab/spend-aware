package com.ayaansaksham.spend_aware.data.repository

import com.ayaansaksham.spend_aware.data.local.dao.BudgetDao
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    fun getAllBudgets(profileId: Int): Flow<List<Budget>> = budgetDao.getAllBudgets(profileId)

    suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }

    suspend fun getOverallBudget(profileId: Int): Budget? {
        return budgetDao.getOverallBudget(profileId)
    }

    suspend fun getBudgetForCategory(categoryId: Int, profileId: Int): Budget? {
        return budgetDao.getBudgetForCategory(categoryId, profileId)
    }
}