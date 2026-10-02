package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    userName: String = "Ayaan",
    expenses: StateFlow<List<Expense>>? = null,
    budgets: StateFlow<List<Budget>>? = null
) {
    val expenseList by (expenses ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState(initial = emptyList())
    val budgetList by (budgets ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState(initial = emptyList())

    val today = LocalDate.now()
    val daysInMonth = YearMonth.of(today.year, today.month).lengthOfMonth()
    val daysRemaining = daysInMonth - today.dayOfMonth + 1
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd")
    val todayString = today.format(dateFormatter)
    val zoneId = ZoneId.systemDefault()

    val totalMonthlyBudget = budgetList.filter { it.categoryId == null }.sumOf { it.limitAmount }
    
    // Determine Fixed Commitments
    val fixedBudgets = budgetList.filter { it.isFixed }
    val fixedCategoryIds = fixedBudgets.mapNotNull { it.categoryId }.toSet()
    val totalFixedCommitments = fixedBudgets.sumOf { it.limitAmount }

    // Date-filtered flexible expense totals (ignores fixed bills)
    val flexibleExpensesThisMonth = expenseList.filter { expense ->
        val expenseDate = Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).toLocalDate()
        expenseDate.year == today.year && expenseDate.month == today.month && expense.categoryId !in fixedCategoryIds
    }
    val spentThisMonth = flexibleExpensesThisMonth.sumOf { it.amount }

    val todayFlexibleExpenses = expenseList.filter { expense ->
        val expenseDate = Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).toLocalDate()
        expenseDate == today && expense.categoryId !in fixedCategoryIds
    }
    val spentToday = todayFlexibleExpenses.sumOf { it.amount }

    // Math: Safe to Spend Pool = (Total Allowance - Fixed Commitments) - Spent Flexible Money
    val flexibleBudget = (totalMonthlyBudget - totalFixedCommitments).coerceAtLeast(0.0)
    val monthlyPool = (flexibleBudget - spentThisMonth).coerceAtLeast(0.0)
    val dailyLimit = if (daysRemaining > 0) monthlyPool / daysRemaining else 0.0
    val progress = if (dailyLimit > 0) (spentToday.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Good Afternoon, $userName 👋",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )
        Text(
            text = todayString,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SAFE TO SPEND TODAY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedContent(
                    targetState = dailyLimit.toInt(),
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                        } else {
                            slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
                        }
                    },
                    label = "dailyLimitAnimation"
                ) { targetCount ->
                    Text(
                        text = "₹$targetCount",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Spent: ₹${spentToday.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text("Remaining: ₹${(dailyLimit - spentToday).toInt().coerceAtLeast(0)}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(8.dp))

                val barColor = when {
                    progress >= 1f -> MaterialTheme.colorScheme.error   // Red (Danger)
                    progress >= 0.75f -> MaterialTheme.colorScheme.secondary // Amber (Warning)
                    else -> MaterialTheme.colorScheme.primary             // Green (Safe)
                }

                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    label = "ProgressBarAnim"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Days left", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = daysRemaining,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                            } else {
                                slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
                            }
                        },
                        label = "daysRemainingAnimation"
                    ) { targetCount ->
                        Text("$targetCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Remaining Allowance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = monthlyPool.toInt(),
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                            } else {
                                slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
                            }
                        },
                        label = "monthlyPoolAnimation"
                    ) { targetCount ->
                        Text("₹$targetCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Quick Summary",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )

        Spacer(modifier = Modifier.height(12.dp))
        
        val yesterday = today.minusDays(1)
        val spentYesterday = expenseList.filter { expense ->
            val expenseDate = Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).toLocalDate()
            expenseDate == yesterday && expense.categoryId !in fixedCategoryIds
        }.sumOf { it.amount }
        
        val diff = spentToday - spentYesterday
        val comparisonMessage = when {
            spentToday > spentYesterday -> "You've spent ₹${diff.toInt()} more than yesterday"
            spentToday < spentYesterday -> "You've spent ₹${(-diff).toInt()} less than yesterday"
            else -> "You've spent exactly the same as yesterday"
        }
        val comparisonColor = when {
            spentToday > spentYesterday -> MaterialTheme.colorScheme.error // Red (Bad)
            spentToday < spentYesterday -> MaterialTheme.colorScheme.primary // Teal (Good)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val comparisonIcon = when {
            spentToday > spentYesterday -> Icons.Rounded.ArrowUpward
            spentToday < spentYesterday -> Icons.Rounded.ArrowDownward
            else -> null
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today's Spent", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${spentToday.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (spentToday > dailyLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                    
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Yesterday", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${spentYesterday.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (comparisonIcon != null) {
                        Icon(
                            imageVector = comparisonIcon,
                            contentDescription = null,
                            tint = comparisonColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = comparisonMessage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = comparisonColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}