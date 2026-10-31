package com.ayaansaksham.spend_aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import com.ayaansaksham.spend_aware.data.repository.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val activeProfileId = userPreferencesRepository.activeProfileIdFlow

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
            val currentProfileId = userPreferencesRepository.activeProfileIdFlow.first()
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

    suspend fun checkAnomaly(
        expense: Expense,
        safeToSpendToday: Double,
        categoryName: String,
        excludeExpenseId: Int? = null
    ): com.ayaansaksham.spend_aware.domain.logic.RealTimeAnomalyResult {
        val currentProfileId = userPreferencesRepository.activeProfileIdFlow.first()
        
        val history = if (excludeExpenseId != null) {
            repository.getCategoryAmountsExcluding(expense.categoryId ?: -1, currentProfileId, excludeExpenseId)
        } else {
            repository.getCategoryAmounts(expense.categoryId ?: -1, currentProfileId)
        }
        
        return com.ayaansaksham.spend_aware.domain.logic.ExpenseAnomalyDetector.detect(
            amount = expense.amount,
            safeToSpendToday = safeToSpendToday,
            categoryHistory = history,
            ceiling = 10000.0,
            categoryName = categoryName
        )
    }
}

class ExpenseViewModelFactory(
    private val repository: ExpenseRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}