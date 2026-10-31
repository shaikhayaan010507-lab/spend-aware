package com.ayaansaksham.spend_aware.domain.logic

sealed class RealTimeAnomalyResult {
    object None : RealTimeAnomalyResult()
    data class LargePercentage(val amount: Double, val percentage: Float) : RealTimeAnomalyResult()
    data class Outlier(
        val amount: Double,
        val multiplier: Float,
        val normalMin: Double,
        val normalMax: Double,
        val categoryName: String
    ) : RealTimeAnomalyResult()
}

object ExpenseAnomalyDetector {

    const val DEFAULT_CEILING_AMOUNT = 10000.0
    const val SAFE_TO_SPEND_PERCENTAGE_THRESHOLD = 50.0f

    fun detect(
        amount: Double,
        safeToSpendToday: Double,
        categoryHistory: List<Double>,
        ceiling: Double = DEFAULT_CEILING_AMOUNT,
        categoryName: String
    ): RealTimeAnomalyResult {
        // 1. Smart Check (IQR) if we have enough history
        if (categoryHistory.size >= 7) {
            val sorted = categoryHistory.sorted()
            val mid = sorted.size / 2
            val lowerHalf = sorted.take(mid)
            val upperHalf = sorted.takeLast(mid)

            val q1 = median(lowerHalf)
            val q3 = median(upperHalf)
            val iqr = q3 - q1
            
            // Outlier boundary is Q3 + 1.5 * IQR
            val upperFence = q3 + 1.5 * iqr
            val lowerFence = (q1 - 1.5 * iqr).coerceAtLeast(0.0)
            
            val usualMedian = median(sorted)

            if (amount > upperFence && upperFence > 0) {
                val multiplier = if (usualMedian > 0) (amount / usualMedian).toFloat() else 1f
                return RealTimeAnomalyResult.Outlier(amount, multiplier, lowerFence, upperFence, categoryName)
            }

            // If history exists, we USE THIS smarter check INSTEAD of the ceiling/percentage check.
            return RealTimeAnomalyResult.None
        } 
        // 2. Fallback Check (Ceiling OR 50% of Safe-to-Spend)
        else {
            val isAboveCeiling = amount > ceiling
            
            android.util.Log.d("ANOMALY_DEBUG", "Raw Expense Amount: $amount")
            android.util.Log.d("ANOMALY_DEBUG", "Safe To Spend Today (received): $safeToSpendToday")

            val percentage = if (safeToSpendToday > 0) (amount / safeToSpendToday * 100).toFloat() else 0f
            
            android.util.Log.d("ANOMALY_DEBUG", "Computed Percentage: $percentage%")

            val isAboveHalfSafe = safeToSpendToday > 0 && percentage > SAFE_TO_SPEND_PERCENTAGE_THRESHOLD

            if (isAboveCeiling || isAboveHalfSafe) {
                return RealTimeAnomalyResult.LargePercentage(amount, percentage)
            }

            return RealTimeAnomalyResult.None
        }
    }

    private fun median(list: List<Double>): Double {
        if (list.isEmpty()) return 0.0
        val mid = list.size / 2
        return if (list.size % 2 == 0) {
            (list[mid - 1] + list[mid]) / 2.0
        } else {
            list[mid]
        }
    }
}