package com.ayaansaksham.spend_aware.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class ScreenRoute(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TRANSACTIONS("Expenses", Icons.Default.CalendarMonth),
    BUDGET("Budget", Icons.Default.AccountBalanceWallet),
    ANALYTICS("Analytics", Icons.Default.Analytics),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentRoute by remember { mutableStateOf(ScreenRoute.HOME) }

    Scaffold(
        floatingActionButton = {
            if (currentRoute == ScreenRoute.HOME || currentRoute == ScreenRoute.TRANSACTIONS) {
                FloatingActionButton(
                    onClick = { /* We will wire this to AddExpenseScreen later */ },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            NavigationBar(tonalElevation = 8.dp) {
                ScreenRoute.values().forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen,
                        onClick = { currentRoute = screen },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentRoute) {
                // This calls your newly pasted Dashboard directly
                ScreenRoute.HOME -> DashboardScreen()
                ScreenRoute.TRANSACTIONS -> Text("Transactions & Calendar going here next", modifier = Modifier.padding(16.dp))
                ScreenRoute.BUDGET -> Text("Budget & Rules going here", modifier = Modifier.padding(16.dp))
                ScreenRoute.ANALYTICS -> Text("Analytics Charts going here", modifier = Modifier.padding(16.dp))
                ScreenRoute.SETTINGS -> Text("Settings going here", modifier = Modifier.padding(16.dp))
            }
        }
    }
}