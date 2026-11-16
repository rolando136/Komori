package com.rolando.komori.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.CalendarBlank
import com.adamglin.phosphoricons.bold.Clock
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
    // FIX: Dynamic Theme Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val isSet = value != "Set Date" && value != "Set Time"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor,
        // FIX: Border uses primary color if set, otherwise transparent
        border = BorderStroke(1.dp, if (isSet) primaryColor.copy(alpha = 0.5f) else Color.Transparent),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = if (label == "Date") PhosphorIcons.Bold.CalendarBlank else PhosphorIcons.Bold.Clock,
                contentDescription = null,
                // FIX: Icon tint
                tint = if (isSet) primaryColor else tertiaryColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSet) FontWeight.SemiBold else FontWeight.Normal
                ),
                // FIX: Text color
                color = if (isSet) primaryColor else tertiaryColor
            )
        }
    }
}