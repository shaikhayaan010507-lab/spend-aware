package com.ayaansaksham.spend_aware

import com.ayaansaksham.spend_aware.domain.logic.ExpenseAnomalyDetector
import com.ayaansaksham.spend_aware.domain.logic.RealTimeAnomalyResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseAnomalyDetectorTest {

    @Test
    fun expense_above_half_of_daily_limit_is_flagged_as_large_percentage() {
        val result = ExpenseAnomalyDetector.detect(
            amount = 600.0,
            safeToSpendToday = 1000.0, // 600 is 60% of safe-to-spend (> 50%)
            categoryHistory = emptyList(),
            ceiling = 10000.0,
            categoryName = "Food"
        )

        assertTrue(result is RealTimeAnomalyResult.LargePercentage)
        val largePercentage = result as RealTimeAnomalyResult.LargePercentage
        assertEquals(600.0, largePercentage.amount, 0.001)
        assertEquals(60.0f, largePercentage.percentage, 0.001f)
    }

    @Test
    fun expense_below_half_of_daily_limit_returns_none() {
        val result = ExpenseAnomalyDetector.detect(
            amount = 400.0,
            safeToSpendToday = 1000.0, // 400 is 40% of safe-to-spend (< 50%)
            categoryHistory = emptyList(),
            ceiling = 10000.0,
            categoryName = "Food"
        )

        assertEquals(RealTimeAnomalyResult.None, result)
    }

    @Test
    fun expense_exactly_at_half_of_daily_limit_boundary_returns_none() {
        val result = ExpenseAnomalyDetector.detect(
            amount = 500.0,
            safeToSpendToday = 1000.0, // 500 is exactly 50% (not > 50%)
            categoryHistory = emptyList(),
            ceiling = 10000.0,
            categoryName = "Food"
        )

        assertEquals(RealTimeAnomalyResult.None, result)
    }

    @Test
    fun expense_exceeding_iqr_upper_fence_is_flagged_as_outlier() {
        // 7 history items trigger IQR check instead of fallback
        val history = listOf(100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0)
        // Q1 = 100, Q3 = 100, IQR = 0, Upper Fence = 100
        val result = ExpenseAnomalyDetector.detect(
            amount = 500.0,
            safeToSpendToday = 2000.0,
            categoryHistory = history,
            ceiling = 10000.0,
            categoryName = "Canteen"
        )

        assertTrue(result is RealTimeAnomalyResult.Outlier)
        val outlier = result as RealTimeAnomalyResult.Outlier
        assertEquals(500.0, outlier.amount, 0.001)
        assertEquals("Canteen", outlier.categoryName)
    }

    @Test
    fun expense_within_iqr_fences_returns_none() {
        val history = listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0)
        val result = ExpenseAnomalyDetector.detect(
            amount = 45.0,
            safeToSpendToday = 100.0,
            categoryHistory = history,
            ceiling = 10000.0,
            categoryName = "Commute"
        )

        assertEquals(RealTimeAnomalyResult.None, result)
    }

    @Test
    fun empty_or_small_history_uses_fallback_percentage_check() {
        // Less than 7 history items uses percentage fallback check
        val smallHistory = listOf(100.0, 100.0, 100.0)
        val result = ExpenseAnomalyDetector.detect(
            amount = 800.0,
            safeToSpendToday = 1000.0, // 80% > 50%
            categoryHistory = smallHistory,
            ceiling = 10000.0,
            categoryName = "Books"
        )

        assertTrue(result is RealTimeAnomalyResult.LargePercentage)
    }
}