package com.ayaansaksham.spend_aware.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionHistoryScreen(
    expenses: StateFlow<List<Expense>>,
    categories: StateFlow<List<Category>>,
    onNavigateBack: () -> Unit
) {
    val allExpenses by expenses.collectAsState()
    val categoryList by categories.collectAsState()
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }

    val categoryMap = remember(categoryList) { categoryList.associateBy { it.categoryId } }

    // Filter and group by Date (Midnight timestamp)
    val groupedExpenses = remember(allExpenses, selectedCategoryId) {
        val filtered = if (selectedCategoryId == null) allExpenses
        else allExpenses.filter { it.categoryId == selectedCategoryId }

        filtered.groupBy { expense ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = expense.dateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.timeInMillis
        }.toSortedMap(compareByDescending { it }) // Sort newest days first
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History", fontWeight = FontWeight.SemiBold) },
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
        ) {
            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChipItem(
                        label = "All",
                        isSelected = selectedCategoryId == null,
                        colorHex = null,
                        onClick = { selectedCategoryId = null }
                    )
                }
                items(categoryList) { category ->
                    FilterChipItem(
                        label = category.name,
                        isSelected = selectedCategoryId == category.categoryId,
                        colorHex = category.colorHex,
                        onClick = { selectedCategoryId = category.categoryId }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            if (groupedExpenses.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No transactions found",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    groupedExpenses.forEach { (dateInMillis, expensesForDate) ->
                        // Sticky Header for the Date
                        stickyHeader {
                            DateHeader(dateInMillis)
                        }

                        // Expenses for that specific date
                        items(expensesForDate) { expense ->
                            val category = categoryMap[expense.categoryId]
                            TransactionRow(expense, category)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    colorHex: String?,
    onClick: () -> Unit
) {
    val colorInt = safeParseColor(colorHex)
    val parsedColor = if (colorInt != null) Color(colorInt) else MaterialTheme.colorScheme.primary

    val backgroundColor = if (isSelected) parsedColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        contentColor = textColor,
        onClick = onClick,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun DateHeader(dateInMillis: Long) {
    val headerText = when {
        DateUtils.isToday(dateInMillis) -> "Today"
        DateUtils.isToday(dateInMillis + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
        else -> SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(dateInMillis))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = headerText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TransactionRow(expense: Expense, category: Category?) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    val colorInt = safeParseColor(category?.colorHex)
    val parsedColor = if (colorInt != null) Color(colorInt) else MaterialTheme.colorScheme.secondaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Category Icon/Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(parsedColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (category != null) {
                    Text(
                        text = category.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = parsedColor,
                        fontSize = 20.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                val title = expense.note?.takeIf { it.isNotBlank() } ?: category?.name ?: "Uncategorized"
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${category?.name ?: "General"} • ${timeFormat.format(Date(expense.dateMillis))}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "-₹${String.format(Locale.getDefault(), "%.2f", expense.amount)}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// Helper function placed outside of Composables to safely parse hex strings
fun safeParseColor(colorHex: String?): Int? {
    if (colorHex.isNullOrBlank()) return null
    return try {
        android.graphics.Color.parseColor(colorHex)
    } catch (e: Exception) {
        null
    }
}