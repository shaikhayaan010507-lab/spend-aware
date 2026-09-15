package com.ayaansaksham.spend_aware.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DbAccentDark,
    secondary = DbOrangeAccent,
    background = DbBackgroundDark,
    surface = DbSurfaceDark,
    onPrimary = DbBackgroundDark,
    onSecondary = DbOrangeText,
    onBackground = DbMainTextDark,
    onSurface = DbMainTextDark,
    outline = DbOutlineDark,
    outlineVariant = DbSubduedDark,
    error = DbErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = DbPrimaryTeal,
    secondary = DbOrangeAccent,
    background = DbBackgroundLight,
    surface = DbSurfaceLight,
    onPrimary = DbSurfaceLight,
    onSecondary = DbOrangeText,
    onBackground = DbMainTextLight,
    onSurface = DbMainTextLight,
    outline = DbInputOutlineLight,
    outlineVariant = DbOutlineLight,
    error = DbError,
    errorContainer = DbErrorContainerLight,
    primaryContainer = DbPaleTeal,
    onPrimaryContainer = DbPrimaryTeal,
    surfaceVariant = DbPaleBlue
)

@Composable
fun SpendawareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor false to strictly adhere to the DailyBudget design spec colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}