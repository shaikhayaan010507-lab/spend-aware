package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Category
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgets: StateFlow<List<Budget>>,
    categories: StateFlow<List<Category>>,
    onSaveBudget: (Budget) -> Unit,
    onNavigateBack: () -> Unit
) {
    val budgetList by budgets.collectAsState()
    val categoryList by categories.collectAsState()

    val overallBudget = remember(budgetList) { budgetList.find { it.categoryId == null } }
    val categoryBudgetMap = remember(budgetList) {
        budgetList.filter { it.categoryId != null }.associateBy { it.categoryId }
    }

    var editingOverall by remember { mutableStateOf(false) }
    var showDialogForCategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            Text("Overall Monthly Budget", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { editingOverall = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (overallBudget != null)
                            "₹${String.format(Locale.getDefault(), "%.2f", overallBudget.limitAmount)}"
                        else "Not set",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Tap to edit", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Category Budgets", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (categoryList.isEmpty()) {
                Text("No categories yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(categoryList) { category ->
                        val budget = categoryBudgetMap[category.categoryId]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDialogForCategory = category }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(category.colorHex)))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(category.name)
                            }
                            Text(
                                text = if (budget != null)
                                    "₹${String.format(Locale.getDefault(), "%.2f", budget.limitAmount)}"
                                else "Not set",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (editingOverall) {
        BudgetAmountDialog(
            title = "Overall Monthly Budget",
            currentAmount = overallBudget?.limitAmount,
            onDismiss = { editingOverall = false },
            onConfirm = { amount ->
                onSaveBudget(
                    Budget(
                        budgetId = overallBudget?.budgetId ?: 0,
                        categoryId = null,
                        limitAmount = amount,
                        periodType = "MONTHLY",
                        startDateMillis = System.currentTimeMillis()
                    )
                )
                editingOverall = false
            }
        )
    }

    showDialogForCategory?.let { category ->
        val existingBudget = categoryBudgetMap[category.categoryId]
        BudgetAmountDialog(
            title = "${category.name} Budget",
            currentAmount = existingBudget?.limitAmount,
            onDismiss = { showDialogForCategory = null },
            onConfirm = { amount ->
                onSaveBudget(
                    Budget(
                        budgetId = existingBudget?.budgetId ?: 0,
                        categoryId = category.categoryId,
                        limitAmount = amount,
                        periodType = "MONTHLY",
                        startDateMillis = System.currentTimeMillis()
                    )
                )
                showDialogForCategory = null
            }
        )
    }
}

@Composable
fun BudgetAmountDialog(
    title: String,
    currentAmount: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(currentAmount?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> if (input.all { it.isDigit() || it == '.' }) amountText = input },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount != null && amount > 0) onConfirm(amount)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}