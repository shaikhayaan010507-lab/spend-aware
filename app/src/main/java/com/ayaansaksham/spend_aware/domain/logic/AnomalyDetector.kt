package com.ayaansaksham.spend_aware.domain.logic

import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlin.math.sqrt

data class AnomalyResult(
    val expense: Expense,
    val deviationFactor: Double, // How many times larger than normal
    val reason: String
)

object AnomalyDetector {

    private const val ANOMALY_MULTIPLIER = 2.5 // Expenses 2.5x above normal are flagged

    fun detectAnomalies(expenses: List<Expense>): List<AnomalyResult> {
        if (expenses.size < 4) return emptyList() // Need enough data points for statistical relevance

        val amounts = expenses.map { it.amount }
        val mean = amounts.average()

        // Calculate standard deviation
        val variance = amounts.sumOf { (it - mean) * (it - mean) } / expenses.size
        val standardDeviation = sqrt(variance)

        val anomalies = mutableListOf<AnomalyResult>()

        for (expense in expenses) {
            // An expense is anomalous if it exceeds the mean by a significant margin
            // and is also strictly greater than (mean + 2 * standardDeviation)
            if (standardDeviation > 0 && expense.amount > (mean + (ANOMALY_MULTIPLIER * standardDeviation))) {
                val deviation = expense.amount / mean
                anomalies.add(
                    AnomalyResult(
                        expense = expense,
                        deviationFactor = deviation,
                        reason = "Unusually high outflow compared to your historical spending average (₹${String.format("%.2f", mean)})."
                    )
                )
            } else if (standardDeviation == 0.0 && expense.amount > mean * 2) {
                // Fallback if all past expenses were identical amounts
                anomalies.add(
                    AnomalyResult(
                        expense = expense,
                        deviationFactor = expense.amount / (if (mean > 0) mean else 1.0),
                        reason = "Significantly higher than your standard uniform transaction amount."
                    )
                )
            }
        }

        return anomalies
    }
}