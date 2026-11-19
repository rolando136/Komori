package com.rolando.komori.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.Database
import com.adamglin.phosphoricons.bold.DownloadSimple
import com.adamglin.phosphoricons.bold.Export
import com.adamglin.phosphoricons.bold.UploadSimple
import com.rolando.komori.data.ThemeManager
import com.rolando.komori.ui.TaskViewModel
import com.rolando.komori.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    currentTheme: AppThemeOption,
    onThemeSelected: (AppThemeOption) -> Unit,
    taskViewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory)
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val isDarkTheme by themeManager.isDarkTheme.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingJsonContent by remember { mutableStateOf<String?>(null) }
    var clearExistingOnRestore by remember { mutableStateOf(false) }

    // Launcher for saving export JSON file to device storage
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = taskViewModel.exportBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    Toast.makeText(context, "Backup saved successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Launcher for picking a JSON file to import
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
                        }
                    }
                    if (!json.isNullOrBlank()) {
                        pendingJsonContent = json
                        showRestoreConfirmDialog = true
                    } else {
                        Toast.makeText(context, "Backup file is empty", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Read failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = AppFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // --- APPEARANCE SECTION ---
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = AppFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = StoneGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Dark Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Dark Mode",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = AppFont,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isDarkTheme) "On" else "Off",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppFont),
                        color = StoneGray
                    )
                }

                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { isChecked ->
                        scope.launch {
                            themeManager.saveTheme(isChecked)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = StoneGray,
                        uncheckedTrackColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFE0E0E0),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Color Theme Selector
            ThemeSelector(
                currentTheme = currentTheme,
                onThemeSelected = onThemeSelected
            )

            Spacer(modifier = Modifier.height(36.dp))

            // --- DATA & BACKUP SECTION ---
            Text(
                text = "DATA & BACKUP",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = AppFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = StoneGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.Database,
                                contentDescription = "Backup",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Komori Data Backup",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = AppFont
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Export or restore tasks, schedule, rituals, and notes as portable JSON.",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppFont),
                                color = StoneGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Export Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                                } else {
                                    System.currentTimeMillis().toString()
                                }
                                exportFileLauncher.launch("komori_backup_$timestamp.json")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.DownloadSimple,
                                contentDescription = "Export File",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export JSON",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = AppFont
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val json = taskViewModel.exportBackupJson()
                                        shareJson(context, json)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.Export,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = AppFont
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Restore Button
                    OutlinedButton(
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/json", "text/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Bold.UploadSimple,
                            contentDescription = "Restore JSON",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Import Backup From File",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AppFont
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog for importing JSON backup
    if (showRestoreConfirmDialog && pendingJsonContent != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingJsonContent = null
            },
            title = {
                Text(
                    text = "Restore Komori Data",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = AppFont,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose how to import the backup data:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppFont)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearExistingOnRestore = !clearExistingOnRestore }
                    ) {
                        Checkbox(
                            checked = clearExistingOnRestore,
                            onCheckedChange = { clearExistingOnRestore = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Replace existing data",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = AppFont
                                )
                            )
                            Text(
                                text = "If unchecked, items will merge with existing ones.",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppFont),
                                color = StoneGray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingJsonContent ?: return@Button
                        scope.launch {
                            val result = taskViewModel.importBackupJson(json, clearExistingOnRestore)
                            result.onSuccess { summary ->
                                Toast.makeText(
                                    context,
                                    "Restored: ${summary.tasksCount} tasks, ${summary.habitsCount} rituals, ${summary.eventsCount} events, ${summary.notesCount} notes",
                                    Toast.LENGTH_LONG
                                ).show()
                            }.onFailure { err ->
                                Toast.makeText(
                                    context,
                                    "Import failed: ${err.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            showRestoreConfirmDialog = false
                            pendingJsonContent = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Import Now", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingJsonContent = null
                }) {
                    Text("Cancel", color = StoneGray)
                }
            }
        )
    }
}

private fun shareJson(context: Context, json: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, json)
        type = "application/json"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Komori Backup")
    context.startActivity(shareIntent)
}

// --- HELPER COMPOSABLES ---

@Composable
fun ThemeSelector(
    currentTheme: AppThemeOption,
    onThemeSelected: (AppThemeOption) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Color Theme",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = AppFont,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose your preferred accent color",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppFont),
            color = StoneGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppThemeOption.entries.forEach { theme ->
                ThemeCircle(
                    theme = theme,
                    isSelected = currentTheme == theme,
                    onClick = { onThemeSelected(theme) }
                )
            }
        }
    }
}

@Composable
fun ThemeCircle(theme: AppThemeOption, isSelected: Boolean, onClick: () -> Unit) {
    val color = when (theme) {
        AppThemeOption.Komori -> SageGreen
        AppThemeOption.Coffee -> CoffeePrimary
        AppThemeOption.Ocean -> OceanPrimary
        AppThemeOption.Sakura -> SakuraPrimary
        AppThemeOption.Lavender -> LavenderPrimary
        AppThemeOption.Paper -> PaperPrimary
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onBackground,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
