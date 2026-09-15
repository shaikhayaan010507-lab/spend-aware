package com.ayaansaksham.spend_aware.data.local.dao

import androidx.room.*
import com.ayaansaksham.spend_aware.data.local.entity.SavingsGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Insert
    suspend fun insertGoal(goal: SavingsGoal)

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)

    @Query("SELECT * FROM savings_goals WHERE profileId = :profileId ORDER BY targetDateMillis ASC")
    fun getAllGoals(profileId: Int): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE goalId = :goalId")
    suspend fun getGoalById(goalId: Int): SavingsGoal?
}