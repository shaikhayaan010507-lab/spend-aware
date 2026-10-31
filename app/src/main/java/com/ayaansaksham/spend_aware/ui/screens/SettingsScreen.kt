package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaansaksham.spend_aware.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    expenses: kotlinx.coroutines.flow.StateFlow<List<com.ayaansaksham.spend_aware.data.local.entity.Expense>>? = null,
    categories: kotlinx.coroutines.flow.StateFlow<List<com.ayaansaksham.spend_aware.data.local.entity.Category>>? = null,
    onNavigateToCategories: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val userPersona by settingsViewModel.userPersona.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val initial = userPersona.firstOrNull()?.uppercaseChar() ?: 'S'
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), // Dynamic primary
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initial.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(userPersona, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("$userPersona Profile", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = userPersona == "Student",
                                onClick = { 
                                    settingsViewModel.updatePersona("Student")
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("Student")
                            }
                            SegmentedButton(
                                selected = userPersona == "Employee",
                                onClick = { 
                                    settingsViewModel.updatePersona("Employee")
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("Employee")
                            }
                        }
                    }
                }
            }

            // Categories Section
            item {
                SectionHeader("Categories")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        SettingsRow(icon = Icons.Outlined.Category, title = "Manage Categories", onClick = onNavigateToCategories)
                    }
                }
            }

            // Notifications Section
            item {
                SectionHeader("Notifications")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    var remindersChecked by remember { mutableStateOf(true) }
                    var alertsChecked by remember { mutableStateOf(true) }
                    
                    Column {
                        SettingsSwitchRow(
                            icon = Icons.Outlined.NotificationsActive, 
                            title = "Daily Reminders", 
                            checked = remindersChecked, 
                            onCheckedChange = { remindersChecked = it }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsSwitchRow(
                            icon = Icons.Outlined.ErrorOutline, 
                            title = "Budget Alerts", 
                            checked = alertsChecked, 
                            onCheckedChange = { alertsChecked = it }
                        )
                    }
                }
            }

            // Data & Privacy Section
            item {
                SectionHeader("Data & Privacy")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        val currentExpenses by (expenses ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
                        val currentCategories by (categories ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
                        val context = androidx.compose.ui.platform.LocalContext.current

                        val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
                        ) { uri ->
                            if (uri != null) {
                                coroutineScope.launch {
                                    val result = com.ayaansaksham.spend_aware.utils.CsvExporter.exportExpensesToCsv(
                                        context = context,
                                        uri = uri,
                                        expenses = currentExpenses,
                                        categories = currentCategories
                                    )
                                    result.onSuccess { count ->
                                        snackbarHostState.showSnackbar("Successfully exported $count expenses!")
                                    }.onFailure {
                                        snackbarHostState.showSnackbar("Failed to export: ${it.localizedMessage}")
                                    }
                                }
                            }
                        }

                        SettingsRow(
                            icon = Icons.Outlined.Download,
                            title = "Export Data (CSV)",
                            onClick = {
                                if (currentExpenses.isEmpty()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("No transactions available to export.")
                                    }
                                } else {
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                                    exportLauncher.launch("SpendAware_Export_$dateStr.csv")
                                }
                            }
                        )
                    }
                }
            }

            // Display Section
            item {
                SectionHeader("Display")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        val themeMode by settingsViewModel.themeMode.collectAsState()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "App Theme",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            SegmentedButton(
                                selected = themeMode == "System",
                                onClick = { settingsViewModel.updateThemeMode("System") },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) { Text("System") }
                            SegmentedButton(
                                selected = themeMode == "Light",
                                onClick = { settingsViewModel.updateThemeMode("Light") },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) { Text("Light") }
                            SegmentedButton(
                                selected = themeMode == "Dark",
                                onClick = { settingsViewModel.updateThemeMode("Dark") },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) { Text("Dark") }
                        }
                    }
                }
            }

            // Danger Zone Section
            item {
                SectionHeader("Danger Zone")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Outlined.Logout,
                            title = "Log Out",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = onLogout,
                            showChevron = false
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value != null) {
            Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (showChevron) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val thumbColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.outline,
        label = "thumbColor"
    )
    val trackColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "trackColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = thumbColor,
                checkedTrackColor = trackColor,
                uncheckedThumbColor = thumbColor,
                uncheckedTrackColor = trackColor
            )
        )
    }
}