package com.rolando.komori.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.Minus
import com.adamglin.phosphoricons.bold.Plus
import com.adamglin.phosphoricons.bold.X
import com.rolando.komori.data.Subtask
import com.rolando.komori.ui.theme.AppFont
import com.rolando.komori.ui.theme.SageGreen
import com.rolando.komori.ui.theme.StoneGray


@Composable
fun SubtaskEntrySection(
    subtasks: List<Subtask>,
    onAddSubtask: (String) -> Unit,
    onRemoveSubtask: (Int) -> Unit
) {
    var newSubtaskText by remember { mutableStateOf("") }

    // FIX: Dynamic Theme Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 1. LIST OF EXISTING SUBTASKS
        subtasks.forEachIndexed { index, subtask ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = subtask.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onRemoveSubtask(index) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.Minus,
                        contentDescription = "Remove",
                        tint = tertiaryColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 2. INPUT ROW (Add New)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Input Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceColor)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (newSubtaskText.isEmpty()) {
                    Text(
                        text = "Add new step...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tertiaryColor.copy(alpha = 0.7f)
                    )
                }
                BasicTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = AppFont
                    ),
                    cursorBrush = SolidColor(primaryColor),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Add Button
            IconButton(
                onClick = {
                    if (newSubtaskText.isNotBlank()) {
                        onAddSubtask(newSubtaskText)
                        newSubtaskText = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    // FIX: Use Primary Color Background
                    .background(primaryColor.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = PhosphorIcons.Bold.Plus,
                    contentDescription = "Add",
                    // FIX: Use Primary Color Tint
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}