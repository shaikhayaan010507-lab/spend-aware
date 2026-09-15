package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    expenses: StateFlow<List<Expense>>,
    categories: StateFlow<List<Category>>,
    onNavigateBack: () -> Unit
) {
    val allExpenses by expenses.collectAsState()
    val categoryList by categories.collectAsState()
    val categoryMap = remember(categoryList) { categoryList.associateBy { it.categoryId } }

    val totalSpent = remember(allExpenses) { allExpenses.sumOf { it.amount } }

    // Group spending by category
    val categorySpending = remember(allExpenses, categoryList) {
        allExpenses.groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending Analytics", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (allExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No data available for analytics",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Overview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Total Tracked Outflow", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%.2f", totalSpent)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Category Breakdown Donut Chart Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Category Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(
                                categorySpending = categorySpending,
                                categoryMap = categoryMap,
                                totalSpent = totalSpent
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Legend
                        categorySpending.forEach { (catId, amount) ->
                            val category = catId?.let { categoryMap[it] }
                            val catName = category?.name ?: "Uncategorized"
                            val colorInt = safeParseColor(category?.colorHex)
                            val catColor = if (colorInt != null) Color(colorInt) else MaterialTheme.colorScheme.primary
                            val percentage = if (totalSpent > 0) (amount / totalSpent * 100).toFloat() else 0f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = catName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", amount)} (${String.format(Locale.getDefault(), "%.1f", percentage)}%)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Spending By Day Bar Chart Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "SPENDING BY DAY", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            SpendingByDayBarChart(expenses = allExpenses)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    categorySpending: Map<Int?, Double>,
    categoryMap: Map<Int, Category>,
    totalSpent: Double
) {
    val fallbackColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.size(160.dp)) {
        var startAngle = -90f
        if (totalSpent <= 0.0) return@Canvas

        categorySpending.forEach { (catId, amount) ->
            val sweepAngle = ((amount / totalSpent) * 360f).toFloat()
            val category = catId?.let { categoryMap[it] }
            val colorInt = safeParseColor(category?.colorHex)
            val color = if (colorInt != null) Color(colorInt) else fallbackColor

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                size = androidx.compose.ui.geometry.Size(size.width, size.height),
                style = Stroke(width = 36f, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun SpendingByDayBarChart(expenses: List<Expense>) {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    // Find Monday of the current week
    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

    val weeklyExpenses = remember(expenses, startOfWeek) {
        val totals = FloatArray(7) { 0f }
        expenses.forEach { expense ->
            val expenseDate = Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).toLocalDate()
            if (!expenseDate.isBefore(startOfWeek) && expenseDate.isBefore(startOfWeek.plusDays(7))) {
                val dayIndex = expenseDate.dayOfWeek.value - 1 // 0 (Mon) to 6 (Sun)
                if (dayIndex in 0..6) {
                    totals[dayIndex] += expense.amount.toFloat()
                }
            }
        }
        totals
    }

    val maxAmount = weeklyExpenses.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
    val currentDayIndex = today.dayOfWeek.value - 1

    Box(modifier = Modifier.fillMaxSize()) {
        // Grid Lines
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp), // Space for text labels
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(4) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)
            }
        }

        // Bars and Labels
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            daysOfWeek.forEachIndexed { index, dayLabel ->
                val amount = weeklyExpenses[index]
                val heightFraction = if (maxAmount > 0f) amount / maxAmount else 0f
                val isToday = index == currentDayIndex

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (heightFraction > 0f || isToday) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(heightFraction.coerceAtLeast(0.08f)) // Tiny min-height for today if 0
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (isToday) Color(0xFF00796B) else Color(0xFFDCE4E9))
                            )
                        }
                    }
                    // Label
                    Text(
                        text = dayLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) Color(0xFF00796B) else Color.Gray
                    )
                }
            }
        }
    }
}

// Add this safeParseColor function at the bottom
fun safeParseColor(colorString: String?): Int? {
    if (colorString.isNullOrEmpty()) return null
    return try {
        android.graphics.Color.parseColor(colorString)
    } catch (_: IllegalArgumentException) {
        null
    }
}