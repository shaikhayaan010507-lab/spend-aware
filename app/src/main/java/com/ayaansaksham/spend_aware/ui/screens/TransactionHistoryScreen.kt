package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    val filteredExpenses = remember(allExpenses, selectedCategoryId) {
        if (selectedCategoryId == null) allExpenses
        else allExpenses.filter { it.categoryId == selectedCategoryId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChipItem(
                        label = "All",
                        isSelected = selectedCategoryId == null,
                        color = null,
                        onClick = { selectedCategoryId = null }
                    )
                }
                items(categoryList) { category ->
                    FilterChipItem(
                        label = category.name,
                        isSelected = selectedCategoryId == category.categoryId,
                        color = Color(android.graphics.Color.parseColor(category.colorHex)),
                        onClick = { selectedCategoryId = category.categoryId }
                    )
                }
            }

            HorizontalDivider()

            if (filteredExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(filteredExpenses) { expense ->
                        val category = categoryMap[expense.categoryId]
                        TransactionRow(expense, category)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipItem(label: String, isSelected: Boolean, color: Color?, onClick: () -> Unit) {
    val backgroundColor = when {
        isSelected && color != null -> color
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = label, color = textColor, fontSize = 14.sp)
    }
}

@Composable
fun TransactionRow(expense: Expense, category: Category?) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (category != null) Color(android.graphics.Color.parseColor(category.colorHex))
                        else Color.Gray
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = expense.note ?: (category?.name ?: "Uncategorized"), fontWeight = FontWeight.Medium)
                Text(
                    text = "${category?.name ?: "Uncategorized"} • ${dateFormat.format(Date(expense.dateMillis))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "₹${String.format(Locale.getDefault(), "%.2f", expense.amount)}",
            fontWeight = FontWeight.Bold
        )
    }
}