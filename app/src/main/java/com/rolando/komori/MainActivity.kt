package com.rolando.komori

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
        enableEdgeToEdge()

        val themeManager = ThemeManager(applicationContext)

        setContent {
            // 1. Wait for Theme to load (Prevents wrong-theme flash)
            val isDarkTheme by themeManager.isDarkTheme.collectAsState(initial = null)
            if (isDarkTheme == null) return@setContent

            KomoriTheme(darkTheme = isDarkTheme!!) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "splash") {

                    // --- SPLASH SCREEN ---
                    composable(
                        route = "splash",
                        // When leaving Splash, Fade Out slowly (1 second)
                        exitTransition = {
                            fadeOut(animationSpec = tween(1000))
                        }
                    ) {
                        SplashScreen(navController = navController)
                    }

                    // --- HOME SCREEN ---
                    composable(
                        route = "home",
                        // When entering Home, Fade In slowly (1 second)
                        enterTransition = {
                            fadeIn(animationSpec = tween(1000))
                        }
                    ) {
                        TasksScreen(navController = navController)
                    }

                    // --- SETTINGS SCREEN ---
                    composable(
                        route = "settings",
                        // Standard slide animation for settings (optional)
                        enterTransition = { fadeIn(animationSpec = tween(500)) },
                        exitTransition = { fadeOut(animationSpec = tween(500)) }
                    ) {
                        SettingsScreen(navController = navController)
                    }
                }
            }
        }
    }
}