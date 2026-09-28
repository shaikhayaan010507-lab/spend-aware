package com.ayaansaksham.spend_aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import com.ayaansaksham.spend_aware.data.repository.UserSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val activeProfileId = userSettingsRepository.settings.flatMapLatest { settings ->
        kotlinx.coroutines.flow.flowOf(settings?.activeProfileId ?: 1)
    }

    val allExpenses: StateFlow<List<Expense>> = activeProfileId
        .flatMapLatest { profileId ->
            repository.getAllExpenses(profileId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            val currentProfileId = userSettingsRepository.settings.first()?.activeProfileId ?: 1
            repository.insertExpense(expense.copy(profileId = currentProfileId))
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }
}

class ExpenseViewModelFactory(
    private val repository: ExpenseRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository, userSettingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}