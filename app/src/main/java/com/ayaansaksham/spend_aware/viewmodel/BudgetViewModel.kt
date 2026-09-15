package com.ayaansaksham.spend_aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.repository.BudgetRepository
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import com.ayaansaksham.spend_aware.data.repository.UserSettingsRepository
import com.ayaansaksham.spend_aware.domain.logic.BurnRateCalculator
import com.ayaansaksham.spend_aware.domain.logic.BurnRateResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val repository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val activeProfileId = userSettingsRepository.settings.flatMapLatest { settings ->
        kotlinx.coroutines.flow.flowOf(settings?.activeProfileId ?: 1)
    }

    val allBudgets: StateFlow<List<Budget>> = activeProfileId
        .flatMapLatest { profileId ->
            repository.getAllBudgets(profileId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _burnRates = MutableStateFlow<Map<Int?, BurnRateResult>>(emptyMap())
    val burnRates: StateFlow<Map<Int?, BurnRateResult>> = _burnRates

    init {
        // Automatically recalculate if budgets OR expenses change
        viewModelScope.launch {
            launch {
                allBudgets.collect {
                    refreshBurnRates()
                }
            }
            // For expenses, we need to observe the flow from ExpenseRepository that uses the same profile.
            // But since ExpenseViewModel handles the active expenses, maybe we can just observe activeProfileId
            // and trigger a refresh. However, simpler to just observe the profile id and trigger refresh manually when needed from UI.
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            repository.insertBudget(budget.copy(profileId = userSettingsRepository.settings.first()?.activeProfileId ?: 1))
        }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            repository.updateBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun refreshBurnRates() {
        viewModelScope.launch {
            val profileId = userSettingsRepository.settings.first()?.activeProfileId ?: 1
            val budgets = repository.getAllBudgets(profileId).first()

            val results = budgets.map { budget ->
                async {
                    budget.categoryId to BurnRateCalculator.calculateForBudget(budget, expenseRepository)
                }
            }.awaitAll().toMap()

            _burnRates.value = results
        }
    }
}

class BudgetViewModelFactory(
    private val repository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(repository, expenseRepository, userSettingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}