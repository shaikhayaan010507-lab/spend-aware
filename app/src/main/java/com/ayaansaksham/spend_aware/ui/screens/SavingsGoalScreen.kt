package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.data.local.entity.SavingsGoal
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalScreen(
    goals: StateFlow<List<SavingsGoal>>,
    onAddGoal: (SavingsGoal) -> Unit,
    onUpdateGoal: (SavingsGoal) -> Unit,
    onNavigateBack: () -> Unit
) {
    val goalList by goals.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var addFundsGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Goal")
            }
        }
    ) { paddingValues ->
        if (goalList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No savings goals yet — tap + to add one", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
            ) {
                items(goalList) { goal ->
                    SavingsGoalCard(goal = goal, onAddFundsClick = { addFundsGoal = goal })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, target ->
                onAddGoal(SavingsGoal(title = title, targetAmount = target, savedAmount = 0.0))
                showAddDialog = false
            }
        )
    }

    addFundsGoal?.let { goal ->
        AddFundsDialog(
            goal = goal,
            onDismiss = { addFundsGoal = null },
            onConfirm = { amountToAdd ->
                onUpdateGoal(goal.copy(savedAmount = goal.savedAmount + amountToAdd))
                addFundsGoal = null
            }
        )
    }
}

@Composable
fun SavingsGoalCard(goal: SavingsGoal, onAddFundsClick: () -> Unit) {
    val progress = if (goal.targetAmount > 0)
        (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(goal.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                TextButton(onClick = onAddFundsClick) { Text("Add Funds") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "₹${String.format(Locale.getDefault(), "%.2f", goal.savedAmount)} of ₹${String.format(Locale.getDefault(), "%.2f", goal.targetAmount)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (title: String, target: Double) -> Unit) {
    var title by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { input -> if (input.all { it.isDigit() || it == '.' }) targetText = input },
                    label = { Text("Target amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = targetText.toDoubleOrNull()
                if (title.isNotBlank() && target != null && target > 0) onConfirm(title.trim(), target)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddFundsDialog(goal: SavingsGoal, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Funds to ${goal.title}") },
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
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}