package com.rolando.komori.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.Minus
import com.adamglin.phosphoricons.bold.Plus
import com.rolando.komori.data.Subroutine
import com.rolando.komori.ui.theme.AppFont

@Composable
fun SubroutineEntrySection(
    subroutines: List<Subroutine>,
    onAddSubroutine: (String) -> Unit,
    onRemoveSubroutine: (Int) -> Unit
) {
    var newText by remember { mutableStateOf("") }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // List of existing subroutines
        subroutines.forEachIndexed { index, sub ->
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
                    text = sub.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppFont),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onRemoveSubroutine(index) },
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

        // Add input row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceColor)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (newText.isEmpty()) {
                    Text(
                        text = "Add routine step...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppFont),
                        color = tertiaryColor.copy(alpha = 0.7f)
                    )
                }
                BasicTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = AppFont
                    ),
                    cursorBrush = SolidColor(primaryColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newText.isNotBlank()) {
                            onAddSubroutine(newText.trim())
                            newText = ""
                        }
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Add button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryColor)
                    .clickable {
                        if (newText.isNotBlank()) {
                            onAddSubroutine(newText.trim())
                            newText = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PhosphorIcons.Bold.Plus,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
