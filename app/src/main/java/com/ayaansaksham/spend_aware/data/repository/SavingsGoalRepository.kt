package com.ayaansaksham.spend_aware.data.repository

import com.ayaansaksham.spend_aware.data.local.dao.SavingsGoalDao
import com.ayaansaksham.spend_aware.data.local.entity.SavingsGoal
import kotlinx.coroutines.flow.Flow

class SavingsGoalRepository(private val savingsGoalDao: SavingsGoalDao) {

    val allGoals: Flow<List<SavingsGoal>> = savingsGoalDao.getAllGoals()

    suspend fun insertGoal(goal: SavingsGoal) {
        savingsGoalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: SavingsGoal) {
        savingsGoalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: SavingsGoal) {
        savingsGoalDao.deleteGoal(goal)
    }

    suspend fun getGoalById(goalId: Int): SavingsGoal? {
        return savingsGoalDao.getGoalById(goalId)
    }
}