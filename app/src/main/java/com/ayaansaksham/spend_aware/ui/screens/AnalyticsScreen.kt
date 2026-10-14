package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    expenses: StateFlow<List<Expense>>,
    categories: StateFlow<List<Category>>,
    budgets: StateFlow<List<Budget>>? = null,
    onNavigateBack: () -> Unit
) {
    val allExpenses by expenses.collectAsState()
    val categoryList by categories.collectAsState()
    val budgetList by (budgets ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val categoryMap = remember(categoryList) { categoryList.associateBy { it.categoryId } }

    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val daysInMonth = YearMonth.of(today.year, today.month).lengthOfMonth()

    val currentMonthExpenses = allExpenses.filter {
        val expenseDate = Instant.ofEpochMilli(it.dateMillis).atZone(zoneId).toLocalDate()
        expenseDate.year == today.year && expenseDate.month == today.month
    }
    
    val lastMonth = today.minusMonths(1)
    val lastMonthExpenses = allExpenses.filter {
        val expenseDate = Instant.ofEpochMilli(it.dateMillis).atZone(zoneId).toLocalDate()
        expenseDate.year == lastMonth.year && expenseDate.month == lastMonth.month
    }

    val totalSpentCurrent = currentMonthExpenses.sumOf { it.amount }
    val totalSpentLast = lastMonthExpenses.sumOf { it.amount }
    
    val percentageDiff = if (totalSpentLast > 0) {
        ((totalSpentCurrent - totalSpentLast) / totalSpentLast * 100).toFloat()
    } else {
        0f
    }
    val diffText = if (percentageDiff > 0) {
        "↑ ${String.format(Locale.getDefault(), "%.1f", percentageDiff)}% vs last mo"
    } else if (percentageDiff < 0) {
        "↓ ${String.format(Locale.getDefault(), "%.1f", -percentageDiff)}% vs last mo"
    } else {
        "Same as last mo"
    }
    val diffColor = if (percentageDiff > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    // Compute Budget Cap
    val masterBudget = budgetList.find { it.categoryId == null }
    val fixedBudgets = budgetList.filter { it.isFixed }
    val totalIncome = masterBudget?.limitAmount ?: 0.0
    val totalFixed = fixedBudgets.sumOf { it.limitAmount }
    val monthlyCap = (totalIncome - totalFixed).coerceAtLeast(0.0)
    
    val consumptionPercent = if (monthlyCap > 0) (totalSpentCurrent / monthlyCap * 100).toFloat() else 0f
    val expectedByToday = if (monthlyCap > 0) (monthlyCap / daysInMonth) * today.dayOfMonth else 0.0
    
    // Group spending by category
    val categorySpending = remember(currentMonthExpenses, categoryList) {
        currentMonthExpenses.groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val sortedCategories = categorySpending.toList().sortedByDescending { it.second }
    val topCategory = sortedCategories.firstOrNull()
    val topCatName = topCategory?.first?.let { categoryMap[it]?.name } ?: "None"
    val topCatAmount = topCategory?.second ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charts", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Export */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Time Filter Tabs
            val filters = listOf(
                today.month.name.uppercase(Locale.getDefault()),
                lastMonth.month.name.uppercase(Locale.getDefault()),
                "Q3 TRENDS"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    val isSelected = filter == filters[0] // Only active month is selected for now
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Macro Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "TOTAL TRACKED OUTFLOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Text(text = "Cap: ₹${monthlyCap.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${totalSpentCurrent.toInt()}",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = diffText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Monthly Allocation Burn
                    Text("Monthly Allocation Burn", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val barColor = if (consumptionPercent >= 100f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    LinearProgressIndicator(
                        progress = { (consumptionPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${String.format(Locale.getDefault(), "%.1f", consumptionPercent)}% Consumed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = barColor)
                        Text("Expected by today: ₹${expectedByToday.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Category Distribution Donut Chart Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "CATEGORY DISTRIBUTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutChart(
                            categorySpending = categorySpending,
                            categoryMap = categoryMap,
                            totalSpent = totalSpentCurrent
                        )
                        // Center Hole Text
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOP SPENDER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(topCatName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("₹${topCatAmount.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Legend
                    if (sortedCategories.isEmpty()) {
                        Text("No spending this month.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    sortedCategories.forEach { (catId, amount) ->
                        val category = catId?.let { categoryMap[it] }
                        val catName = category?.name ?: "Uncategorized"
                        val colorInt = safeParseColor(category?.colorHex)
                        val catColor = if (colorInt != null) Color(colorInt) else MaterialTheme.colorScheme.primary
                        val percentage = if (totalSpentCurrent > 0) (amount / totalSpentCurrent * 100).toFloat() else 0f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
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
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = catName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${amount.toInt()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.1f", percentage)}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Daily Burn Rate Bar Chart Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "DAILY BURN RATE (ROLLING 7-DAY)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    val targetDailyLimit = if (daysInMonth > 0) monthlyCap / daysInMonth else 0.0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        SpendingByDayBarChart(
                            expenses = allExpenses,
                            targetDailyLimit = targetDailyLimit
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    val todaySpent = currentMonthExpenses.filter { 
                        Instant.ofEpochMilli(it.dateMillis).atZone(zoneId).toLocalDate() == today 
                    }.sumOf { it.amount }
                    
                    val dailyDiff = targetDailyLimit - todaySpent
                    val dailyStatusText = if (dailyDiff >= 0) "Today is ₹${dailyDiff.toInt()} under limit" else "Today is ₹${(-dailyDiff).toInt()} over limit"
                    val dailyStatusColor = if (dailyDiff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                    // Weekly average calculation
                    val startOfRollingWeek = today.minusDays(6)
                    val rollingWeekExpenses = allExpenses.filter {
                        val date = Instant.ofEpochMilli(it.dateMillis).atZone(zoneId).toLocalDate()
                        !date.isBefore(startOfRollingWeek) && !date.isAfter(today)
                    }.sumOf { it.amount }
                    val weeklyAverage = rollingWeekExpenses / 7.0

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(dailyStatusText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = dailyStatusColor)
                        Text("Weekly Avg: ₹${weeklyAverage.toInt()}/d", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Predictive Pacing Advisor Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Lightbulb, contentDescription = "Insight", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "PREDICTIVE PACING ADVISOR", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "You spend 32% more on Friday evenings. Consider allocating ₹500 from your flexible buffer for this weekend to stay on track.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { /* Apply Buffer Rule */ },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Buffer rule", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
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

    Canvas(modifier = Modifier.size(180.dp)) {
        var startAngle = -90f
        if (totalSpent <= 0.0) {
            drawArc(
                color = Color(0xFFE3F0F8),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                size = Size(size.width, size.height),
                style = Stroke(width = 48f, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        categorySpending.forEach { (catId, amount) ->
            val sweepAngle = ((amount / totalSpent) * 360f).toFloat()
            val category = catId?.let { categoryMap[it] }
            val colorInt = safeParseColor(category?.colorHex)
            val color = if (colorInt != null) Color(colorInt) else fallbackColor

            // Add slight padding between arcs
            val actualSweep = if (categorySpending.size > 1) sweepAngle - 2f else sweepAngle

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = actualSweep.coerceAtLeast(1f), // Avoid drawing nothing if very small
                useCenter = false,
                size = Size(size.width, size.height),
                style = Stroke(width = 48f, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun SpendingByDayBarChart(expenses: List<Expense>, targetDailyLimit: Double) {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    // Last 7 days including today (e.g., Tue to Mon)
    val rollingDays = (6 downTo 0).map { today.minusDays(it.toLong()) }
    
    val dailyTotals = remember(expenses, rollingDays) {
        rollingDays.map { date ->
            expenses.filter {
                Instant.ofEpochMilli(it.dateMillis).atZone(zoneId).toLocalDate() == date
            }.sumOf { it.amount }
        }
    }

    val maxAmount = (dailyTotals + targetDailyLimit).maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val daysOfWeek = rollingDays.map { it.dayOfWeek.name.take(1) } // e.g., "M", "T", "W"
    
    val targetColor = MaterialTheme.colorScheme.outlineVariant
    val overLimitColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    val underLimitColor = MaterialTheme.colorScheme.primary

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
        
        // Target Line
        Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)) {
            val targetY = size.height - (targetDailyLimit.toFloat() / maxAmount * size.height)
            drawLine(
                color = targetColor,
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }
        
        // Target Label
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)) {
            val targetYFraction = 1f - (targetDailyLimit.toFloat() / maxAmount)
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
                Spacer(modifier = Modifier.fillMaxHeight(targetYFraction))
                Text("Target: ₹${targetDailyLimit.toInt()}/d", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            }
        }

        // Bars and Labels
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            dailyTotals.forEachIndexed { index, amount ->
                val heightFraction = if (maxAmount > 0f) (amount.toFloat() / maxAmount) else 0f
                val isToday = index == 6
                val isOverLimit = amount > targetDailyLimit

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    if (isOverLimit) {
                        Text("₹${amount.toInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 4.dp))
                    }
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
                                    .fillMaxHeight(heightFraction.coerceAtLeast(0.05f))
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (isOverLimit) overLimitColor else if (isToday) underLimitColor else MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                    // Label
                    Text(
                        text = daysOfWeek[index],
                        fontSize = 12.sp,
                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun safeParseColor(colorString: String?): Int? {
    if (colorString.isNullOrEmpty()) return null
    return try {
        android.graphics.Color.parseColor(colorString)
    } catch (_: IllegalArgumentException) {
        null
    }
}