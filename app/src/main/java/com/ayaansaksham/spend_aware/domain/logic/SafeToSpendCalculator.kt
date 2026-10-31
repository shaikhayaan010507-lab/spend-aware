package com.ayaansaksham.spend_aware.domain.logic

import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class SafeToSpendResult(
    val totalIncome: Double,
    val totalFixed: Double,
    val flexibleBudget: Double,
    val spentThisMonth: Double,
    val spentToday: Double,
    val monthlyPool: Double,
    val daysInMonth: Int,
    val daysRemaining: Int,
    val dailyLimit: Double,
    val progress: Float
)

object SafeToSpendCalculator {

    fun calculate(
        budgets: List<Budget>,
        expenses: List<Expense>,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
        zoneId: ZoneId = ZoneId.systemDefault(),
        savingsGoal: Double = 0.0
    ): SafeToSpendResult {
        val daysInMonth = YearMonth.of(today.year, today.month).lengthOfMonth()
        val daysRemaining = (daysInMonth - today.dayOfMonth + 1).coerceAtLeast(0)

        val masterBudget = budgets.find { it.categoryId == null }
        val fixedBudgets = budgets.filter { it.isFixed }
        val totalIncome = masterBudget?.limitAmount ?: 0.0
        val totalFixed = fixedBudgets.sumOf { it.limitAmount }
        val fixedCategoryIds = fixedBudgets.mapNotNull { it.categoryId }.toSet()

        val flexibleExpensesThisMonth = expenses.filter { expense ->
            val expenseDate = Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).toLocalDate()
            expenseDate.year == today.year && expenseDate.month == today.month && expense.categoryId !in fixedCategoryIds
        }
        val spentThisMonth = flexibleExpensesThisMonth.sumOf { it.amount }

        val todayExpenses = expenses.filter { expense ->
            val expenseDate = Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).toLocalDate()
            expenseDate == today && expense.categoryId !in fixedCategoryIds
        }
        val spentToday = todayExpenses.sumOf { it.amount }

        val flexibleBudget = (totalIncome - totalFixed - savingsGoal).coerceAtLeast(0.0)
        val monthlyPool = (flexibleBudget - spentThisMonth).coerceAtLeast(0.0)
        val dailyLimit = if (daysRemaining > 0) monthlyPool / daysRemaining else 0.0
        val progress = if (dailyLimit > 0) (spentToday.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f) else 0f

        return SafeToSpendResult(
            totalIncome = totalIncome,
            totalFixed = totalFixed,
            flexibleBudget = flexibleBudget,
            spentThisMonth = spentThisMonth,
            spentToday = spentToday,
            monthlyPool = monthlyPool,
            daysInMonth = daysInMonth,
            daysRemaining = daysRemaining,
            dailyLimit = dailyLimit,
            progress = progress
        )
    }
}