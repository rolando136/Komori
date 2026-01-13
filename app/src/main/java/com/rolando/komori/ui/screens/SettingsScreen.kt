package com.rolando.komori.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rolando.komori.data.ThemeManager
import com.rolando.komori.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeManager = remember { ThemeManager(context) }

    // Observe the theme preference
    val isDarkTheme by themeManager.isDarkTheme.collectAsState(initial = false)

    Scaffold(
        // 1. Dynamic Background (White in Light, Dark Grey/Black in Dark)
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
                        // 2. Dynamic Title Color
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            // 3. Dynamic Icon Color
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
        ) {
            // Appearance Header
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelMedium,
                color = StoneGray // StoneGray looks good on both backgrounds
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dark Mode Toggle Row
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
                        // 4. Dynamic Label Color
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
                        checkedTrackColor = SageGreen,
                        // Unchecked Colors:
                        uncheckedThumbColor = StoneGray,
                        // Make sure the track is visible in Dark Mode (Dark Grey) vs Light Mode (Light Green)
                        uncheckedTrackColor = if (isDarkTheme) Color(0xFF444444) else MistGreen,
                        uncheckedBorderColor = if (isDarkTheme) Color(0xFF666666) else MistGreen
                    )
                )
            }
        }
    }
}