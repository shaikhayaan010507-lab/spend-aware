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
import com.ayaansaksham.spend_aware.data.repository.CategoryRepository
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import com.ayaansaksham.spend_aware.ui.Screen
import com.ayaansaksham.spend_aware.ui.screens.AddExpenseScreen
import com.ayaansaksham.spend_aware.ui.screens.CategoryScreen
import com.ayaansaksham.spend_aware.ui.screens.DashboardScreen
import com.ayaansaksham.spend_aware.ui.theme.SpendawareTheme
import com.ayaansaksham.spend_aware.viewmodel.CategoryViewModel
import com.ayaansaksham.spend_aware.viewmodel.CategoryViewModelFactory
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

                val categoryRepository = remember { CategoryRepository(database.categoryDao()) }
                val categoryViewModel: CategoryViewModel = viewModel(
                    factory = CategoryViewModelFactory(categoryRepository)
                )

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            currentMonthTotal = expenseViewModel.currentMonthTotal,
                            recentExpenses = expenseViewModel.allExpenses,
                            onAddExpenseClick = {
                                navController.navigate(Screen.AddExpense.route)
                            },
                            onManageCategoriesClick = {
                                navController.navigate(Screen.Categories.route)
                            }
                        )
                    }
                    composable(Screen.AddExpense.route) {
                        AddExpenseScreen(
                            onSaveExpense = { expense ->
                                expenseViewModel.addExpense(expense)
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(Screen.Categories.route) {
                        CategoryScreen(
                            categories = categoryViewModel.allCategories,
                            onAddCategory = { category ->
                                categoryViewModel.addCategory(category)
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}