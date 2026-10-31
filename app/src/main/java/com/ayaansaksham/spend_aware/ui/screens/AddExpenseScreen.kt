package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import com.ayaansaksham.spend_aware.domain.logic.RealTimeAnomalyResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    categories: StateFlow<List<Category>>,
    budgets: StateFlow<List<com.ayaansaksham.spend_aware.data.local.entity.Budget>>? = null,
    expenses: StateFlow<List<Expense>>? = null,
    currentProfileId: Int = 1,
    expenseIdToEdit: Int? = null,
    onSaveExpense: (Expense) -> Unit,
    onUpdateExpense: (Expense) -> Unit = {},
    onAddCategory: (Category) -> Unit = {},
    onCheckAnomaly: suspend (Expense, Double, String, Int?) -> RealTimeAnomalyResult = { _, _, _, _ -> RealTimeAnomalyResult.None },
    onNavigateBack: () -> Unit,
    initialDateMillis: Long = System.currentTimeMillis()
) {
    val categoryList by categories.collectAsState()
    val budgetList by (budgets ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val expenseList by (expenses ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedDateMillis by rememberSaveable { mutableStateOf(initialDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var hasPrefilled by rememberSaveable { mutableStateOf(false) }

    var showAddCategorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    var newCategoryName by remember { mutableStateOf("") }
    val defaultColors = listOf("#00685D", "#F89C00", "#6D7A77", "#875200", "#B02528", "#16A34A", "#1976D2", "#9C27B0")
    var selectedNewCategoryColor by remember { mutableStateOf(defaultColors[0]) }

    var pendingExpense by remember { mutableStateOf<Expense?>(null) }
    var anomalyResult by remember { mutableStateOf<RealTimeAnomalyResult?>(null) }

    LaunchedEffect(expenseIdToEdit, expenseList) {
        if (expenseIdToEdit != null && !hasPrefilled && expenseList.isNotEmpty()) {
            val expenseToEdit = expenseList.find { it.expenseId == expenseIdToEdit }
            if (expenseToEdit != null) {
                amountText = if (expenseToEdit.amount % 1 == 0.0) expenseToEdit.amount.toInt().toString() else expenseToEdit.amount.toString()
                note = expenseToEdit.note ?: ""
                selectedCategoryId = expenseToEdit.categoryId
                selectedDateMillis = expenseToEdit.dateMillis
                hasPrefilled = true
            }
        }
    }

    val isEditMode = expenseIdToEdit != null
    val screenTitle = if (isEditMode) "Edit Expense" else "Add Expense"
    val buttonText = if (isEditMode) "Save changes" else "Record Expense"

    // Calculate Safe to Spend Today
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)

    val safeToSpend = remember(budgetList, expenseList, today) {
        com.ayaansaksham.spend_aware.domain.logic.SafeToSpendCalculator.calculate(
            budgets = budgetList,
            expenses = expenseList,
            today = today,
            zoneId = zoneId
        )
    }

    val dailyLimit = safeToSpend.dailyLimit

    // Quick add chips
    val quickAddValues = listOf(100, 250, 500, 1000)

    val isFutureDate = remember(selectedDateMillis) {
        val selectedDate = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now(ZoneId.systemDefault())
        selectedDate.isAfter(today)
    }

    val isValid = amountText.isNotBlank() && amountText.toDoubleOrNull()?.let { it > 0 } == true && selectedCategoryId != null && !isFutureDate

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Hero amount input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "₹",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                TextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                            amountText = input
                        }
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    placeholder = {
                        Text(
                            "0",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.width(IntrinsicSize.Min)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Add Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                quickAddValues.forEach { value ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            val currentAmount = amountText.toDoubleOrNull() ?: 0.0
                            val newAmount = currentAmount + value
                            // Format to remove trailing .0 if present
                            amountText = if (newAmount % 1 == 0.0) newAmount.toInt().toString() else newAmount.toString()
                        }
                    ) {
                        Text(
                            text = "+$value",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category Grid
            if (categoryList.isEmpty()) {
                Text(
                    text = "No categories available.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp) // Bound height to avoid taking over entire screen
                ) {
                    items(categoryList) { category ->
                        CategoryGridItem(
                            category = category,
                            isSelected = category.categoryId == selectedCategoryId,
                            onClick = { selectedCategoryId = category.categoryId }
                        )
                    }
                    item {
                        AddCategoryGridItem(onClick = { showAddCategorySheet = true })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Note Field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("e.g. Lunch with team, Groceries, Metro pass", color = MaterialTheme.colorScheme.outlineVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.outline,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker Card
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val dateLabel = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                    Text(
                        text = dateLabel,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            if (isFutureDate) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Future dates are not allowed for actual expenses. Use budget commitments for planned spending.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0 && selectedCategoryId != null && !isFutureDate) {
                        val expense = Expense(
                            expenseId = expenseIdToEdit ?: 0,
                            amount = amount,
                            categoryId = selectedCategoryId,
                            note = note.trim().ifEmpty { null },
                            dateMillis = selectedDateMillis,
                            profileId = currentProfileId
                        )
                        
                        coroutineScope.launch {
                            val categoryName = categoryList.find { it.categoryId == selectedCategoryId }?.name ?: "Unknown"
                            val anomaly = onCheckAnomaly(expense, dailyLimit, categoryName, expenseIdToEdit)
                            
                            if (anomaly is RealTimeAnomalyResult.None) {
                                if (isEditMode) {
                                    onUpdateExpense(expense)
                                } else {
                                    onSaveExpense(expense)
                                }
                                snackbarHostState.showSnackbar(if (isEditMode) "Expense updated successfully" else "Expense saved successfully")
                                onNavigateBack()
                            } else {
                                pendingExpense = expense
                                anomalyResult = anomaly
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Add Category Bottom Sheet
    if (showAddCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddCategorySheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("New Category", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Select Color Tag", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    defaultColors.forEach { hexColor ->
                        val color = try { Color(android.graphics.Color.parseColor(hexColor)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                        val isColorSelected = selectedNewCategoryColor == hexColor
                        
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedNewCategoryColor = hexColor }
                                .border(
                                    width = if (isColorSelected) 3.dp else 0.dp,
                                    color = if (isColorSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isColorSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(
                                Category(
                                    name = newCategoryName.trim(),
                                    colorHex = selectedNewCategoryColor,
                                    profileId = currentProfileId,
                                    isDefault = false
                                )
                            )
                            newCategoryName = ""
                            coroutineScope.launch {
                                sheetState.hide()
                                showAddCategorySheet = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Save Category", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Anomaly Alert Dialog
    anomalyResult?.let { anomaly ->
        AlertDialog(
            onDismissRequest = { anomalyResult = null },
            title = { Text("Large Expense Alert", fontWeight = FontWeight.Bold) },
            text = {
                val message = when (anomaly) {
                    is RealTimeAnomalyResult.LargePercentage -> {
                        "This expense is ₹${anomaly.amount.toInt()}, ${anomaly.percentage.toInt()}% of today's Safe to Spend limit. Save anyway?"
                    }
                    is RealTimeAnomalyResult.Outlier -> {
                        "This is ${String.format(Locale.getDefault(), "%.1fx", anomaly.multiplier)} your usual spending in ${anomaly.categoryName} (normal range: ₹${anomaly.normalMin.toInt()}–₹${anomaly.normalMax.toInt()}). Save anyway?"
                    }
                    else -> ""
                }
                Text(message)
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingExpense?.let {
                        if (isEditMode) onUpdateExpense(it) else onSaveExpense(it)
                    }
                    anomalyResult = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(if (isEditMode) "Expense updated successfully" else "Expense saved successfully")
                        onNavigateBack()
                    }
                }) {
                    Text("Save Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { anomalyResult = null }) {
                    Text("Edit")
                }
            }
        )
    }
}

@Composable
fun CategoryGridItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary // Fallback
    }

    // DailyBudget spec says selected tile has teal outline and pale teal fill.
    // However, since categories have their own colors, we adapt slightly:
    // Outline matches the category color if selected, fill is very pale version.
    val outlineColor = if (isSelected) parsedColor else Color.Transparent
    val backgroundColor = if (isSelected) parsedColor.copy(alpha = 0.1f) else Color.Transparent

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CategoryScaleAnim"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = outlineColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isSelected) parsedColor else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Reusing getCategoryIcon from TransactionHistoryScreen logic if needed, 
            // but since we can't easily import private functions, we use a default icon 
            // or the text initial for now.
            val icon = getIconForCategoryName(category.name)
            Icon(
                imageVector = icon,
                contentDescription = category.name,
                tint = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddCategoryGridItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add New",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Add New",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// Temporary helper until we extract a shared icon mapping object
fun getIconForCategoryName(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name.lowercase()) {
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