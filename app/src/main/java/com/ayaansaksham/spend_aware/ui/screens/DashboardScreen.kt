package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import com.ayaansaksham.spend_aware.domain.logic.BurnRateResult
import com.ayaansaksham.spend_aware.domain.logic.BurnRateStatus
import com.ayaansaksham.spend_aware.domain.logic.FinancialHealthCalculator
import com.ayaansaksham.spend_aware.domain.logic.SpendingBehaviorClassifier
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    expenses: StateFlow<List<Expense>>,
    categories: StateFlow<List<Category>>,
    budgets: StateFlow<List<Budget>>,
    burnRates: StateFlow<Map<Int?, BurnRateResult>>,
    // Restored Navigation Callbacks
    onAddExpenseClick: () -> Unit,
    onViewHistoryClick: () -> Unit,
    onManageBudgetClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onManageGoalsClick: () -> Unit
) {
    val allExpenses by expenses.collectAsState()
    val allCategories by categories.collectAsState()
    val allBudgets by budgets.collectAsState()
    val calculatedBurnRates by burnRates.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        // Modern Floating Action Button for Adding Expenses
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpenseClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Overall Monthly Budget",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            val overallBudget = allBudgets.find { it.categoryId == null }
            val overallBurnRate = calculatedBurnRates[null]

            if (overallBudget != null && overallBurnRate != null) {
                BudgetCard(budget = overallBudget, burnRate = overallBurnRate)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "No overall budget set.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            IntelligenceSection(
                expenses = allExpenses,
                categories = allCategories,
                budgets = allBudgets,
                burnRates = calculatedBurnRates
            )

            Spacer(modifier = Modifier.height(32.dp))

            // The New Quick Actions Row
            Text(
                text = "Quick Actions",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionButton(icon = Icons.Default.List, label = "History", onClick = onViewHistoryClick)
                QuickActionButton(icon = Icons.Default.DateRange, label = "Budgets", onClick = onManageBudgetClick)
                QuickActionButton(icon = Icons.Default.Settings, label = "Categories", onClick = onManageCategoriesClick)
                QuickActionButton(icon = Icons.Default.Star, label = "Goals", onClick = onManageGoalsClick)
            }

            Spacer(modifier = Modifier.height(80.dp)) // Extra space so FAB doesn't block content
        }
    }
}

// Reusable component for the tiny action buttons
@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun BudgetCard(budget: Budget, burnRate: BurnRateResult) {
    val statusColor = when (burnRate.status) {
        BurnRateStatus.UNDER_BUDGET -> Color(0xFF4CAF50)
        BurnRateStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
        BurnRateStatus.AT_RISK -> Color(0xFFFF9800)
        BurnRateStatus.OVER_BUDGET -> Color(0xFFF44336)
    }

    val statusText = when (burnRate.status) {
        BurnRateStatus.UNDER_BUDGET -> "Under budget"
        BurnRateStatus.ON_TRACK -> "On track"
        BurnRateStatus.AT_RISK -> "At risk"
        BurnRateStatus.OVER_BUDGET -> "Over budget"
    }

    val budgetLimit = budget.limitAmount
    val amountSpent = burnRate.amountSpent

    val percentageUsed = if (budgetLimit > 0) (amountSpent / budgetLimit) * 100 else 0.0
    val progress = if (budgetLimit > 0) (amountSpent / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%.2f", budgetLimit)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap to edit",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.2f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$statusText · ${percentageUsed.toInt()}% used",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = statusColor
                )
                Text(
                    text = "${burnRate.daysRemaining}d left",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IntelligenceSection(
    expenses: List<Expense>,
    categories: List<Category>,
    budgets: List<Budget>,
    burnRates: Map<Int?, BurnRateResult>
) {
    val healthScore = remember(expenses, budgets, burnRates) {
        FinancialHealthCalculator.calculateScore(budgets, burnRates, expenses)
    }

    val behaviors = remember(expenses, categories) {
        SpendingBehaviorClassifier.classify(expenses, categories)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Financial Intelligence",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                healthScore.score >= 80 -> Color(0xFF4CAF50)
                                healthScore.score >= 50 -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${healthScore.score}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = healthScore.rating,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = healthScore.summary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (behaviors.isNotEmpty()) {
            Text(
                text = "Spending Profile",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                behaviors.forEach { trait ->
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = trait.title,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}