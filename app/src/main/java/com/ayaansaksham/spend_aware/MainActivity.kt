package com.ayaansaksham.spend_aware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ayaansaksham.spend_aware.data.local.AppDatabase
import com.ayaansaksham.spend_aware.data.repository.BudgetRepository
import com.ayaansaksham.spend_aware.data.repository.CategoryRepository
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import com.ayaansaksham.spend_aware.data.repository.SavingsGoalRepository
import com.ayaansaksham.spend_aware.ui.Screen
import com.ayaansaksham.spend_aware.ui.screens.AddExpenseScreen
import com.ayaansaksham.spend_aware.ui.screens.AnalyticsScreen
import com.ayaansaksham.spend_aware.ui.screens.BudgetScreen
import com.ayaansaksham.spend_aware.ui.screens.CategoryScreen
import com.ayaansaksham.spend_aware.ui.screens.DashboardScreen
import com.ayaansaksham.spend_aware.ui.screens.SavingsGoalScreen
import com.ayaansaksham.spend_aware.ui.screens.SignUpScreen
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
import androidx.compose.ui.unit.dp

// Data class to define the bottom navigation tabs
data class BottomNavItem(val title: String, val icon: ImageVector, val route: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendawareTheme {
                val database = remember { AppDatabase.getDatabase(applicationContext) }

                val userSettingsRepository = remember { com.ayaansaksham.spend_aware.data.repository.UserSettingsRepository(database.userSettingsDao()) }

                val expenseRepository = remember { ExpenseRepository(database.expenseDao()) }
                val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(expenseRepository, userSettingsRepository))

                val categoryRepository = remember { CategoryRepository(database.categoryDao()) }
                val categoryViewModel: CategoryViewModel = viewModel(factory = CategoryViewModelFactory(categoryRepository, userSettingsRepository))

                val budgetRepository = remember { BudgetRepository(database.budgetDao()) }
                val budgetViewModel: BudgetViewModel = viewModel(factory = BudgetViewModelFactory(budgetRepository, expenseRepository, userSettingsRepository))

                val savingsGoalRepository = remember { SavingsGoalRepository(database.savingsGoalDao()) }
                val savingsGoalViewModel: SavingsGoalViewModel = viewModel(factory = SavingsGoalViewModelFactory(savingsGoalRepository, userSettingsRepository))

                val navController = rememberNavController()

                // Track current screen to hide/show bottom bar
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Map tabs to your existing Screen routes
                val bottomNavItems = listOf(
                    BottomNavItem("Home", Icons.Default.Home, Screen.Dashboard.route),
                    BottomNavItem("Expenses", Icons.Default.CalendarMonth, Screen.ExpenseHistory.route),
                    BottomNavItem("Budget", Icons.Default.AccountBalanceWallet, Screen.Budget.route),
                    BottomNavItem("Analytics", Icons.Default.Analytics, Screen.Analytics.route),
                    BottomNavItem("Settings", Icons.Default.Settings, Screen.Settings.route)
                )

                // Only show bottom bar on main tabs (hide on SignUp and AddExpense)
                val showBottomBar = bottomNavItems.any { it.route == currentRoute }

                // --- DEMO DATA INJECTION ---
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val existingCategories = categoryViewModel.allCategories.first()
                    if (existingCategories.isEmpty()) {
                        // Insert Categories (DailyBudget Spec)
                        val c1 = com.ayaansaksham.spend_aware.data.local.entity.Category(categoryId = 1, name = "Food", colorHex = "#00685D", isDefault = true)
                        val c2 = com.ayaansaksham.spend_aware.data.local.entity.Category(categoryId = 2, name = "Transport", colorHex = "#F89C00", isDefault = true)
                        val c3 = com.ayaansaksham.spend_aware.data.local.entity.Category(categoryId = 3, name = "Shopping", colorHex = "#6D7A77", isDefault = true)
                        val c4 = com.ayaansaksham.spend_aware.data.local.entity.Category(categoryId = 4, name = "Entertainment", colorHex = "#875200", isDefault = true)
                        val c5 = com.ayaansaksham.spend_aware.data.local.entity.Category(categoryId = 5, name = "Bills & Utilities", colorHex = "#B02528", isDefault = true)
                        val c6 = com.ayaansaksham.spend_aware.data.local.entity.Category(categoryId = 6, name = "Health & Fitness", colorHex = "#008376", isDefault = true)
                        val c7 = com.ayaansaksham.spend_aware.data.local.entity.Category(categoryId = 7, name = "Groceries", colorHex = "#16A34A", isDefault = true)
                        val c8 = com.ayaansaksham.spend_aware.data.local.entity.Category(categoryId = 8, name = "Other", colorHex = "#64748B", isDefault = true)
                        
                        categoryViewModel.addCategory(c1)
                        categoryViewModel.addCategory(c2)
                        categoryViewModel.addCategory(c3)
                        categoryViewModel.addCategory(c4)
                        categoryViewModel.addCategory(c5)
                        categoryViewModel.addCategory(c6)
                        categoryViewModel.addCategory(c7)
                        categoryViewModel.addCategory(c8)
                        
                        // Insert Master Budget
                        val startOfMonth = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.DAY_OF_MONTH, 1)
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        budgetViewModel.addBudget(
                            com.ayaansaksham.spend_aware.data.local.entity.Budget(
                                limitAmount = 60000.0,
                                periodType = "MONTHLY",
                                startDateMillis = startOfMonth,
                                categoryId = null
                            )
                        )
                        
                        // Insert Fixed Bills Budget
                        budgetViewModel.addBudget(
                            com.ayaansaksham.spend_aware.data.local.entity.Budget(
                                limitAmount = 25000.0, // Fixed cost
                                periodType = "MONTHLY",
                                startDateMillis = startOfMonth,
                                categoryId = 5, // Bills category
                                isFixed = true
                            )
                        )
                        
                        // --- END OF DEMO DATA INJECTION ---
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(tonalElevation = 8.dp) {
                                bottomNavItems.forEach { item ->
                                    NavigationBarItem(
                                        selected = currentRoute == item.route,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(item.icon, contentDescription = item.title) },
                                        label = { Text(item.title) },
                                        alwaysShowLabel = false
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (currentRoute == Screen.Dashboard.route || currentRoute == Screen.ExpenseHistory.route) {
                            FloatingActionButton(
                                onClick = { navController.navigate(Screen.AddExpense.route) },
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Expense")
                            }
                        }
                    },
                    floatingActionButtonPosition = FabPosition.End
                ) { innerPadding ->

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding) // This prevents content from hiding behind the bottom bar
                    ) {
                        composable(Screen.SignUp.route) {
                            SignUpScreen(
                                onSignUpSuccess = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.SignUp.route) { inclusive = true }
                                    }
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                userName = "Ayaan", // You can pull this from UserSettingsViewModel later
                                expenses = expenseViewModel.allExpenses,
                                budgets = budgetViewModel.allBudgets
                            )
                        }

                        composable(Screen.Analytics.route) {
                            AnalyticsScreen(
                                expenses = expenseViewModel.allExpenses,
                                categories = categoryViewModel.allCategories,
                                onNavigateBack = { navController.popBackStack() }
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
                                onUpdateCategory = { category -> categoryViewModel.updateCategory(category) },
                                onDeleteCategory = { category -> categoryViewModel.deleteCategory(category) },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.ExpenseHistory.route) {
                            TransactionHistoryScreen(
                                expenses = expenseViewModel.allExpenses,
                                categories = categoryViewModel.allCategories,
                                budgets = budgetViewModel.allBudgets,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Budget.route) {
                            BudgetScreen(
                                budgets = budgetViewModel.allBudgets,
                                categories = categoryViewModel.allCategories,
                                burnRates = budgetViewModel.burnRates,
                                onSaveBudget = { budget -> budgetViewModel.addBudget(budget) },
                                onRefreshBurnRates = { budgetViewModel.refreshBurnRates() },
                                onNavigateBack = { navController.popBackStack() },
                                expenses = expenseViewModel.allExpenses
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

                        composable(Screen.Settings.route) {
                            com.ayaansaksham.spend_aware.ui.screens.SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}