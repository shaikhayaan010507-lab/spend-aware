package com.ayaansaksham.spend_aware.domain.logic

import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.repository.ExpenseRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class BurnRateStatus {
    UNDER_BUDGET,
    ON_TRACK,
    AT_RISK,
    OVER_BUDGET
}

data class BurnRateResult(
    val totalBudget: Double,
    val amountSpent: Double,
    val amountRemaining: Double,
    val percentageUsed: Float,
    val expectedPercentage: Float,
    val daysElapsed: Int,
    val daysInPeriod: Int,
    val daysRemaining: Int,
    val dailyAverageSpend: Double,
    val projectedTotalSpend: Double,
    val status: BurnRateStatus
)

object BurnRateCalculator {

    private const val AT_RISK_THRESHOLD = 10f

    fun calculate(
        budget: Budget,
        amountSpent: Double,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): BurnRateResult {
        val periodEndMillis = calculatePeriodEnd(budget.startDateMillis, budget.periodType)

        val totalDurationMillis = (periodEndMillis - budget.startDateMillis).coerceAtLeast(1L)
        val daysInPeriod = TimeUnit.MILLISECONDS.toDays(totalDurationMillis).toInt().coerceAtLeast(1)

        val elapsedMillis = (currentTimeMillis - budget.startDateMillis).coerceAtLeast(0L)
        val rawDaysElapsed = (TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt() + 1)
        val daysElapsed = rawDaysElapsed.coerceIn(1, daysInPeriod)
        val daysRemaining = (daysInPeriod - daysElapsed).coerceAtLeast(0)

        val percentageUsed = if (budget.limitAmount > 0.0) {
            ((amountSpent / budget.limitAmount) * 100).toFloat()
        } else 0f

        val expectedPercentage = ((daysElapsed.toDouble() / daysInPeriod) * 100).toFloat()
        val dailyAverageSpend = amountSpent / daysElapsed
        val projectedTotalSpend = dailyAverageSpend * daysInPeriod
        val amountRemaining = budget.limitAmount - amountSpent

        val status = when {
            amountSpent > budget.limitAmount -> BurnRateStatus.OVER_BUDGET
            percentageUsed > (expectedPercentage + AT_RISK_THRESHOLD) -> BurnRateStatus.AT_RISK
            percentageUsed <= expectedPercentage -> BurnRateStatus.UNDER_BUDGET
            else -> BurnRateStatus.ON_TRACK
        }

        return BurnRateResult(
            totalBudget = budget.limitAmount,
            amountSpent = amountSpent,
            amountRemaining = amountRemaining,
            percentageUsed = percentageUsed,
            expectedPercentage = expectedPercentage,
            daysElapsed = daysElapsed,
            daysInPeriod = daysInPeriod,
            daysRemaining = daysRemaining,
            dailyAverageSpend = dailyAverageSpend,
            projectedTotalSpend = projectedTotalSpend,
            status = status
        )
    }

    suspend fun calculateForBudget(
        budget: Budget,
        expenseRepository: ExpenseRepository,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): BurnRateResult {
        val periodEndMillis = calculatePeriodEnd(budget.startDateMillis, budget.periodType)

        // Query the total spent using the full period end to prevent microsecond dropouts
        val amountSpent = if (budget.categoryId == null) {
            expenseRepository.getTotalSpentBetweenDates(budget.startDateMillis, periodEndMillis, budget.profileId)
        } else {
            expenseRepository.getTotalSpentByCategoryBetweenDates(
                budget.categoryId,
                budget.startDateMillis,
                periodEndMillis,
                budget.profileId
            )
        }

        return calculate(budget, amountSpent, currentTimeMillis)
    }

    private fun calculatePeriodEnd(startDateMillis: Long, periodType: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
        }

        when (periodType.uppercase()) {
            "WEEKLY" -> calendar.add(Calendar.DAY_OF_YEAR, 7)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }

        return calendar.timeInMillis
    }
}