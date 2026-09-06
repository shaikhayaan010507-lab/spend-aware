package com.ayaansaksham.spend_aware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ayaansaksham.spend_aware.data.local.AppDatabase
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import com.ayaansaksham.spend_aware.ui.Screen
import com.ayaansaksham.spend_aware.ui.screens.AddExpenseScreen
import com.ayaansaksham.spend_aware.ui.theme.SpendawareTheme
import com.ayaansaksham.spend_aware.viewmodel.ExpenseViewModel
import com.ayaansaksham.spend_aware.viewmodel.ExpenseViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendawareTheme {
                val database = remember { AppDatabase.getDatabase(applicationContext) }
                val expenseRepository = remember { ExpenseRepository(database.expenseDao()) }
                val expenseViewModel: ExpenseViewModel = viewModel(
                    factory = ExpenseViewModelFactory(expenseRepository)
                )

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.AddExpense.route
                ) {
                    composable(Screen.AddExpense.route) {
                        AddExpenseScreen(
                            onSaveExpense = { expense ->
                                expenseViewModel.addExpense(expense)
                            },
                            onNavigateBack = {

                            }
                        )
                    }
                }
            }
        }
    }
}