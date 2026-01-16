package com.rolando.komori.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolando.komori.data.Task
import com.rolando.komori.ui.theme.AppFont
import com.rolando.komori.ui.theme.SageGreen
import com.rolando.komori.ui.theme.StoneGray
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
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

    // --- PROGRESS LOGIC ---
    val totalSubtasks = task.subtasks.size
    val completedSubtasks = task.subtasks.count { it.isCompleted }

    // Calculate percentage only if subtasks exist
    val progress = if (totalSubtasks > 0) {
        completedSubtasks.toFloat() / totalSubtasks
    } else {
        0f // Default to 0 if no subtasks (won't be shown anyway)
    }

    // --- DATE FORMATTING ---
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    val dateTimeString = remember(task.date, task.time) {
        val datePart = task.date?.format(dateFormatter) ?: ""
        val timePart = task.time?.format(timeFormatter) ?: ""

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
            // 1. Checkbox (Left)
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

            // 2. Text Content (Middle)
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

            // 3. Progress Circle (Right) - ONLY SHOW IF SUBTASKS EXIST
            if (totalSubtasks > 0) {
                Spacer(modifier = Modifier.width(16.dp))

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(32.dp),
                        color = if (task.isCompleted) StoneGray.copy(alpha = 0.5f) else greenAccent,
                        trackColor = StoneGray.copy(alpha = 0.15f),
                        strokeWidth = 3.dp,
                        strokeCap = StrokeCap.Round
                    )

                    if (!task.isCompleted) {
                        Text(
                            text = "${(progress * 100).toInt()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = StoneGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
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