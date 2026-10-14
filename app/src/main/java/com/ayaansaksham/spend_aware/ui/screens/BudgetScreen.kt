package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import com.ayaansaksham.spend_aware.domain.logic.BurnRateResult
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgets: StateFlow<List<Budget>>,
    categories: StateFlow<List<Category>>,
    burnRates: StateFlow<Map<Int?, BurnRateResult>>,
    onSaveBudget: (Budget) -> Unit,
    onRefreshBurnRates: () -> Unit,
    onNavigateBack: () -> Unit,
    // Injecting expenses to calculate accurate spent/remaining exactly like Dashboard
    expenses: StateFlow<List<Expense>>? = null,
    currentProfileId: Int = 1
) {
    val budgetList by budgets.collectAsState()
    val categoryList by categories.collectAsState()
    val expenseList by (expenses ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()

    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val daysInMonth = YearMonth.of(today.year, today.month).lengthOfMonth()
    val daysRemaining = daysInMonth - today.dayOfMonth + 1

    val masterBudget = remember(budgetList) { budgetList.find { it.categoryId == null } }
    val fixedBudgets = remember(budgetList) { budgetList.filter { it.isFixed } }
    
    val totalIncome = masterBudget?.limitAmount ?: 0.0
    val totalFixed = fixedBudgets.sumOf { it.limitAmount }
    val fixedCategoryIds = fixedBudgets.mapNotNull { it.categoryId }.toSet()
    
    // Using a local state to visually track savings target for the presentation
    var displaySavingsTarget by remember { mutableStateOf(0.0) }
    val totalSavings = displaySavingsTarget

    val flexibleBudget = (totalIncome - totalFixed - totalSavings).coerceAtLeast(0.0)
    
    val flexibleExpensesThisMonth = expenseList.filter { expense ->
        val expenseDate = Instant.ofEpochMilli(expense.dateMillis).atZone(zoneId).toLocalDate()
        expenseDate.year == today.year && expenseDate.month == today.month && expense.categoryId !in fixedCategoryIds
    }
    val spentThisMonth = flexibleExpensesThisMonth.sumOf { it.amount }
    
    val remaining = (flexibleBudget - spentThisMonth).coerceAtLeast(0.0)
    val dailyLimit = if (daysRemaining > 0) remaining / daysRemaining else 0.0
    val progress = if (flexibleBudget > 0) (spentThisMonth.toFloat() / flexibleBudget.toFloat()).coerceIn(0f, 1f) else 0f

    var showIncomeDialog by remember { mutableStateOf(false) }
    var showFixedExpenseDialog by remember { mutableStateOf(false) }
    var showSavingsGoalDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    val showNotImplemented = {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("This feature is coming soon in V2!")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Monthly Budget", fontWeight = FontWeight.SemiBold) },
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
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // SUMMARY CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Metrics Grid
                            val incomeLabel = if (currentProfileId == 1) "Allowance" else "Income"
                            val fixedLabel = if (currentProfileId == 1) "Fixed Deductions" else "Fixed Expenses"
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(incomeLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("₹${totalIncome.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(fixedLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("₹${totalFixed.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Savings Goal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("₹${totalSavings.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(20.dp))

                            Text("AVAILABLE FOR PACING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("₹${flexibleBudget.toInt()}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Live Pace", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Cycle Commitment Ratio Bar (Fixed vs Save vs Flexible)
                            val totalBudgetAmount = totalIncome.coerceAtLeast(1.0)
                            val fixedRatio = (totalFixed / totalBudgetAmount).toFloat()
                            val savingsRatio = (totalSavings / totalBudgetAmount).toFloat()
                            val flexRatio = 1f - fixedRatio - savingsRatio
                            
                            Row(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
                                Box(modifier = Modifier.weight(fixedRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.error))
                                Box(modifier = Modifier.weight(savingsRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
                                Box(modifier = Modifier.weight(flexRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cycle Commitment Ratio", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${(fixedRatio*100).toInt()}% Fixed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${(savingsRatio*100).toInt()}% Save", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${(flexRatio*100).toInt()}% Pace", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Spent: ₹${spentThisMonth.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Remaining: ₹${remaining.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("⚡ ₹${dailyLimit.toInt()} / daily pace", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val barColor = when {
                                progress >= 1f -> MaterialTheme.colorScheme.error
                                progress >= 0.75f -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                            
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                // ADVANCED PREDICTIVE TREND CARD
                item {
                    val daysElapsed = daysInMonth - daysRemaining + 1
                    val projectedTotal = if (daysElapsed > 0) (spentThisMonth / daysElapsed) * daysInMonth else 0.0
                    val isTrendingOver = projectedTotal > flexibleBudget
                    
                    val trendColor = if (isTrendingOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    val trendText = if (isTrendingOver) {
                        "Trend: You are on track to exceed your budget by ₹${(projectedTotal - flexibleBudget).toInt()}"
                    } else {
                        "Trend: On track to save ₹${(flexibleBudget - projectedTotal).toInt()}"
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = trendColor.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, trendColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isTrendingOver) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Month-End Projection", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = trendColor.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(trendText, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = trendColor)
                            }
                        }
                    }
                }

                // BUDGET CONFIGURATION
                item {
                    Text("Budget Configuration", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val incomeLabel = if (currentProfileId == 1) "Daily Floor" else "Daily Floor"
                    val fixedLabel = if (currentProfileId == 1) "Rollover Rule" else "Rollover Rule"
                    val savingsLabel = if (currentProfileId == 1) "Weekend Boost" else "Weekend Boost"
                    val rulesLabel = if (currentProfileId == 1) "Hard Ceiling" else "Hard Ceiling"

                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().height(220.dp) // Fixed height to fit the 2x2 grid
                    ) {
                        item {
                            ConfigTile(
                                icon = Icons.Outlined.AccountBalanceWallet,
                                label = incomeLabel,
                                value = "₹500",
                                onClick = { showNotImplemented() },
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        item {
                            ConfigTile(
                                icon = Icons.Outlined.Lock,
                                label = fixedLabel,
                                value = "Active",
                                onClick = { showNotImplemented() },
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        item {
                            ConfigTile(
                                icon = Icons.Outlined.Star,
                                label = savingsLabel,
                                value = "Disabled",
                                onClick = { showNotImplemented() },
                                indicatorColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        item {
                            ConfigTile(
                                icon = Icons.Default.TrendingDown,
                                label = rulesLabel,
                                value = "₹10,000",
                                onClick = { showNotImplemented() },
                                indicatorColor = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // FIXED COMMITMENTS SECTION (USING BUDGET ENTITY)
                item {
                    Text("Fixed Recurring Outflows", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (fixedBudgets.isEmpty()) {
                        Text("No fixed commitments configured.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            fixedBudgets.forEach { budget ->
                                val category = categoryList.find { it.categoryId == budget.categoryId }
                                val parsedColor = try {
                                    Color(android.graphics.Color.parseColor(category?.colorHex ?: "#64748B"))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                        // Colored left border indicator
                                        Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(parsedColor))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(parsedColor.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarToday, // Placeholder icon
                                                    contentDescription = null,
                                                    tint = parsedColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(category?.name ?: "Commitment", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                                Text("Auto-Pay Active", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            Text("₹${budget.limitAmount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Add Fixed Commitment Button
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showFixedExpenseDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) // Dashed effect placeholder
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+ Add Fixed Commitment", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                
                // INTELLIGENCE FOOTER CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Intelligence",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Your daily pace automatically recalibrates at midnight if discretionary spending stays under ₹${dailyLimit.toInt()} today.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (showIncomeDialog) {
        BudgetAmountDialog(
            title = "Monthly Income",
            currentAmount = masterBudget?.limitAmount,
            onDismiss = { showIncomeDialog = false },
            onConfirm = { amount ->
                onSaveBudget(
                    Budget(
                        budgetId = masterBudget?.budgetId ?: 0,
                        categoryId = null,
                        limitAmount = amount,
                        periodType = masterBudget?.periodType ?: "MONTHLY",
                        startDateMillis = masterBudget?.startDateMillis ?: System.currentTimeMillis()
                    )
                )
                showIncomeDialog = false
            }
        )
    }

    if (showFixedExpenseDialog) {
        // Since we are editing a general aggregate here (to quickly change total fixed expenses for demo purposes)
        // We will just edit the primary fixed budget. If there isn't one, we'll create a placeholder.
        val primaryFixedBudget = fixedBudgets.firstOrNull() 
        BudgetAmountDialog(
            title = "Total Fixed Expenses",
            currentAmount = totalFixed,
            onDismiss = { showFixedExpenseDialog = false },
            onConfirm = { amount ->
                // Updating the primary fixed budget to hold the new aggregate value for quick presentation adjustment
                val updatedBudget = primaryFixedBudget?.copy(limitAmount = amount) ?: Budget(
                    limitAmount = amount,
                    periodType = "MONTHLY",
                    startDateMillis = System.currentTimeMillis(),
                    categoryId = 4, // Defaulting to some "Bills" category ID
                    isFixed = true,
                    profileId = masterBudget?.profileId ?: 1
                )
                onSaveBudget(updatedBudget)
                showFixedExpenseDialog = false
            }
        )
    }

    if (showSavingsGoalDialog) {
        val title = if (currentProfileId == 1) "Wishlist Target" else "Monthly Savings Target"
        BudgetAmountDialog(
            title = title,
            currentAmount = totalSavings,
            onDismiss = { showSavingsGoalDialog = false },
            onConfirm = { amount ->
                // Updating local state so the UI reacts instantly for the presentation
                displaySavingsTarget = amount
                showSavingsGoalDialog = false
            }
        )
    }
}

@Composable
fun ConfigTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, indicatorColor: Color? = null, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (indicatorColor != null) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(indicatorColor))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
fun BudgetAmountDialog(
    title: String,
    currentAmount: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(currentAmount?.toInt()?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> if (input.all { it.isDigit() || it == '.' }) amountText = input },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.outline,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount >= 0) onConfirm(amount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Cancel") }
        }
    )
}