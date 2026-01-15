package com.rolando.komori.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember // Import Remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolando.komori.data.Task
import com.rolando.komori.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onItemClick: () -> Unit,
    showDivider: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val titleColor = MaterialTheme.colorScheme.onSurface
    val greenAccent = SageGreen
    val circleBorderColor = if (isDark) Color.Gray else Color(0xFFC5C5C5)

    // PERFORMANCE FIX:
    // Create the formatter once and remember it.
    // This stops the "lag" caused by creating heavy objects during scrolling/swiping.
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val dateText = if (task.dueDate != null) dateFormatter.format(Date(task.dueDate)) else ""

    // Helper logic to format text efficiently
    val dateTimeString = remember(task.dueDate, task.dueTime) {
        val datePart = if (task.dueDate != null) dateFormatter.format(Date(task.dueDate)) else ""
        val timePart = task.dueTime ?: ""

        buildString {
            if (datePart.isNotEmpty()) append(datePart)
            if (datePart.isNotEmpty() && timePart.isNotEmpty()) append(" • ")
            if (timePart.isNotEmpty()) append(timePart)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (task.isCompleted) greenAccent else circleBorderColor,
                        shape = CircleShape
                    )
                    .clickable { onCheckedChange(!task.isCompleted) },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(greenAccent)
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // 2. Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = AppFont
                    ),
                    color = if (task.isCompleted) StoneGray else titleColor,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 3. Date & Time (Optimized)
                if (dateTimeString.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dateTimeString,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontFamily = AppFont,
                            fontSize = 12.sp
                        ),
                        color = if (task.isCompleted) StoneGray else greenAccent
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                thickness = 1.dp,
                color = if (isDark) Color(0xFF333333) else Color(0xFFF0F0F0)
            )
        }
    }
}