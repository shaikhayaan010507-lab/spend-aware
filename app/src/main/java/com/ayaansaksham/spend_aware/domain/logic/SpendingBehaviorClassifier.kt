package com.ayaansaksham.spend_aware.domain.logic

import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import java.util.Calendar

data class BehaviorTrait(
    val title: String,
    val description: String,
    val iconName: String
)

object SpendingBehaviorClassifier {

    const val WEEKEND_SPEND_RATIO_THRESHOLD = 0.45
    const val FOOD_SPEND_RATIO_THRESHOLD = 0.40
    const val FREQUENT_SMALL_TICKET_THRESHOLD = 150.0
    const val CONSERVATIVE_SPEND_THRESHOLD = 2000.0

    fun classify(expenses: List<Expense>, categories: List<Category>): List<BehaviorTrait> {
        if (expenses.isEmpty()) return emptyList()

        val traits = mutableListOf<BehaviorTrait>()
        val totalSpent = expenses.sumOf { it.amount }
        val totalCount = expenses.size

        // 1. Weekend Spender Check
        val weekendSpent = expenses.filter { expense ->
            val cal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        }.sumOf { it.amount }

        if (totalSpent > 0 && (weekendSpent / totalSpent) >= WEEKEND_SPEND_RATIO_THRESHOLD) {
            traits.add(
                BehaviorTrait(
                    title = "Weekend Spender",
                    description = "Over 45% of your total outflows occur on Saturdays and Sundays.",
                    iconName = "weekend"
                )
            )
        }

        // 2. High Food Spender Check
        val foodCategoryIds = categories.filter {
            it.name.contains("food", ignoreCase = true) ||
                    it.name.contains("restaurant", ignoreCase = true) ||
                    it.name.contains("dining", ignoreCase = true)
        }.map { it.categoryId }

        val foodSpent = expenses.filter { it.categoryId in foodCategoryIds }.sumOf { it.amount }
        if (totalSpent > 0 && (foodSpent / totalSpent) >= FOOD_SPEND_RATIO_THRESHOLD) {
            traits.add(
                BehaviorTrait(
                    title = "High Food Spender",
                    description = "Food and dining make up more than 40% of your expenses.",
                    iconName = "restaurant"
                )
            )
        }

        // 3. Frequent Small Spender Check
        val averageExpense = totalSpent / totalCount
        if (totalCount >= 5 && averageExpense < FREQUENT_SMALL_TICKET_THRESHOLD) {
            traits.add(
                BehaviorTrait(
                    title = "Frequent Small Spender",
                    description = "You log frequent micro-transactions with low average ticket sizes.",
                    iconName = "shopping_cart"
                )
            )
        }

        // 4. Conservative Spender Check
        if (totalCount <= 5 && totalSpent < CONSERVATIVE_SPEND_THRESHOLD) {
            traits.add(
                BehaviorTrait(
                    title = "Conservative Spender",
                    description = "Your tracked outflows are minimal and tightly controlled.",
                    iconName = "savings"
                )
            )
        }

        // Fallback if no specific trait dominates
        if (traits.isEmpty()) {
            traits.add(
                BehaviorTrait(
                    title = "Balanced Spender",
                    description = "Your spending distribution is currently well-balanced across categories.",
                    iconName = "balance"
                )
            )
        }

        return traits
    }
}