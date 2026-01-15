package com.rolando.komori.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.rolando.komori.ui.theme.AppFont
import com.rolando.komori.ui.theme.SageGreen
import com.rolando.komori.ui.theme.StoneGray
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.Trash
import com.adamglin.phosphoricons.bold.X

@Composable
fun KomoriDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null, // If null, we assume it's an "Add" dialog
    saveButtonText: String = "Save",
    saveButtonIcon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        SideEffect {
            dialogWindowProvider?.window?.setDimAmount(0.3f)
        }
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- 1. HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = AppFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // If Edit Mode: Show Trash.
                    // If Add Mode: Show X (Close).
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.Trash,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.X,
                                contentDescription = "Close",
                                tint = StoneGray,

                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. CONTENT SLOT ---
                content()

                Spacer(modifier = Modifier.height(32.dp))

                // --- 3. FOOTER (DYNAMIC) ---
                if (onDelete != null) {
                    // CASE A: EDIT MODE (Has Delete option)
                    // Show Cancel button + Smaller Save button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = StoneGray)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        KomoriButton(
                            text = saveButtonText,
                            icon = saveButtonIcon,
                            onClick = onSave,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                } else {
                    // CASE B: ADD MODE (No Delete option)
                    // Cancel is handled by the 'X' in header.
                    // Save button fills the full width.
                    KomoriButton(
                        text = saveButtonText,
                        icon = saveButtonIcon,
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}