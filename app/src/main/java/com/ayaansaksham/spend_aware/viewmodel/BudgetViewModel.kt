package com.ayaansaksham.spend_aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.repository.BudgetRepository
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import com.ayaansaksham.spend_aware.data.repository.UserPreferencesRepository
import com.ayaansaksham.spend_aware.domain.logic.BurnRateCalculator
import com.ayaansaksham.spend_aware.domain.logic.BurnRateResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val activeProfileId = userPreferencesRepository.activeProfileIdFlow

    val allBudgets: StateFlow<List<Budget>> = activeProfileId
        .flatMapLatest { profileId ->
            budgetRepository.getAllBudgets(profileId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _burnRates = MutableStateFlow<Map<Int?, BurnRateResult>>(emptyMap())
    val burnRates: StateFlow<Map<Int?, BurnRateResult>> = _burnRates.asStateFlow()

    init {
        refreshBurnRates()
    }

    fun refreshBurnRates() {
        viewModelScope.launch {
            val budgets = allBudgets.first()
            val results = mutableMapOf<Int?, BurnRateResult>()
            for (budget in budgets) {
                val result = BurnRateCalculator.calculateForBudget(budget, expenseRepository)
                results[budget.categoryId] = result
            }
            _burnRates.value = results
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            val currentProfileId = userPreferencesRepository.activeProfileIdFlow.first()
            budgetRepository.insertBudget(budget.copy(profileId = currentProfileId))
            refreshBurnRates()
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
            refreshBurnRates()
        }
    }
}

class BudgetViewModelFactory(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(budgetRepository, expenseRepository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}