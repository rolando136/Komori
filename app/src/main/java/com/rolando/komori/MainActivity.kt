package com.rolando.komori

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // <--- IMPORT THIS
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rolando.komori.data.ThemeManager
import com.rolando.komori.ui.TaskViewModel
import com.rolando.komori.ui.ThemeViewModel
import com.rolando.komori.ui.screens.AnalyticsScreen
import com.rolando.komori.ui.screens.SettingsScreen
import com.rolando.komori.ui.screens.SplashScreen
import com.rolando.komori.ui.screens.TasksScreen
import com.rolando.komori.ui.theme.KomoriTheme


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIX: This enables the app to draw behind the status bar
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            val themeViewModel: ThemeViewModel = viewModel()
            val taskViewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory)
            val context = LocalContext.current
            val themeManager = remember { ThemeManager(context) }

            // Observe States
            val currentTheme by themeViewModel.currentTheme.collectAsState()
            val isDarkTheme by themeManager.isDarkTheme.collectAsState(initial = isSystemInDarkTheme())

            KomoriTheme(
                themeOption = currentTheme,
                darkTheme = isDarkTheme
            ) {
                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(navController = navController)
                    }
                    composable("home") {
                        TasksScreen(navController = navController, viewModel = taskViewModel)
                    }
                    composable("settings") {
                        SettingsScreen(
                            navController = navController,
                            currentTheme = currentTheme,
                            onThemeSelected = { newTheme ->
                                themeViewModel.setTheme(newTheme)
                            },
                            taskViewModel = taskViewModel
                        )
                    }
                    composable("analytics") {
                        AnalyticsScreen(navController = navController, viewModel = taskViewModel)
                    }
                }
            }
        }
    }
}