package com.ayaansaksham.spend_aware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ayaansaksham.spend_aware.data.local.AppDatabase
import com.ayaansaksham.spend_aware.data.repository.BudgetRepository
import com.ayaansaksham.spend_aware.data.repository.CategoryRepository
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import com.ayaansaksham.spend_aware.data.repository.SavingsGoalRepository
import com.ayaansaksham.spend_aware.data.repository.UserPreferencesRepository
import com.ayaansaksham.spend_aware.data.repository.UserRepository
import com.ayaansaksham.spend_aware.data.repository.dataStore
import com.ayaansaksham.spend_aware.ui.Screen
import com.ayaansaksham.spend_aware.ui.screens.AddExpenseScreen
import com.ayaansaksham.spend_aware.ui.screens.AnalyticsScreen
import com.ayaansaksham.spend_aware.ui.screens.BudgetScreen
import com.ayaansaksham.spend_aware.ui.screens.CategoryScreen
import com.ayaansaksham.spend_aware.ui.screens.DashboardScreen
import com.ayaansaksham.spend_aware.ui.screens.LoginScreen
import com.ayaansaksham.spend_aware.ui.screens.ProfileSetupScreen
import com.ayaansaksham.spend_aware.ui.screens.SavingsGoalScreen
import com.ayaansaksham.spend_aware.ui.screens.SettingsScreen
import com.ayaansaksham.spend_aware.ui.screens.SignUpScreen
import com.ayaansaksham.spend_aware.ui.screens.TransactionHistoryScreen
import com.ayaansaksham.spend_aware.ui.theme.SpendawareTheme
import com.ayaansaksham.spend_aware.viewmodel.AuthViewModel
import com.ayaansaksham.spend_aware.viewmodel.AuthViewModelFactory
import com.ayaansaksham.spend_aware.viewmodel.BudgetViewModel
import com.ayaansaksham.spend_aware.viewmodel.BudgetViewModelFactory
import com.ayaansaksham.spend_aware.viewmodel.CategoryViewModel
import com.ayaansaksham.spend_aware.viewmodel.CategoryViewModelFactory
import com.ayaansaksham.spend_aware.viewmodel.ExpenseViewModel
import com.ayaansaksham.spend_aware.viewmodel.ExpenseViewModelFactory
import com.ayaansaksham.spend_aware.viewmodel.SavingsGoalViewModel
import com.ayaansaksham.spend_aware.viewmodel.SavingsGoalViewModelFactory
import com.ayaansaksham.spend_aware.viewmodel.SettingsViewModel
import com.ayaansaksham.spend_aware.viewmodel.SettingsViewModelFactory
import com.ayaansaksham.spend_aware.viewmodel.SplashState
import com.ayaansaksham.spend_aware.viewmodel.SplashViewModel
import com.ayaansaksham.spend_aware.viewmodel.SplashViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Data class to define the bottom navigation tabs
data class BottomNavItem(val title: String, val icon: ImageVector, val route: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userPreferencesRepository = remember { UserPreferencesRepository(applicationContext.dataStore) }
            val themeMode by userPreferencesRepository.themeModeFlow.collectAsState(initial = "System")
            
            val isDarkTheme = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            SpendawareTheme(darkTheme = isDarkTheme) {
                val database = remember { AppDatabase.getDatabase(applicationContext) }

                val splashViewModel: SplashViewModel = viewModel(factory = SplashViewModelFactory(userPreferencesRepository))
                val splashState by splashViewModel.splashState.collectAsState()

                val expenseRepository = remember { ExpenseRepository(database.expenseDao()) }
                val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(expenseRepository, userPreferencesRepository))

                val categoryRepository = remember { CategoryRepository(database.categoryDao()) }
                val categoryViewModel: CategoryViewModel = viewModel(factory = CategoryViewModelFactory(categoryRepository, userPreferencesRepository))

                val budgetRepository = remember { BudgetRepository(database.budgetDao()) }
                val budgetViewModel: BudgetViewModel = viewModel(factory = BudgetViewModelFactory(budgetRepository, expenseRepository, userPreferencesRepository))

                val savingsGoalRepository = remember { SavingsGoalRepository(database.savingsGoalDao()) }
                val savingsGoalViewModel: SavingsGoalViewModel = viewModel(factory = SavingsGoalViewModelFactory(savingsGoalRepository, userPreferencesRepository))

                val userRepository = remember { UserRepository(database.userDao()) }
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(
                        userRepository,
                        database.profileDao(),
                        database.categoryDao(),
                        database.budgetDao(),
                        userPreferencesRepository
                    )
                )

                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(userRepository, database.profileDao(), userPreferencesRepository)
                )

                val activeProfileId by userPreferencesRepository.activeProfileIdFlow.collectAsState(initial = 1)
                val storedUserName by userPreferencesRepository.userNameFlow.collectAsState(initial = "Ayaan")

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

                // Only show bottom bar on main tabs (hide on SignUp, Login, and AddExpense)
                val showBottomBar = bottomNavItems.any { it.route == currentRoute }

                if (splashState is SplashState.Loading) {
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
                            startDestination = when (splashState) {
                                is SplashState.NeedsAuth -> Screen.Login.route
                                is SplashState.NeedsSetup -> Screen.ProfileSetup.route
                                else -> Screen.Dashboard.route
                            },
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Login.route) {
                                LoginScreen(
                                    authViewModel = authViewModel,
                                    onLoginSuccess = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onNavigateToSignUp = {
                                        navController.navigate(Screen.SignUp.route)
                                    }
                                )
                            }
                            composable(Screen.SignUp.route) {
                                SignUpScreen(
                                    authViewModel = authViewModel,
                                    onSignUpSuccess = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onNavigateToLogin = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                            composable(Screen.ProfileSetup.route) {
                                ProfileSetupScreen(
                                    onSaveName = { name -> splashViewModel.saveUserName(name) },
                                    onNameSaved = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.Dashboard.route) {
                                val currentExpenses by expenseViewModel.allExpenses.collectAsState() // TEMP wipe - remove later
                                androidx.compose.runtime.LaunchedEffect(currentExpenses) { // TEMP wipe - remove later
                                    currentExpenses.forEach { expenseViewModel.deleteExpense(it) } // TEMP wipe - remove later
                                } // TEMP wipe - remove later

                                DashboardScreen(
                                    userName = storedUserName ?: "Ayaan",
                                    expenses = expenseViewModel.allExpenses,
                                    budgets = budgetViewModel.allBudgets
                                )
                            }

                            composable(Screen.Analytics.route) {
                                AnalyticsScreen(
                                    expenses = expenseViewModel.allExpenses,
                                    categories = categoryViewModel.allCategories,
                                    budgets = budgetViewModel.allBudgets,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Screen.AddExpense.route,
                                arguments = listOf(androidx.navigation.navArgument("expenseId") { type = androidx.navigation.NavType.StringType; nullable = true })
                            ) { backStackEntry ->
                                val expenseIdStr = backStackEntry.arguments?.getString("expenseId")
                                val expenseIdToEdit = expenseIdStr?.toIntOrNull()

                                AddExpenseScreen(
                                    categories = categoryViewModel.allCategories,
                                    budgets = budgetViewModel.allBudgets,
                                    expenses = expenseViewModel.allExpenses,
                                    currentProfileId = activeProfileId,
                                    expenseIdToEdit = expenseIdToEdit,
                                    onSaveExpense = { expense -> expenseViewModel.addExpense(expense) },
                                    onUpdateExpense = { expense -> expenseViewModel.updateExpense(expense) },
                                    onAddCategory = { category -> categoryViewModel.addCategory(category) },
                                    onCheckAnomaly = { expense, dailyLimit, catName, excludeId ->
                                        expenseViewModel.checkAnomaly(expense, dailyLimit, catName, excludeId)
                                    },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Categories.route) {
                                CategoryScreen(
                                    categories = categoryViewModel.allCategories,
                                    expenses = expenseViewModel.allExpenses,
                                    currentProfileId = activeProfileId,
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
                                    onEditExpense = { expense ->
                                        navController.navigate(Screen.AddExpense.createRoute(expense.expenseId))
                                    },
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
                                    expenses = expenseViewModel.allExpenses,
                                    currentProfileId = activeProfileId,
                                    savingsGoals = savingsGoalViewModel.allGoals,
                                    onAddSavingsGoal = { goal -> savingsGoalViewModel.addGoal(goal) },
                                    onUpdateSavingsGoal = { goal -> savingsGoalViewModel.updateGoal(goal) },
                                    onDeleteSavingsGoal = { goal -> savingsGoalViewModel.deleteGoal(goal) }
                                )
                            }

                            composable(Screen.SavingsGoals.route) {
                                SavingsGoalScreen(
                                    goals = savingsGoalViewModel.allGoals,
                                    onAddGoal = { goal -> savingsGoalViewModel.addGoal(goal) },
                                    onUpdateGoal = { goal -> savingsGoalViewModel.updateGoal(goal) },
                                    onDeleteGoal = { goal -> savingsGoalViewModel.deleteGoal(goal) },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                settingsViewModel = settingsViewModel,
                                expenses = expenseViewModel.allExpenses,
                                categories = categoryViewModel.allCategories,
                                onNavigateToCategories = {
                                    navController.navigate(Screen.Categories.route)
                                },
                                onLogout = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            userPreferencesRepository.clearSession()
                                        }
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
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