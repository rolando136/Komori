package com.rolando.komori.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.MagnifyingGlass
import com.adamglin.phosphoricons.bold.PushPin
import com.adamglin.phosphoricons.bold.PushPinSimple
import com.adamglin.phosphoricons.bold.X
import com.rolando.komori.data.Note
import com.rolando.komori.ui.components.KomoriDialog
import com.rolando.komori.ui.theme.AppFont
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    notes: List<Note>,
    onNoteClick: (Note) -> Unit,
    onPinToggle: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar (only show if there are notes or if query active)
        if (notes.isNotEmpty() || searchQuery.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.MagnifyingGlass,
                        contentDescription = "Search",
                        tint = tertiaryColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search notes...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = tertiaryColor.copy(alpha = 0.6f)
                                )
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = primaryColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.X,
                                contentDescription = "Clear search",
                                tint = tertiaryColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No matching notes found."
                    else "No notes yet.\nTap + to write.",
                    color = tertiaryColor,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onPinToggle = { onPinToggle(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onPinToggle: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(note.timestamp) { dateFormatter.format(Date(note.timestamp)) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = surfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (note.title.isNotBlank()) note.title else "Untitled",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFont
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (note.isPinned) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.PushPin,
                        contentDescription = "Pinned",
                        tint = primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onPinToggle() }
                    )
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp
                    ),
                    color = tertiaryColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontFamily = AppFont
                ),
                color = tertiaryColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, isPinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    KomoriDialog(
        title = "New Note",
        titleAlign = TextAlign.Center,
        onDismiss = onDismiss,
        saveButtonText = "Save Note",
        onSave = {
            if (title.isNotBlank() || content.isNotBlank()) {
                onSave(title.trim(), content.trim(), isPinned)
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        "Title",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = tertiaryColor.copy(alpha = 0.5f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = primaryColor
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Pin row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isPinned = !isPinned }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isPinned) PhosphorIcons.Bold.PushPin else PhosphorIcons.Bold.PushPinSimple,
                    contentDescription = "Pin Note",
                    tint = if (isPinned) primaryColor else tertiaryColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPinned) "Pinned to top" else "Pin to top",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPinned) primaryColor else tertiaryColor.copy(alpha = 0.7f)
                )
            }

            HorizontalDivider(
                color = tertiaryColor.copy(alpha = 0.15f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = {
                    Text(
                        "Write your thoughts...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = tertiaryColor.copy(alpha = 0.5f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = primaryColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 280.dp)
            )
        }
    }
}

@Composable
fun EditNoteDialog(
    note: Note,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Note) -> Unit
) {
    var editedTitle by remember { mutableStateOf(note.title) }
    var editedContent by remember { mutableStateOf(note.content) }
    var isPinned by remember { mutableStateOf(note.isPinned) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    KomoriDialog(
        title = "Edit Note",
        titleAlign = TextAlign.Center,
        onDismiss = onDismiss,
        onDelete = onDelete,
        saveButtonText = "Save",
        onSave = {
            onSave(
                note.copy(
                    title = editedTitle.trim(),
                    content = editedContent.trim(),
                    isPinned = isPinned
                )
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = editedTitle,
                onValueChange = { editedTitle = it },
                placeholder = {
                    Text(
                        "Title",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = tertiaryColor.copy(alpha = 0.5f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = primaryColor
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Pin row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isPinned = !isPinned }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isPinned) PhosphorIcons.Bold.PushPin else PhosphorIcons.Bold.PushPinSimple,
                    contentDescription = "Pin Note",
                    tint = if (isPinned) primaryColor else tertiaryColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPinned) "Pinned to top" else "Pin to top",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPinned) primaryColor else tertiaryColor.copy(alpha = 0.7f)
                )
            }

            HorizontalDivider(
                color = tertiaryColor.copy(alpha = 0.15f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            TextField(
                value = editedContent,
                onValueChange = { editedContent = it },
                placeholder = {
                    Text(
                        "Write your thoughts...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = tertiaryColor.copy(alpha = 0.5f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = primaryColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 280.dp)
            )
        }
    }
}
