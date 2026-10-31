package com.ayaansaksham.spend_aware

import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import com.ayaansaksham.spend_aware.domain.logic.SafeToSpendCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SafeToSpendCalculatorTest {

    private val zoneId = ZoneId.of("UTC")

    @Test
    fun safe_to_spend_calculation_normal_month() {
        val today = LocalDate.of(2026, 10, 1) // 31 days in October, day 1 -> 31 days remaining
        val budgets = listOf(
            Budget(budgetId = 1, limitAmount = 3100.0, categoryId = null, startDateMillis = 0L)
        )
        val expenses = emptyList<Expense>()

        val result = SafeToSpendCalculator.calculate(
            budgets = budgets,
            expenses = expenses,
            today = today,
            zoneId = zoneId
        )

        assertEquals(3100.0, result.totalIncome, 0.001)
        assertEquals(0.0, result.totalFixed, 0.001)
        assertEquals(3100.0, result.flexibleBudget, 0.001)
        assertEquals(3100.0, result.monthlyPool, 0.001)
        assertEquals(31, result.daysRemaining)
        assertEquals(100.0, result.dailyLimit, 0.001)
    }

    @Test
    fun safe_to_spend_calculation_last_day_of_month() {
        val today = LocalDate.of(2026, 10, 31) // Last day -> 1 day remaining
        val budgets = listOf(
            Budget(budgetId = 1, limitAmount = 3100.0, categoryId = null, startDateMillis = 0L)
        )
        val expenses = emptyList<Expense>()

        val result = SafeToSpendCalculator.calculate(
            budgets = budgets,
            expenses = expenses,
            today = today,
            zoneId = zoneId
        )

        assertEquals(1, result.daysRemaining)
        assertEquals(3100.0, result.dailyLimit, 0.001)
    }

    @Test
    fun safe_to_spend_calculation_zero_income_returns_zero_limits() {
        val today = LocalDate.of(2026, 10, 15)
        val budgets = listOf(
            Budget(budgetId = 1, limitAmount = 0.0, categoryId = null, startDateMillis = 0L)
        )
        val expenses = emptyList<Expense>()

        val result = SafeToSpendCalculator.calculate(
            budgets = budgets,
            expenses = expenses,
            today = today,
            zoneId = zoneId
        )

        assertEquals(0.0, result.flexibleBudget, 0.001)
        assertEquals(0.0, result.monthlyPool, 0.001)
        assertEquals(0.0, result.dailyLimit, 0.001)
    }

    @Test
    fun fixed_costs_larger_than_income_results_in_zero_flexible_budget() {
        val today = LocalDate.of(2026, 10, 10)
        val budgets = listOf(
            Budget(budgetId = 1, limitAmount = 5000.0, categoryId = null, startDateMillis = 0L),
            Budget(budgetId = 2, limitAmount = 7000.0, categoryId = 1, isFixed = true, startDateMillis = 0L)
        )
        val expenses = emptyList<Expense>()

        val result = SafeToSpendCalculator.calculate(
            budgets = budgets,
            expenses = expenses,
            today = today,
            zoneId = zoneId
        )

        assertEquals(5000.0, result.totalIncome, 0.001)
        assertEquals(7000.0, result.totalFixed, 0.001)
        assertEquals(0.0, result.flexibleBudget, 0.001)
        assertEquals(0.0, result.monthlyPool, 0.001)
        assertEquals(0.0, result.dailyLimit, 0.001)
    }

    @Test
    fun spending_above_budget_coerces_monthly_pool_and_daily_limit_to_zero_no_negative_results() {
        val today = LocalDate.of(2026, 10, 15) // Oct 15 2026 = 1791936000000L in UTC
        val todayMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val budgets = listOf(
            Budget(budgetId = 1, limitAmount = 1000.0, categoryId = null, startDateMillis = 0L)
        )
        val expenses = listOf(
            Expense(expenseId = 1, amount = 1500.0, categoryId = 2, dateMillis = todayMillis)
        )

        val result = SafeToSpendCalculator.calculate(
            budgets = budgets,
            expenses = expenses,
            today = today,
            zoneId = zoneId
        )

        assertEquals(1000.0, result.flexibleBudget, 0.001)
        assertEquals(1500.0, result.spentThisMonth, 0.001)
        assertEquals(0.0, result.monthlyPool, 0.001)
        assertEquals(0.0, result.dailyLimit, 0.001)
        assertTrue(result.monthlyPool >= 0.0)
        assertTrue(result.dailyLimit >= 0.0)
    }
}