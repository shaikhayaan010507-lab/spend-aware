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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.MoreHoriz
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
    onDeleteExpense: (Expense) -> Unit = {},
    onEditExpense: (Expense) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val expenseList by (expenses ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val categoryList by (categories ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val budgetList by (budgets ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()

    // Toggle state: true = List View, false = Calendar View
    var isListView by remember { mutableStateOf(true) }

    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf("All Time") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)

    val filteredExpenses = remember(expenseList, searchQuery, selectedPeriod, selectedCategory) {
        val currentCategory = selectedCategory
        expenseList.filter { expense ->
            val noteMatches = searchQuery.isBlank() || expense.note?.contains(searchQuery, ignoreCase = true) == true
            val catMatches = currentCategory == null || expense.categoryId == currentCategory.categoryId
            val date = Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).toLocalDate()
            
            val periodMatches = when (selectedPeriod) {
                "Today" -> date == today
                "Last 7 Days" -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today)
                "This Month" -> date.year == today.year && date.month == today.month
                else -> true // "All Time"
            }
            
            noteMatches && catMatches && periodMatches
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Segmented Toggle Button (List vs Calendar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // List Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isListView) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isListView = true },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = "List", tint = if (isListView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Expense History", fontWeight = FontWeight.SemiBold, color = if (isListView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }

            // Calendar Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (!isListView) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isListView = false },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = if (!isListView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calendar View", fontWeight = FontWeight.SemiBold, color = if (!isListView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Content Area
        if (isListView) {
            QuickStatsRow(filteredExpenses, categoryList)
            Spacer(modifier = Modifier.height(16.dp))
            FiltersSection(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                selectedPeriod = selectedPeriod,
                onPeriodChange = { selectedPeriod = it },
                categories = categoryList,
                selectedCategory = selectedCategory,
                onCategoryChange = { selectedCategory = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            ExpenseListView(filteredExpenses, categoryList, onDeleteExpense, onEditExpense)
        } else {
            ExpenseCalendarView(expenseList, budgetList)
        }
    }
}

@Composable
fun QuickStatsRow(expenseList: List<Expense>, categoryList: List<Category>) {
    val totalSpent = expenseList.sumOf { it.amount }
    val txCount = expenseList.size
    val txText = if (txCount == 1) "1 entry" else "$txCount entries"
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
        StatCard(title = "TOTAL SPENT", value = "₹${totalSpent.toInt()}", valueColor = MaterialTheme.colorScheme.error)
        StatCard(title = "TRANSACTIONS", value = txText, valueColor = MaterialTheme.colorScheme.onSurface)
        StatCard(title = "AVERAGE / LOG", value = "₹${avgSpend.toInt()}", valueColor = MaterialTheme.colorScheme.primary)
        StatCard(title = "TOP CATEGORY", value = topCategoryName, valueColor = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun StatCard(title: String, value: String, valueColor: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.widthIn(min = 120.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit,
    categories: List<Category>,
    selectedCategory: Category?,
    onCategoryChange: (Category?) -> Unit
) {
    val periods = listOf("All Time", "Today", "Last 7 Days", "This Month")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                placeholder = { Text("Search expenses by note...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(25.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Period Filter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PERIOD:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(periods) { period ->
                        val isSelected = period == selectedPeriod
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onPeriodChange(period) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                period,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter
            if (categories.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        val isSelected = selectedCategory == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onCategoryChange(null) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "All Categories",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onCategoryChange(cat) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                cat.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                 Text(
                    "No Categories Available",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListView(
    expenseList: List<Expense>,
    categoryList: List<Category>,
    onDeleteExpense: (Expense) -> Unit,
    onEditExpense: (Expense) -> Unit
) {
    if (expenseList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val categoryMap = categoryList.associateBy { it.categoryId }
        val zoneId = ZoneId.systemDefault()
        
        val today = LocalDate.now(zoneId)
        val yesterday = today.minusDays(1)
        
        // Group by Date
        val groupedExpenses = expenseList.groupBy { expense ->
            Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).toLocalDate()
        }.toSortedMap(compareByDescending { it })
        
        val fallbackFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupedExpenses.forEach { (date, dailyExpenses) ->
                stickyHeader {
                    val dailyTotal = dailyExpenses.sumOf { it.amount }
                    val dateString = when (date) {
                        today -> "Today"
                        yesterday -> "Yesterday"
                        else -> date.format(fallbackFormatter)
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dateString, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("-₹${dailyTotal.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                items(dailyExpenses, key = { it.expenseId }) { expense ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                onDeleteExpense(expense)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    val category = expense.categoryId?.let { categoryMap[it] }
                    val categoryName = category?.name ?: "Uncategorized"
                    val colorInt = safeParseColor(category?.colorHex)
                    val categoryColor = if (colorInt != null) Color(colorInt) else MaterialTheme.colorScheme.primaryContainer
                    val tintColor = if (colorInt != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onPrimaryContainer
                    val title = if (!expense.note.isNullOrBlank()) expense.note else categoryName
                    val timeStr = Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).format(timeFormatter)

                    SwipeToDismissBox(
                        state = dismissState,
                        modifier = Modifier.animateItem(),
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.error)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onError)
                            }
                        },
                        content = {
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onEditExpense(expense) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
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
                                        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        Text("$categoryName • $timeStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Text("-₹${expense.amount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

fun getCategoryIcon(categoryName: String): ImageVector {
    return when (categoryName.lowercase()) {
        "food", "dining", "canteen" -> Icons.Rounded.Fastfood
        "transport", "commute" -> Icons.Rounded.DirectionsBus
        "shopping" -> Icons.Rounded.ShoppingBag
        "bills", "bills & utilities", "rent", "hostel fees" -> Icons.Rounded.Receipt
        "health & fitness" -> Icons.Rounded.FavoriteBorder
        "groceries" -> Icons.Rounded.LocalGroceryStore
        "stationery" -> Icons.Rounded.Edit
        "events" -> Icons.Rounded.Event
        "investments" -> Icons.Rounded.TrendingUp
        else -> Icons.Rounded.MoreHoriz
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
        LegendItem(MaterialTheme.colorScheme.primaryContainer, "Under")
        LegendItem(MaterialTheme.colorScheme.secondaryContainer, "75%+")
        LegendItem(MaterialTheme.colorScheme.errorContainer, "90%+")
        LegendItem(MaterialTheme.colorScheme.error, "Over")
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Pair(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
                } else {
                    val utilization: Double = if (baselineDaily > 0) spent / baselineDaily else if (spent > 0) 2.0 else 0.0
                    when {
                        utilization > 1.0 -> Pair(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError) // Over
                        utilization >= 0.9 -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer) // 90%+
                        utilization >= 0.75 -> Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) // 75%+
                        else -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) // Under
                    }
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(
                            width = if (isToday) 2.dp else 1.dp,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
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
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}