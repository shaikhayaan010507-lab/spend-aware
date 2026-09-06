package com.ayaansaksham.spend_aware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentMonthTotal = MutableStateFlow(0.0)
    val currentMonthTotal: StateFlow<Double> = _currentMonthTotal.asStateFlow()

    init {
        refreshCurrentMonthTotal()
    }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
            refreshCurrentMonthTotal()
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            refreshCurrentMonthTotal()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            refreshCurrentMonthTotal()
        }
    }

    private fun refreshCurrentMonthTotal() {
        viewModelScope.launch {
            _currentMonthTotal.value = repository.getCurrentMonthTotal()
        }
    }
}

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}