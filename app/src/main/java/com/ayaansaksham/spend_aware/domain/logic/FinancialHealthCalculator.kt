package com.ayaansaksham.spend_aware.domain.logic

import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Expense

data class HealthScoreResult(
    val score: Int,
    val rating: String,
    val summary: String
)

object FinancialHealthCalculator {

    fun calculateScore(
        budgets: List<Budget>,
        burnRates: Map<Int?, BurnRateResult>,
        expenses: List<Expense>
    ): HealthScoreResult {

        // If there is no data, return a neutral starting score
        if (budgets.isEmpty() && expenses.isEmpty()) {
            return HealthScoreResult(100, "Excellent", "Start tracking your expenses to see your score.")
        }

        var score = 100.0

        // 1. Dynamic Budget Penalties (The Fix)
        burnRates.values.forEach { burnRate ->
            when (burnRate.status) {
                BurnRateStatus.AT_RISK -> {
                    score -= 8.0 // Minor warning penalty
                }
                BurnRateStatus.OVER_BUDGET -> {
                    // Massive Upgrade: Penalty scales with the overspend!
                    // 133% used = 33% over. Subtract 15 base points + 33 extra points = -48 points.
                    val overagePercentage = (burnRate.percentageUsed - 100f).coerceAtLeast(0f)
                    score -= (15.0 + overagePercentage)
                }
                else -> { /* No penalty for SAFE or ON_TRACK */ }
            }
        }

        // 2. Anomaly Penalties (Deduct points for massive outliers)
        val anomalies = AnomalyDetector.detectAnomalies(expenses)
        score -= (anomalies.size * 5.0) // -5 points per anomaly

        // Ensure score stays between 0 and 100
        val finalScore = score.toInt().coerceIn(0, 100)

        // 3. Dynamic Rating and Summary Generation
        val (rating, summary) = when {
            finalScore >= 80 -> Pair(
                "Excellent",
                "Your budget pacing and spending patterns are tightly controlled."
            )
            finalScore >= 50 -> Pair(
                "Needs Attention",
                "You are spending too quickly or frequently exceeding your limits."
            )
            else -> Pair(
                "Critical",
                "Your spending is severely over budget. Immediate adjustment is required."
            )
        }

        return HealthScoreResult(finalScore, rating, summary)
    }
}