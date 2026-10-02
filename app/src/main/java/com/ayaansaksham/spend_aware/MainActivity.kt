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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import com.ayaansaksham.spend_aware.data.repository.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
                val userPreferencesRepository = remember { com.ayaansaksham.spend_aware.data.repository.UserPreferencesRepository(applicationContext.dataStore) }

                val splashViewModel: com.ayaansaksham.spend_aware.viewmodel.SplashViewModel = viewModel(factory = com.ayaansaksham.spend_aware.viewmodel.SplashViewModelFactory(userPreferencesRepository))
                val splashState by splashViewModel.splashState.collectAsState()

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

                if (splashState is com.ayaansaksham.spend_aware.viewmodel.SplashState.Loading) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        // Empty loading screen
                    }
                } else {
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
                        startDestination = if (splashState is com.ayaansaksham.spend_aware.viewmodel.SplashState.NeedsSetup) Screen.ProfileSetup.route else Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding) // This prevents content from hiding behind the bottom bar
                    ) {
                        composable(Screen.ProfileSetup.route) {
                            com.ayaansaksham.spend_aware.ui.screens.ProfileSetupScreen(
                                onSaveName = { name -> splashViewModel.saveUserName(name) },
                                onNameSaved = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                                    }
                                }
                            )
                        }
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
                            val userSettings by userSettingsRepository.settings.collectAsState(initial = null)
                            val activeProfileId = userSettings?.activeProfileId ?: 1

                            AddExpenseScreen(
                                categories = categoryViewModel.allCategories,
                                budgets = budgetViewModel.allBudgets,
                                expenses = expenseViewModel.allExpenses,
                                currentProfileId = activeProfileId,
                                onSaveExpense = { expense -> expenseViewModel.addExpense(expense) },
                                onAddCategory = { category -> categoryViewModel.addCategory(category) },
                                onCheckAnomaly = { expense, dailyLimit, catName ->
                                    expenseViewModel.checkAnomaly(expense, dailyLimit, catName)
                                },
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
                                onDeleteExpense = { expense -> expenseViewModel.deleteExpense(expense) },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Budget.route) {
                            val userSettings by userSettingsRepository.settings.collectAsState(initial = null)
                            val activeProfileId = userSettings?.activeProfileId ?: 1
                            BudgetScreen(
                                budgets = budgetViewModel.allBudgets,
                                categories = categoryViewModel.allCategories,
                                burnRates = budgetViewModel.burnRates,
                                onSaveBudget = { budget -> budgetViewModel.addBudget(budget) },
                                onRefreshBurnRates = { budgetViewModel.refreshBurnRates() },
                                onNavigateBack = { navController.popBackStack() },
                                expenses = expenseViewModel.allExpenses,
                                currentProfileId = activeProfileId
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
                            val userSettings by userSettingsRepository.settings.collectAsState(initial = null)
                            com.ayaansaksham.spend_aware.ui.screens.SettingsScreen(
                                currentProfileId = userSettings?.activeProfileId ?: 1,
                                onProfileSelected = { newProfileId ->
                                    val currentSettings = userSettings ?: com.ayaansaksham.spend_aware.data.local.entity.UserSettings(id = 1, firebaseUid = "local_only", activeProfileId = 1)
                                    CoroutineScope(Dispatchers.IO).launch {
                                        userSettingsRepository.saveSettings(currentSettings.copy(activeProfileId = newProfileId))
                                    }
                                }
                            )
                        }
                    }
                }
                }
            }
        }
    }
}