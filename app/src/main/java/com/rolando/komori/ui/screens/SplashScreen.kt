package com.rolando.komori.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.Plant
import com.rolando.komori.ui.theme.AppFont
import com.rolando.komori.ui.theme.SageGreen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {

    // 1. Animation State
    val scale = remember { Animatable(0f) }

    // 2. Logic: Animate -> Wait -> Navigate
    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )

        // This delay simulates "loading" time.
        // In a real app, you could check for data loading here instead.
        delay(700)

        // Navigate to 'home' and remove 'splash' from backstack
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    // 3. UI Overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = PhosphorIcons.Bold.Plant, // Using the new stable import
                contentDescription = "Logo",
                tint = SageGreen,
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Komori",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = AppFont,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.scale(scale.value)
            )
        }
    }
}