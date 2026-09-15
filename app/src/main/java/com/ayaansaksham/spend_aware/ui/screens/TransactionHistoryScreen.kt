package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionHistoryScreen(
    expenses: StateFlow<List<Expense>>? = null,
    categories: StateFlow<List<Category>>? = null,
    budgets: StateFlow<List<com.ayaansaksham.spend_aware.data.local.entity.Budget>>? = null,
    onNavigateBack: () -> Unit = {}
) {
    val expenseList by (expenses ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val categoryList by (categories ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val budgetList by (budgets ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()

    // Toggle state: true = List View, false = Calendar View
    var isListView by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8)) // Slightly cool gray background for cleaner look
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Segmented Toggle Button (List vs Calendar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE2E8F0)) // Softer gray for unselected
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // List Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isListView) Color(0xFF00695C) else Color.Transparent) // Dark teal for selected
                    .clickable { isListView = true },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = "List", tint = if (isListView) Color.White else Color.DarkGray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Expense History", fontWeight = FontWeight.SemiBold, color = if (isListView) Color.White else Color.DarkGray, fontSize = 14.sp)
                }
            }

            // Calendar Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (!isListView) Color.White else Color.Transparent)
                    .clickable { isListView = false },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = if (!isListView) Color(0xFF1A1A1A) else Color.DarkGray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calendar View", fontWeight = FontWeight.SemiBold, color = if (!isListView) Color(0xFF1A1A1A) else Color.DarkGray, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Content Area
        if (isListView) {
            QuickStatsRow(expenseList, categoryList)
            Spacer(modifier = Modifier.height(16.dp))
            FiltersSection()
            Spacer(modifier = Modifier.height(16.dp))
            ExpenseListView(expenseList, categoryList)
        } else {
            ExpenseCalendarView(expenseList, budgetList)
        }
    }
}

@Composable
fun QuickStatsRow(expenseList: List<Expense>, categoryList: List<Category>) {
    val totalSpent = expenseList.sumOf { it.amount }
    val txCount = expenseList.size
    val avgSpend = if (txCount > 0) totalSpent / txCount else 0.0

    val categoryMap = categoryList.associateBy { it.categoryId }
    val categoryTotals = expenseList.groupBy { it.categoryId }.mapValues { entry -> entry.value.sumOf { it.amount } }
    val topCategoryId = categoryTotals.maxByOrNull { it.value }?.key
    val topCategoryName = topCategoryId?.let { categoryMap[it]?.name } ?: "None"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(title = "TOTAL SPENT", value = "₹${totalSpent.toInt()}", valueColor = Color(0xFFD32F2F))
        StatCard(title = "TRANSACTIONS", value = "$txCount logs", valueColor = Color(0xFF1A1A1A))
        StatCard(title = "AVERAGE / LOG", value = "₹${avgSpend.toInt()}", valueColor = Color(0xFF00796B))
        StatCard(title = "TOP CATEGORY", value = topCategoryName, valueColor = Color(0xFF1A1A1A))
    }
}

@Composable
fun StatCard(title: String, value: String, valueColor: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.widthIn(min = 120.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSection() {
    var searchQuery by remember { mutableStateOf("") }
    val periods = listOf("All Time", "Today", "Last 7 Days", "This Month")
    var selectedPeriod by remember { mutableStateOf(periods.first()) }
    
    // Categories placeholder for Chips
    val categories = listOf("All Categories", "Food", "Transport", "Shopping")
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                placeholder = { Text("Search expenses by note...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF8F9FA),
                    focusedContainerColor = Color(0xFFF8F9FA),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF00796B)
                ),
                shape = RoundedCornerShape(25.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Period Filter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PERIOD:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(periods) { period ->
                        val isSelected = period == selectedPeriod
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFF00695C) else Color.Transparent)
                                .clickable { selectedPeriod = period }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                period,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.DarkGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF1A1A1A) else Color(0xFFF8F9FA))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseListView(expenseList: List<Expense>, categoryList: List<Category>) {
    if (expenseList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions yet.", color = Color.Gray)
        }
    } else {
        val categoryMap = categoryList.associateBy { it.categoryId }
        val zoneId = ZoneId.systemDefault()
        
        // Group by Date
        val groupedExpenses = expenseList.groupBy { expense ->
            Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).toLocalDate()
        }.toSortedMap(compareByDescending { it })
        
        val headerFormatter = DateTimeFormatter.ofPattern("EEE, MMM d (yyyy-MM-dd)")
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedExpenses.forEach { (date, dailyExpenses) ->
                item {
                    val dailyTotal = dailyExpenses.sumOf { it.amount }
                    val dateString = date.format(headerFormatter)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column {
                            // Date Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFAFAFA))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(dateString, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                }
                                Text("-₹${dailyTotal.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            }
                            
                            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                            // Items
                            dailyExpenses.forEachIndexed { index, expense ->
                                val category = expense.categoryId?.let { categoryMap[it] }
                                val categoryName = category?.name ?: "Uncategorized"
                                val colorInt = safeParseColor(category?.colorHex)
                                val categoryColor = if (colorInt != null) Color(colorInt) else Color(0xFFE0F2F1)
                                val tintColor = if (colorInt != null) Color.White else Color(0xFF00796B)
                                val title = if (!expense.note.isNullOrBlank()) expense.note else categoryName
                                val timeStr = Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).format(timeFormatter)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(categoryName),
                                            contentDescription = categoryName,
                                            tint = tintColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                                        Text("$categoryName • $timeStr", fontSize = 12.sp, color = Color.Gray)
                                    }

                                    Text("-₹${expense.amount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                                }
                                
                                if (index < dailyExpenses.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF0F4F8), thickness = 1.dp, modifier = Modifier.padding(start = 72.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getCategoryIcon(categoryName: String): ImageVector {
    return when (categoryName.lowercase()) {
        "food", "dining" -> Icons.Outlined.Restaurant
        "transport" -> Icons.Outlined.DirectionsCar
        "shopping" -> Icons.Outlined.ShoppingBag
        "bills" -> Icons.Default.Receipt
        else -> Icons.Outlined.Category
    }
}



@Composable
fun ExpenseCalendarView(expenseList: List<Expense>, budgetList: List<com.ayaansaksham.spend_aware.data.local.entity.Budget>) {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    
    // For simplicity right now, we use the current month
    val yearMonth = YearMonth.of(today.year, today.month)
    val daysInMonth = yearMonth.lengthOfMonth()
    
    // Baseline logic from Section 6 spec: (Income - Fixed - Savings) / daysInMonth
    val totalIncome = budgetList.find { it.categoryId == null }?.limitAmount ?: 0.0
    val totalFixed = budgetList.filter { it.isFixed }.sumOf { it.limitAmount }
    val totalSavings = 10000.0 // Same demo fixture
    val monthlySpendable = (totalIncome - totalFixed - totalSavings).coerceAtLeast(0.0)
    
    val baselineDaily = monthlySpendable / daysInMonth
    
    // Group expenses by day
    val dailyExpenses = expenseList.filter {
        val date = Instant.ofEpochMilli(it.dateMillis).atZone(zoneId).toLocalDate()
        date.year == today.year && date.month == today.month
    }.groupBy {
        Instant.ofEpochMilli(it.dateMillis).atZone(zoneId).toLocalDate().dayOfMonth
    }.mapValues { entry ->
        entry.value.sumOf { it.amount }
    }

    // Legend
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LegendItem(Color(0xFFE6F4F2), "Under")
        LegendItem(Color(0xFFFFF4E5), "75%+")
        LegendItem(Color(0xFFFFDAD6), "90%+")
        LegendItem(Color(0xFFBA1A1A), "Over")
    }

    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Get offset for first day of month (Sunday = 0, Monday = 1... mapped from standard ISO where Mon = 1, Sun = 7)
        val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value
        val offset = if (firstDayOfWeek == 7) 0 else firstDayOfWeek

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(offset) { Spacer(modifier = Modifier.size(40.dp)) }

            items(daysInMonth) { index ->
                val day = index + 1
                val isToday = day == today.dayOfMonth
                val spent = dailyExpenses[day] ?: 0.0
                
                val (bgColor, textColor) = if (spent == 0.0) {
                    Pair(Color.White, Color(0xFF1A1A1A))
                } else {
                    val utilization: Double = if (baselineDaily > 0) spent / baselineDaily else if (spent > 0) 2.0 else 0.0
                    when {
                        utilization > 1.0 -> Pair(Color(0xFFBA1A1A), Color.White) // Over
                        utilization >= 0.9 -> Pair(Color(0xFFFFDAD6), Color(0xFF1A1A1A)) // 90%+
                        utilization >= 0.75 -> Pair(Color(0xFFFFF4E5), Color(0xFF1A1A1A)) // 75%+
                        else -> Pair(Color(0xFFE6F4F2), Color(0xFF00685D)) // Under
                    }
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(
                            width = if (isToday) 2.dp else 1.dp,
                            color = if (isToday) Color(0xFF00685D) else Color(0xFFF0F4F8),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = day.toString(),
                            fontSize = 12.sp,
                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                            color = textColor
                        )
                        if (spent > 0) {
                            Text(
                                text = "₹${spent.toInt()}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
    }
}