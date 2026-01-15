package com.rolando.komori.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rolando.komori.ui.theme.AppFont
import com.rolando.komori.ui.theme.MistGreen
import com.rolando.komori.ui.theme.SageGreen
import com.rolando.komori.ui.theme.StoneGray

@Composable
fun CompactSelector(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Check if app is in Dark Mode
    val isAppInDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Colors
    val containerColor = if (isAppInDarkMode) Color(0xFF333333) else MistGreen.copy(alpha = 0.3f)
    val borderColor = if (isAppInDarkMode) SageGreen.copy(alpha = 0.5f) else MistGreen
    val textColor = SageGreen

    Column(modifier = modifier) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = StoneGray,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Button Surface
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onClick() }
        ) {
            Box(
                modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AppFont
                    ),
                    color = textColor
                )
            }
        }
    }
}