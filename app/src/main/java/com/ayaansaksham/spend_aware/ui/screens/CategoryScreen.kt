package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    categories: StateFlow<List<Category>>,
    expenses: StateFlow<List<Expense>>? = null,
    currentProfileId: Int = 1,
    onAddCategory: (Category) -> Unit,
    onUpdateCategory: (Category) -> Unit = {},
    onDeleteCategory: (Category) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val categoryList by categories.collectAsState()
    val expenseList by (expenses ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Category?>(null) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    categoryToEdit = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(categoryList) { category ->
                CategoryItem(
                    category = category,
                    onEdit = {
                        categoryToEdit = category
                        showDialog = true
                    },
                    onDelete = { 
                        showDeleteConfirmDialog = category 
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // Space for FAB
        }
    }

    if (showDialog) {
        CategoryDialog(
            category = categoryToEdit,
            existingCategories = categoryList,
            currentProfileId = currentProfileId,
            onDismiss = {
                showDialog = false
                categoryToEdit = null
            },
            onSave = { name, colorHex ->
                if (categoryToEdit == null) {
                    onAddCategory(Category(name = name, colorHex = colorHex, profileId = currentProfileId))
                } else {
                    onUpdateCategory(categoryToEdit!!.copy(name = name, colorHex = colorHex))
                }
                showDialog = false
                categoryToEdit = null
            }
        )
    }

    if (showDeleteConfirmDialog != null) {
        val categoryToDelete = showDeleteConfirmDialog!!
        val orphanedExpensesCount = expenseList.count { it.categoryId == categoryToDelete.categoryId }
        
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Category?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Are you sure you want to delete '${categoryToDelete.name}'?")
                    if (orphanedExpensesCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "WARNING: This category is currently used by $orphanedExpensesCount expenses. Deleting it will mark those expenses as 'Uncategorized'.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(categoryToDelete)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Safely parse the color hex string
    val circleColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary // Fallback just in case
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = category.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 3-Dot Options Menu
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            expanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDialog(
    category: Category?,
    existingCategories: List<Category>,
    currentProfileId: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }

    // A professional set of Material colors for the user to choose from
    val colorPalette = listOf("#F44336", "#E91E63", "#9C27B0", "#3F51B5", "#2196F3", "#00BCD4", "#4CAF50", "#FF9800", "#795548")
    var selectedColor by remember { mutableStateOf(category?.colorHex ?: colorPalette[0]) }

    val nameTrimmed = name.trim()
    val isNameEmpty = nameTrimmed.isEmpty()
    val isNameTooLong = nameTrimmed.length > 20
    val isNameDuplicate = existingCategories.any { 
        it.name.equals(nameTrimmed, ignoreCase = true) && 
        it.profileId == currentProfileId && 
        it.categoryId != category?.categoryId 
    }
    
    val isFormValid = !isNameEmpty && !isNameTooLong && !isNameDuplicate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (category == null) "New Category" else "Edit Category", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    isError = isNameTooLong || isNameDuplicate,
                    supportingText = {
                        if (isNameTooLong) Text("Maximum 20 characters")
                        else if (isNameDuplicate) Text("Category name already exists")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text("Select Color", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))

                // Color Picker Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorPalette.take(6).forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isFormValid) onSave(nameTrimmed, selectedColor) },
                enabled = isFormValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}