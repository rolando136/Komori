package com.rolando.komori.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rolando.komori.data.ThemeManager
import com.rolando.komori.ui.theme.*
import kotlinx.coroutines.launch
import com.rolando.komori.ui.theme.AppThemeOption // Make sure this Enum is imported from Theme.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    // 1. NEW PARAMETERS for Color Theme
    currentTheme: AppThemeOption,
    onThemeSelected: (AppThemeOption) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeManager = remember { ThemeManager(context) }

    // Observe the dark mode preference
    val isDarkTheme by themeManager.isDarkTheme.collectAsState(initial = false)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = AppFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()) // Allow scrolling for small screens
        ) {
            // --- APPEARANCE SECTION ---
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelMedium,
                color = StoneGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Dark Mode Toggle (Existing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Dark Mode",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = AppFont,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isDarkTheme) "On" else "Off",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppFont),
                        color = StoneGray
                    )
                }

                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { isChecked ->
                        scope.launch {
                            themeManager.saveTheme(isChecked)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary, // Use dynamic primary color
                        uncheckedThumbColor = StoneGray,
                        uncheckedTrackColor = if (isDarkTheme) Color(0xFF444444) else MistGreen,
                        uncheckedBorderColor = if (isDarkTheme) Color(0xFF666666) else MistGreen
                    )
                )
            }

            Divider(modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // 2. Color Theme Selector (New)
            ThemeSelector(
                currentTheme = currentTheme,
                onThemeSelected = onThemeSelected
            )
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun ThemeSelector(
    currentTheme: AppThemeOption,
    onThemeSelected: (AppThemeOption) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Color Theme",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = AppFont,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose your preferred accent color",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppFont),
            color = StoneGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppThemeOption.entries.forEach { theme ->
                ThemeCircle(
                    theme = theme,
                    isSelected = currentTheme == theme,
                    onClick = { onThemeSelected(theme) }
                )
            }
        }
    }
}

@Composable
fun ThemeCircle(theme: AppThemeOption, isSelected: Boolean, onClick: () -> Unit) {
    // Map the enum to the main primary color of that theme
    // Ensure these colors are imported from Color.kt
    val color = when (theme) {
        AppThemeOption.Komori -> SageGreen
        AppThemeOption.Coffee -> CoffeePrimary
        AppThemeOption.Ocean -> OceanPrimary
        AppThemeOption.Sakura -> SakuraPrimary
        AppThemeOption.Lavender -> LavenderPrimary
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onBackground, // Border matches text color
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Show checkmark if selected
        if (isSelected) {
            // If you don't have PhosphorIcons.Bold.Check, use standard Icons.Default.Check
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}