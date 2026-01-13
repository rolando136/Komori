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
import com.rolando.komori.ui.screens.SplashScreen
import com.rolando.komori.ui.screens.TasksScreen
import com.rolando.komori.ui.theme.KomoriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themeManager = ThemeManager(applicationContext)

        setContent {
            val isDarkTheme by themeManager.isDarkTheme.collectAsState(initial = false)

            KomoriTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                // START DESTINATION IS SPLASH
                NavHost(navController = navController, startDestination = "splash") {

                    composable("splash") {
                        SplashScreen(navController = navController)
                    }

                    // Route must match what we called in SplashScreen ("home")
                    composable("home") {
                        TasksScreen(navController = navController)
                    }

                    composable("settings") {
                        SettingsScreen(navController = navController)
                    }
                }
            }
        }
    }
}