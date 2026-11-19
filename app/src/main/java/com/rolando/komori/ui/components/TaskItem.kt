package com.rolando.komori.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.ArrowsClockwise
import com.adamglin.phosphoricons.bold.Check
import com.rolando.komori.data.Task
import com.rolando.komori.data.TaskRepeat
import com.rolando.komori.ui.theme.AppFont
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onItemClick: () -> Unit,
    onBlockedClick: (() -> Unit)? = null,
    showDivider: Boolean = true
) {
    // --- DYNAMIC THEME COLORS ---
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val titleColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    // --- PROGRESS LOGIC ---
    val totalSubtasks = task.subtasks.size
    val completedSubtasks = task.subtasks.count { it.isCompleted }

    val progress = if (totalSubtasks > 0) {
        completedSubtasks.toFloat() / totalSubtasks
    } else {
        0f
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
            // 1. Checkbox (Rounded Square)
            val checkboxShape = RoundedCornerShape(8.dp)
            val hasIncompleteSubtasks = task.subtasks.isNotEmpty() && !task.subtasks.all { it.isCompleted }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(checkboxShape)
                    // Fills the background completely when checked
                    .background(if (task.isCompleted) primaryColor else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (task.isCompleted) primaryColor else outlineColor,
                        shape = checkboxShape
                    )
                    .clickable {
                        if (!task.isCompleted && hasIncompleteSubtasks) {
                            onBlockedClick?.invoke() ?: onItemClick()
                        } else {
                            onCheckedChange(!task.isCompleted)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Adds a clean white checkmark when completed
                if (task.isCompleted) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
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
                    color = if (task.isCompleted) tertiaryColor else titleColor,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (dateTimeString.isNotEmpty() || task.repeat != TaskRepeat.NONE) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.repeat != TaskRepeat.NONE) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(primaryColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Bold.ArrowsClockwise,
                                    contentDescription = task.repeat.label,
                                    tint = primaryColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = task.repeat.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontFamily = AppFont,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = primaryColor
                                )
                            }
                            if (dateTimeString.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                        if (dateTimeString.isNotEmpty()) {
                            Text(
                                text = dateTimeString,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = AppFont,
                                    fontSize = 12.sp
                                ),
                                color = if (task.isCompleted) tertiaryColor else primaryColor
                            )
                        }
                    }
                }
            }

            // 3. Progress Circle (Right)
            if (totalSubtasks > 0) {
                Spacer(modifier = Modifier.width(16.dp))

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(32.dp),
                        color = if (task.isCompleted) tertiaryColor.copy(alpha = 0.5f) else primaryColor,
                        trackColor = tertiaryColor.copy(alpha = 0.15f),
                        strokeWidth = 3.dp,
                        strokeCap = StrokeCap.Round
                    )

                    if (!task.isCompleted) {
                        Text(
                            text = "${(progress * 100).toInt()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = tertiaryColor,
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
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}