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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentProfileId: Int = 1,
    onProfileSelected: (Int) -> Unit = {}
) {
    var showProfileSwitcher by remember { mutableStateOf(false) }

    // Snackbar state for features that are coming soon
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

            // Account & Profile Section
            item {
                SectionHeader("Account & Profile")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        // Current Profile Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentProfileId == 1) {
                                    Text("A", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                } else {
                                    Text("J", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (currentProfileId == 1) {
                                    Text("Ayaan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Text("Student Profile", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    Text("John Doe", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Text("Employee Profile", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            TextButton(onClick = { showNotImplemented() }) {
                                Text("Edit")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsRow(
                            icon = Icons.Outlined.SwapHoriz,
                            title = "Switch Account",
                            subtitle = "Change to Employee Demo",
                            onClick = { showProfileSwitcher = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsRow(
                            icon = Icons.Outlined.Payments,
                            title = "Currency",
                            value = "INR (₹)",
                            onClick = { showNotImplemented() }
                        )
                    }
                }
            }

            // Budget Settings Section
            item {
                SectionHeader("Budget Settings")
                val incomeLabel = if (currentProfileId == 1) "Monthly Allowance" else "Income / Salary"
                var rolloverChecked by remember { mutableStateOf(true) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        SettingsRow(icon = Icons.Outlined.AccountBalanceWallet, title = incomeLabel, onClick = { showNotImplemented() })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsRow(icon = Icons.Outlined.Savings, title = "Savings Goal", onClick = { showNotImplemented() })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsRow(icon = Icons.Outlined.WarningAmber, title = "Alert Thresholds", value = "80%, 100%", onClick = { showNotImplemented() })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsSwitchRow(
                            icon = Icons.Outlined.Loop,
                            title = "Rollover Unused Budget",
                            subtitle = "Redistribute remaining funds",
                            checked = rolloverChecked,
                            onCheckedChange = { rolloverChecked = it }
                        )
                    }
                }
            }

            // Categories & Payments Section
            item {
                SectionHeader("Categories & Payments")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        SettingsRow(icon = Icons.Outlined.Category, title = "Manage Categories", onClick = { showNotImplemented() })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsRow(icon = Icons.Outlined.CreditCard, title = "Payment Methods", onClick = { showNotImplemented() })
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
                        SettingsRow(icon = Icons.Outlined.Download, title = "Export Data (CSV)", onClick = { showNotImplemented() })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsRow(icon = Icons.Outlined.UploadFile, title = "Import Data (JSON)", onClick = { showNotImplemented() })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // Make Dark Mode functional
                        SettingsRow(icon = Icons.Outlined.DarkMode, title = "Dark Mode", value = "System", onClick = { showNotImplemented() })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsRow(
                            icon = Icons.Outlined.DeleteForever,
                            title = "Reset Budget",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { showNotImplemented() },
                            showChevron = false
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    var activeProfileId by remember(currentProfileId) { mutableStateOf(currentProfileId) }

    if (showProfileSwitcher) {
        AlertDialog(
            onDismissRequest = { showProfileSwitcher = false },
            title = { Text("Fast Demo Switch", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select a profile to load its isolated data:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Profile 1 Option
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            activeProfileId = 1
                            onProfileSelected(1)
                            showProfileSwitcher = false
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeProfileId == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                Text("A", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Ayaan", fontWeight = FontWeight.Bold, color = if (activeProfileId == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground)
                                Text("Student Profile", fontSize = 12.sp, color = if (activeProfileId == 1) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Profile 2 Option
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            activeProfileId = 2
                            onProfileSelected(2)
                            showProfileSwitcher = false
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeProfileId == 2) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
                                Text("J", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("John Doe", fontWeight = FontWeight.Bold, color = if (activeProfileId == 2) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onBackground)
                                Text("Employee Profile", fontSize = 12.sp, color = if (activeProfileId == 2) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileSwitcher = false }) {
                    Text("Cancel")
                }
            }
        )
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}