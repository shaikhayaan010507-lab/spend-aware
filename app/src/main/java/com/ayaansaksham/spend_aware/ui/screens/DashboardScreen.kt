package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SAFE TO SPEND TODAY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "₹${dailyLimit.toInt()}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1A1A)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Spent: ₹${spentToday.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00796B))
                    Text("Remaining: ₹${(dailyLimit - spentToday).toInt().coerceAtLeast(0)}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(8.dp))

                val barColor = when {
                    progress >= 1f -> Color(0xFFD32F2F)   // Red (Danger)
                    progress >= 0.75f -> Color(0xFFF57C00) // Amber (Warning)
                    else -> Color(0xFF00796B)             // Green (Safe)
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = barColor,
                    trackColor = Color(0xFFE0E0E0)
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Days left", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$daysRemaining", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Remaining Allowance", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${monthlyPool.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
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
            spentToday > spentYesterday -> "You've spent ₹${diff.toInt()} more than yesterday 📈"
            spentToday < spentYesterday -> "You've spent ₹${(-diff).toInt()} less than yesterday 📉"
            else -> "You've spent exactly the same as yesterday ➖"
        }
        val comparisonColor = when {
            spentToday > spentYesterday -> Color(0xFFD32F2F) // Red (Bad)
            spentToday < spentYesterday -> Color(0xFF00796B) // Green (Good)
            else -> Color.Gray
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today's Spent", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${spentToday.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (spentToday > dailyLimit) Color(0xFFD32F2F) else Color(0xFF00796B))
                    }
                    
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE0E0E0)))

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Yesterday", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${spentYesterday.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF0F4F8), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = comparisonMessage,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = comparisonColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}