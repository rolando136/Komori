package com.rolando.komori.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. UPDATE ENUM
enum class AppThemeOption {
    Komori,   // Green
    Coffee,   // Brown
    Ocean,    // Blue
    Sakura,   // Pink (New)
    Lavender  // Purple (New)
}

// --- EXISTING SCHEMES (Green, Coffee, Ocean) ---
// ... (Keep your Green, Coffee, and Ocean schemes exactly as they were) ...

// --- UPDATED SCHEMES (Subtle & Relaxing Backgrounds) ---

// 1. KOMORI (Sage Green)
private val GreenLightScheme = lightColorScheme(
    primary = SageGreen, secondary = SageGreen, tertiary = StoneGray,
    background = Color(0xFFF4F7F4),
    surface = Color.White, onPrimary = Color.White,
    surfaceContainerHigh = MistGreen,
    surfaceVariant = MistGreen
)
private val GreenDarkScheme = darkColorScheme(
    primary = SageGreen, secondary = SageGreen, tertiary = MistGreen,
    // Deepest Forest Black
    background = Color(0xFF0A0C0A),
    surface = Color(0xFF141614),
    onPrimary = Color.White,
    surfaceContainerHigh = Color(0xFF1F221F),
    surfaceVariant = Color(0xFF1F221F)
)

// 2. COFFEE (Brown)
private val CoffeeLightScheme = lightColorScheme(
    primary = CoffeePrimary, secondary = CoffeeDark, tertiary = CoffeeDark,
    background = Color(0xFFFAF9F8),
    surface = Color.White, onPrimary = Color.White,
    surfaceContainerHigh = CoffeeSurface,
    surfaceVariant = CoffeeSurface
)
private val CoffeeDarkScheme = darkColorScheme(
    primary = CoffeePrimary, secondary = Latte, tertiary = Latte,
    // Deepest Espresso Black
    background = Color(0xFF0C0B0A),
    surface = Color(0xFF161514),
    onPrimary = Color.White,
    surfaceContainerHigh = Color(0xFF22201E),
    surfaceVariant = Color(0xFF22201E)
)

// 3. OCEAN (Blue)
private val OceanLightScheme = lightColorScheme(
    primary = OceanPrimary, secondary = OceanDark, tertiary = OceanDark,
    background = Color(0xFFF2F8FA),
    surface = Color.White, onPrimary = Color.White,
    surfaceContainerHigh = OceanSurface,
    surfaceVariant = OceanSurface
)
private val OceanDarkScheme = darkColorScheme(
    primary = OceanPrimary, secondary = Foam, tertiary = Foam,
    // Deepest Abyss Black
    background = Color(0xFF080A0C),
    surface = Color(0xFF101316),
    onPrimary = Color.White,
    surfaceContainerHigh = Color(0xFF1A1E22),
    surfaceVariant = Color(0xFF1A1E22)
)

// 4. SAKURA (Pink)
private val SakuraLightScheme = lightColorScheme(
    primary = SakuraPrimary, secondary = SakuraDark, tertiary = SakuraDark,
    background = Color(0xFFFFF8FA),
    surface = Color.White, onPrimary = Color.White,
    surfaceContainerHigh = SakuraSurface,
    surfaceVariant = SakuraSurface
)
private val SakuraDarkScheme = darkColorScheme(
    primary = SakuraPrimary, secondary = Petal, tertiary = Petal,
    // Deepest Midnight Rose
    background = Color(0xFF0C090A),
    surface = Color(0xFF171214),
    onPrimary = Color.White,
    surfaceContainerHigh = Color(0xFF241C1F),
    surfaceVariant = Color(0xFF241C1F)
)

// 5. LAVENDER (Purple)
private val LavenderLightScheme = lightColorScheme(
    primary = LavenderPrimary, secondary = LavenderDark, tertiary = LavenderDark,
    background = Color(0xFFFBF9FC),
    surface = Color.White, onPrimary = Color.White,
    surfaceContainerHigh = LavenderSurface,
    surfaceVariant = LavenderSurface
)
private val LavenderDarkScheme = darkColorScheme(
    primary = LavenderPrimary, secondary = Wisteria, tertiary = Wisteria,
    // Deepest Void Purple
    background = Color(0xFF0A090C),
    surface = Color(0xFF141217),
    onPrimary = Color.White,
    surfaceContainerHigh = Color(0xFF201D25),
    surfaceVariant = Color(0xFF201D25)
)

@Composable
fun KomoriTheme(
    themeOption: AppThemeOption = AppThemeOption.Komori,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 2. UPDATE SELECTION LOGIC
    val colorScheme = when (themeOption) {
        AppThemeOption.Komori   -> if (darkTheme) GreenDarkScheme else GreenLightScheme
        AppThemeOption.Coffee   -> if (darkTheme) CoffeeDarkScheme else CoffeeLightScheme
        AppThemeOption.Ocean    -> if (darkTheme) OceanDarkScheme else OceanLightScheme
        AppThemeOption.Sakura   -> if (darkTheme) SakuraDarkScheme else SakuraLightScheme
        AppThemeOption.Lavender -> if (darkTheme) LavenderDarkScheme else LavenderLightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}