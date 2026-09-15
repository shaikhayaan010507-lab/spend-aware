package com.ayaansaksham.spend_aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.entity.SavingsGoal
import com.ayaansaksham.spend_aware.data.repository.SavingsGoalRepository
import com.ayaansaksham.spend_aware.data.repository.UserSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsGoalViewModel(
    private val repository: SavingsGoalRepository,
    userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val activeProfileId = userSettingsRepository.settings.flatMapLatest { settings ->
        kotlinx.coroutines.flow.flowOf(settings?.activeProfileId ?: 1)
    }

    val allGoals: StateFlow<List<SavingsGoal>> = activeProfileId
        .flatMapLatest { profileId ->
            repository.getAllGoals(profileId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addGoal(goal: SavingsGoal) {
        viewModelScope.launch { repository.insertGoal(goal) }
    }

    fun updateGoal(goal: SavingsGoal) {
        viewModelScope.launch { repository.updateGoal(goal) }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }
}

class SavingsGoalViewModelFactory(
    private val repository: SavingsGoalRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavingsGoalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavingsGoalViewModel(repository, userSettingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}