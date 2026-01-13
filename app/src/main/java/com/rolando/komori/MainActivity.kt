package com.rolando.komori

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rolando.komori.data.ThemeManager
import com.rolando.komori.ui.screens.SettingsScreen
import com.rolando.komori.ui.screens.TasksScreen
import com.rolando.komori.ui.theme.KomoriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Theme Manager
        val themeManager = ThemeManager(applicationContext)

        setContent {
            // 1. Listen to the stored theme preference
            val isDarkTheme by themeManager.isDarkTheme.collectAsState(initial = false)

            // 2. Apply the theme to the WHOLE app
            KomoriTheme(darkTheme = isDarkTheme) {

                // 3. Setup Navigation
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "tasks") {

                    // Main Screen
                    composable("tasks") {
                        TasksScreen(navController = navController)
                    }

                    // Settings Screen
                    composable("settings") {
                        SettingsScreen(navController = navController)
                    }
                }
            }
        }
    }
}