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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlinx.coroutines.flow.StateFlow
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

                // Trend Line Chart Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Spending Trend (Recent)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            TrendLineChart(expenses = allExpenses)
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
fun TrendLineChart(expenses: List<Expense>) {
    val dailySpending = remember(expenses) {
        expenses.groupBy { expense ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = expense.dateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }.mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedBy { it.first }
            .takeLast(7)
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.secondary

    if (dailySpending.isEmpty()) return

    val maxAmount = dailySpending.maxOfOrNull { it.second } ?: 1.0

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        val width = size.width
        val height = size.height
        val stepX = if (dailySpending.size > 1) width / (dailySpending.size - 1) else width

        val points = dailySpending.mapIndexed { index, entry ->
            val x = index * stepX
            val y = height - (entry.second / maxAmount * height * 0.8f).toFloat() - 10f
            Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 6f
            )
        }

        points.forEach { point ->
            drawCircle(
                color = pointColor,
                radius = 8f,
                center = point
            )
        }
    }
}