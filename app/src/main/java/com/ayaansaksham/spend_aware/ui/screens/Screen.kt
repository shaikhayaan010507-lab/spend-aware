package com.ayaansaksham.spend_aware.ui

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AddExpense : Screen("add_expense")
    object ExpenseHistory : Screen("expense_history")
    object Categories : Screen("categories")
    object Budget : Screen("budget")
    object SavingsGoals : Screen("savings_goals")
    object Settings : Screen("settings")
}