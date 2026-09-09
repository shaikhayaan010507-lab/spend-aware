package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentMonthTotal: StateFlow<Double>,
    recentExpenses: StateFlow<List<Expense>>,
    onAddExpenseClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onViewHistoryClick: () -> Unit,
    onManageBudgetClick: () -> Unit,
    onManageGoalsClick: () -> Unit
) {
    val total by currentMonthTotal.collectAsState()
    val expenses by recentExpenses.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Spend-Aware") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpenseClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("This Month", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%.2f", total)}",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAccessButton("Categories", Modifier.weight(1f), onManageCategoriesClick)
                QuickAccessButton("History", Modifier.weight(1f), onViewHistoryClick)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAccessButton("Budgets", Modifier.weight(1f), onManageBudgetClick)
                QuickAccessButton("Goals", Modifier.weight(1f), onManageGoalsClick)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Recent Expenses", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No expenses yet — tap + to add one", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(expenses.take(5)) { expense ->
                        ExpenseRow(expense)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAccessButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(label, fontSize = 13.sp)
    }
}

@Composable
fun ExpenseRow(expense: Expense) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = expense.note ?: "Expense", fontWeight = FontWeight.Medium)
            Text(
                text = dateFormat.format(Date(expense.dateMillis)),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(text = "₹${String.format(Locale.getDefault(), "%.2f", expense.amount)}", fontWeight = FontWeight.Bold)
    }
}