package com.ayaansaksham.spend_aware.ui

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AddExpense : Screen("add_expense")
    object Categories : Screen("categories")
    object ExpenseHistory : Screen("expense_history")
    object Budget : Screen("budget")
    object SavingsGoals : Screen("savings_goals")
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object SignUp : Screen("sign_up")
    object ProfileSetup : Screen("profile_setup")
}