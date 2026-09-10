package com.ayaansaksham.spend_aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.repository.BudgetRepository
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import com.ayaansaksham.spend_aware.domain.logic.BurnRateCalculator
import com.ayaansaksham.spend_aware.domain.logic.BurnRateResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val repository: BudgetRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    val allBudgets: StateFlow<List<Budget>> = repository.allBudgets
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
                repository.allBudgets.collect {
                    refreshBurnRates()
                }
            }
            launch {
                expenseRepository.allExpenses.collect {
                    refreshBurnRates()
                }
            }
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            repository.insertBudget(budget)
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
            val budgets = repository.allBudgets.first()

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
    private val expenseRepository: ExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(repository, expenseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}