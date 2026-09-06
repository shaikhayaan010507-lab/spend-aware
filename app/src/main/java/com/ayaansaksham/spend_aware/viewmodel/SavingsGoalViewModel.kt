package com.ayaansaksham.spend_aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.entity.SavingsGoal
import com.ayaansaksham.spend_aware.data.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingsGoalViewModel(private val repository: SavingsGoalRepository) : ViewModel() {

    val allGoals: StateFlow<List<SavingsGoal>> = repository.allGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.insertGoal(goal)
        }
    }

    fun updateGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }
}

class SavingsGoalViewModelFactory(private val repository: SavingsGoalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavingsGoalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavingsGoalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}