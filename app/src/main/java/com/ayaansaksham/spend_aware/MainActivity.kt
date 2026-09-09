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
import com.ayaansaksham.spend_aware.data.repository.BudgetRepository
import com.ayaansaksham.spend_aware.data.repository.CategoryRepository
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import com.ayaansaksham.spend_aware.data.repository.SavingsGoalRepository
import com.ayaansaksham.spend_aware.ui.Screen
import com.ayaansaksham.spend_aware.ui.screens.AddExpenseScreen
import com.ayaansaksham.spend_aware.ui.screens.BudgetScreen
import com.ayaansaksham.spend_aware.ui.screens.CategoryScreen
import com.ayaansaksham.spend_aware.ui.screens.DashboardScreen
import com.ayaansaksham.spend_aware.ui.screens.SavingsGoalScreen
import com.ayaansaksham.spend_aware.ui.screens.TransactionHistoryScreen
import com.ayaansaksham.spend_aware.ui.theme.SpendawareTheme
import com.ayaansaksham.spend_aware.viewmodel.BudgetViewModel
import com.ayaansaksham.spend_aware.viewmodel.BudgetViewModelFactory
import com.ayaansaksham.spend_aware.viewmodel.CategoryViewModel
import com.ayaansaksham.spend_aware.viewmodel.CategoryViewModelFactory
import com.ayaansaksham.spend_aware.viewmodel.ExpenseViewModel
import com.ayaansaksham.spend_aware.viewmodel.ExpenseViewModelFactory
import com.ayaansaksham.spend_aware.viewmodel.SavingsGoalViewModel
import com.ayaansaksham.spend_aware.viewmodel.SavingsGoalViewModelFactory

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

                val budgetRepository = remember { BudgetRepository(database.budgetDao()) }
                val budgetViewModel: BudgetViewModel = viewModel(
                    factory = BudgetViewModelFactory(budgetRepository)
                )

                val savingsGoalRepository = remember { SavingsGoalRepository(database.savingsGoalDao()) }
                val savingsGoalViewModel: SavingsGoalViewModel = viewModel(
                    factory = SavingsGoalViewModelFactory(savingsGoalRepository)
                )

                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            currentMonthTotal = expenseViewModel.currentMonthTotal,
                            recentExpenses = expenseViewModel.allExpenses,
                            onAddExpenseClick = { navController.navigate(Screen.AddExpense.route) },
                            onManageCategoriesClick = { navController.navigate(Screen.Categories.route) },
                            onViewHistoryClick = { navController.navigate(Screen.ExpenseHistory.route) },
                            onManageBudgetClick = { navController.navigate(Screen.Budget.route) },
                            onManageGoalsClick = { navController.navigate(Screen.SavingsGoals.route) }
                        )
                    }
                    composable(Screen.AddExpense.route) {
                        AddExpenseScreen(
                            categories = categoryViewModel.allCategories,
                            onSaveExpense = { expense -> expenseViewModel.addExpense(expense) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Categories.route) {
                        CategoryScreen(
                            categories = categoryViewModel.allCategories,
                            onAddCategory = { category -> categoryViewModel.addCategory(category) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.ExpenseHistory.route) {
                        TransactionHistoryScreen(
                            expenses = expenseViewModel.allExpenses,
                            categories = categoryViewModel.allCategories,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Budget.route) {
                        BudgetScreen(
                            budgets = budgetViewModel.allBudgets,
                            categories = categoryViewModel.allCategories,
                            onSaveBudget = { budget -> budgetViewModel.addBudget(budget) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.SavingsGoals.route) {
                        SavingsGoalScreen(
                            goals = savingsGoalViewModel.allGoals,
                            onAddGoal = { goal -> savingsGoalViewModel.addGoal(goal) },
                            onUpdateGoal = { goal -> savingsGoalViewModel.updateGoal(goal) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}