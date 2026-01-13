package com.rolando.komori.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- COLOR DEFINITIONS ---

// 1. Dark Mode Palette
private val DarkColorScheme = darkColorScheme(
    primary = SageGreen,
    background = Color(0xFF121212),       // Almost black
    surface = Color(0xFF1E1E1E),          // Dark Grey for cards
    surfaceContainerHigh = Color(0xFF2C2C2C), // Lighter Grey for Dialogs
    onBackground = NatureWhite,           // Text is white
    onSurface = NatureWhite,
    onPrimary = Color.White
)

// 2. Light Mode Palette
private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    background = NatureWhite,             // Your custom soft white
    surface = Color.White,                // Pure white for cards
    surfaceContainerHigh = Color.White,   // White for Dialogs
    onBackground = PrimaryBlack,          // Text is black
    onSurface = PrimaryBlack,
    onPrimary = Color.White
)

@Composable
fun KomoriTheme(
    // This 'darkTheme' boolean comes from your MainActivity/Settings
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to force YOUR colors (SageGreen etc)
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // 1. Set Status Bar Color to match the App Background
            window.statusBarColor = colorScheme.background.toArgb()

            // 2. Control the Icon Colors (Time, Battery)
            // If Dark Theme is ON, we want LIGHT icons (isAppearanceLightStatusBars = false)
            // If Dark Theme is OFF, we want DARK icons (isAppearanceLightStatusBars = true)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}