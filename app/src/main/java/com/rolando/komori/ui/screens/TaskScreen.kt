package com.rolando.komori.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.rolando.komori.data.Task
import com.rolando.komori.ui.TaskViewModel
import com.rolando.komori.ui.components.TaskItem
import com.rolando.komori.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

// --- PHOSPHOR ICONS IMPORTS (BOLD) ---
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    navController: NavController,
    viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory)
) {
    val taskList by viewModel.allTasks.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var isCompletedExpanded by remember { mutableStateOf(false) }

    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDetails by remember { mutableStateOf("") }
    var newTaskDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var newTaskTime by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    val isDark = isSystemInDarkTheme()
    val buttonBgColor = if (isDark) Color(0xFF333333) else MistGreen
    val dialogSurfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val dialogTextColor = MaterialTheme.colorScheme.onSurface

    // --- NAVIGATION STATE (PAGER) ---
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 1) { 4 }
    val currentScreen = AppScreen.entries[pagerState.currentPage]

    val activeTasks = remember(taskList) {
        taskList.filter { !it.isCompleted }
    }

    val completedTasks = remember(taskList) {
        taskList.filter { it.isCompleted }
    }

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return "Pick a Date"
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,

        // --- DYNAMIC HEADER ---
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            AppScreen.Calendar -> "Schedule"
                            AppScreen.Tasks -> "My Focus"
                            AppScreen.Habits -> "Rituals"
                            AppScreen.Notes -> "Notes"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = AppFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            // PHOSPHOR BOLD: Gear
                            imageVector = PhosphorIcons.Bold.Gear,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },

        // --- FLOATING DOCK & FAB ---
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {

            val dockBias by animateFloatAsState(
                targetValue = if (currentScreen == AppScreen.Tasks) -1f else 0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "DockPosition"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {

                // --- NAVIGATION DOCK ---
                Box(
                    modifier = Modifier.align(
                        BiasAlignment(
                            horizontalBias = dockBias,
                            verticalBias = 0f
                        )
                    )
                ) {
                    FloatingNavBar(
                        currentScreen = currentScreen,
                        onScreenSelected = { screen ->
                            scope.launch {
                                pagerState.animateScrollToPage(screen.ordinal)
                            }
                        }
                    )
                }

                // --- FAB CONTAINER ---
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentScreen == AppScreen.Tasks,
                        enter = slideInVertically(
                            initialOffsetY = { it * 2 },
                            animationSpec = tween(durationMillis = 500, delayMillis = 450)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 500, delayMillis = 450)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it * 2 },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 300)
                        )
                    ) {
                        FloatingActionButton(
                            onClick = {
                                newTaskTitle = ""
                                newTaskDetails = ""
                                newTaskTime = null
                                newTaskDate = null
                                showDialog = true
                            },
                            containerColor = SageGreen,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            elevation = FloatingActionButtonDefaults.elevation(4.dp, 2.dp),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                // PHOSPHOR BOLD: Plus
                                imageVector = PhosphorIcons.Bold.Plus,
                                contentDescription = "New Focus",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            beyondViewportPageCount = 3
        ) { pageIndex ->

            when (AppScreen.entries[pageIndex]) {
                AppScreen.Calendar -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Calendar / Schedule Coming Soon", color = StoneGray)
                    }
                }

                AppScreen.Tasks -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        if (activeTasks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize().padding(bottom = 100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No focus yet.\nTap + to start.",
                                        color = StoneGray,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            item { SectionHeader("ACTIVE FOCUS") }
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 0.dp
                                ) {
                                    Column {
                                        activeTasks.forEachIndexed { index, task ->
                                            TaskItem(
                                                task = task,
                                                onCheckedChange = { isChecked -> viewModel.toggleTask(task, isChecked) },
                                                onItemClick = { selectedTask = task },
                                                showDivider = index < activeTasks.lastIndex
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (completedTasks.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isCompletedExpanded = !isCompletedExpanded }
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("COMPLETED (${completedTasks.size})", style = MaterialTheme.typography.labelMedium, color = StoneGray)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        // PHOSPHOR BOLD: CaretUp / CaretDown
                                        imageVector = if (isCompletedExpanded) PhosphorIcons.Bold.CaretUp else PhosphorIcons.Bold.CaretDown,
                                        contentDescription = "Expand",
                                        tint = StoneGray
                                    )
                                }
                            }
                            if (isCompletedExpanded) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    ) {
                                        Column {
                                            completedTasks.forEachIndexed { index, task ->
                                                TaskItem(
                                                    task = task,
                                                    onCheckedChange = { isChecked -> viewModel.toggleTask(task, isChecked) },
                                                    onItemClick = { selectedTask = task },
                                                    showDivider = index < completedTasks.lastIndex
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AppScreen.Habits -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Habit Tracker Coming Soon", color = StoneGray)
                        }
                    }
                }

                AppScreen.Notes -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Notes & Habits Coming Soon", color = StoneGray)
                    }
                }
            }

        }

    }

    if (selectedTask != null) {
        TaskDetailDialog(
            task = selectedTask!!,
            onDismiss = { selectedTask = null },
            onDelete = {
                viewModel.deleteTask(selectedTask!!)
                selectedTask = null
            },
            onSave = { updatedTask ->
                viewModel.updateTask(updatedTask)
                selectedTask = null
            }
        )
    }

    // --- MAIN ADD TASK DIALOG ---
    if (showDialog) {
        val isAppInDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val currentButtonColor = if (isAppInDarkMode) Color(0xFF333333) else MistGreen

        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(0.94f).padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "New Focus",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold, fontFamily = AppFont
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    CalmInputTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = "What's the core task?",
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CalmInputTextField(
                        value = newTaskDetails,
                        onValueChange = { newTaskDetails = it },
                        label = "Any details?",
                        singleLine = false,
                        minLines = 3,
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Date Button
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(currentButtonColor)
                                .clickable { showDatePicker = true }
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                // PHOSPHOR BOLD: CalendarBlank
                                imageVector = PhosphorIcons.Bold.CalendarBlank,
                                contentDescription = null,
                                tint = SageGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (newTaskDate != null) formatDate(newTaskDate) else "Date",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppFont),
                                color = if (newTaskDate != null) SageGreen else StoneGray
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Time Button
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(currentButtonColor)
                                .clickable { showTimePicker = true }
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                // PHOSPHOR BOLD: Clock
                                imageVector = PhosphorIcons.Bold.Clock,
                                contentDescription = null,
                                tint = SageGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = newTaskTime ?: "Time",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppFont),
                                color = if (newTaskTime != null) SageGreen else StoneGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                viewModel.addTask(newTaskTitle, newTaskDetails, newTaskDate, newTaskTime)
                                showDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(
                            text = "Add Focus",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = AppFont)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { showDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = StoneGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Not now",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppFont)
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG PICKERS ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            colors = DatePickerDefaults.colors(containerColor = dialogSurfaceColor),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(0.88f),
            confirmButton = {
                TextButton(onClick = {
                    newTaskDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK", color = SageGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = StoneGray) }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = StoneGray,
                    headlineContentColor = dialogTextColor,
                    weekdayContentColor = StoneGray,
                    subheadContentColor = dialogTextColor,
                    yearContentColor = dialogTextColor,
                    dayContentColor = dialogTextColor,
                    todayContentColor = SageGreen,
                    todayDateBorderColor = SageGreen,
                    selectedDayContainerColor = SageGreen,
                    selectedDayContentColor = Color.White,
                    navigationContentColor = dialogTextColor
                )
            )
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState()
        val isAppInDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f

        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(0.94f).padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Select time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(24.dp))
                    TimePicker(
                        state = timeState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = if (isAppInDarkMode) Color(0xFF444444) else MistGreen,
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                            selectorColor = SageGreen,
                            periodSelectorBorderColor = SageGreen,
                            periodSelectorSelectedContainerColor = SageGreen.copy(alpha = 0.2f),
                            periodSelectorSelectedContentColor = SageGreen,
                            periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = StoneGray) }
                        TextButton(onClick = {
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, timeState.hour)
                            cal.set(Calendar.MINUTE, timeState.minute)
                            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            newTaskTime = sdf.format(cal.time)
                            showTimePicker = false
                        }) { Text("OK", color = SageGreen) }
                    }
                }
            }
        }
    }
}

// ... CalmInputTextField ...
@Composable
fun CalmInputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    val isAppInDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val inputBoxColor = if (isAppInDarkMode) Color(0xFF333333) else NatureWhite
    val textColor = MaterialTheme.colorScheme.onSurface
    val placeholderColor = StoneGray

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = placeholderColor, fontFamily = AppFont) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = textColor,
            fontFamily = AppFont
        ),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = inputBoxColor,
            unfocusedContainerColor = inputBoxColor,
            disabledContainerColor = inputBoxColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = SageGreen
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = StoneGray,
        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp, top = 16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDialog(
    task: Task,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Task) -> Unit
) {
    var editedTitle by remember { mutableStateOf(task.title) }
    var editedDetails by remember { mutableStateOf(task.details) }
    var editedDate by remember { mutableStateOf(task.dueDate) }
    var editedTime by remember { mutableStateOf(task.dueTime) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val isAppInDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val dialogBgColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val mainTextColor = MaterialTheme.colorScheme.onSurface
    val genericContainerColor = if (isAppInDarkMode) Color(0xFF333333) else NatureWhite
    val genericContentColor = if (isAppInDarkMode) Color(0xFFE0E0E0) else Color(0xFF2E7D32)
    val statusBg = Color(0xFFFFE6BA).copy(alpha = if (isAppInDarkMode) 0.8f else 0.6f)
    val statusText = Color(0xFF6B4E18)
    val deleteBg = if (isAppInDarkMode) Color(0xFF5A1D1D) else Color(0xFFFFCDD2)
    val deleteText = if (isAppInDarkMode) Color(0xFFFFB4AB) else Color(0xFFC62828)

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = if (editedDate != null) dateFormat.format(Date(editedDate!!)) else "Set Date"
    val timeString = editedTime ?: "Set Time"
    val statusLabel = if (task.isCompleted) "Completed" else "In Progress"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = dialogBgColor,
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())
            ) {
                // 1. HEADER ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    TextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        placeholder = { Text("Task Name", color = StoneGray, fontSize = 24.sp) },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFont,
                            fontSize = 24.sp
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = SageGreen,
                            focusedTextColor = mainTextColor,
                            unfocusedTextColor = mainTextColor
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        // PHOSPHOR BOLD: X (Close)
                        imageVector = PhosphorIcons.Bold.X,
                        contentDescription = "Close",
                        tint = if (isAppInDarkMode) Color.LightGray else Color.Gray,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onDismiss() }
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = editedDetails,
                    onValueChange = { editedDetails = it },
                    placeholder = { Text("Add details...", color = StoneGray) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFont,
                        lineHeight = 22.sp
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = genericContainerColor,
                        unfocusedContainerColor = genericContainerColor,
                        disabledContainerColor = genericContainerColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = SageGreen,
                        focusedTextColor = mainTextColor,
                        unfocusedTextColor = mainTextColor
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(genericContainerColor)
                            .clickable { showDatePicker = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = AppFont,
                                fontWeight = FontWeight.Medium
                            ),
                            color = genericContentColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(genericContainerColor)
                            .clickable { showTimePicker = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = AppFont,
                                fontWeight = FontWeight.Medium
                            ),
                            color = genericContentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(statusBg)
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = AppFont,
                                fontWeight = FontWeight.Medium
                            ),
                            color = statusText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val updatedTask = task.copy(
                            title = editedTitle,
                            details = editedDetails,
                            dueDate = editedDate,
                            dueTime = editedTime
                        )
                        onSave(updatedTask)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = AppFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = deleteBg,
                        contentColor = deleteText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        text = "Delete Focus",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = AppFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    // --- REUSED DATE/TIME PICKERS (For Edit Dialog) ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    editedDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK", color = SageGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = StoneGray) }
            },
            colors = DatePickerDefaults.colors(
                containerColor = dialogBgColor,
                yearContentColor = mainTextColor,
                navigationContentColor = mainTextColor,
                headlineContentColor = mainTextColor,
                subheadContentColor = mainTextColor,
                weekdayContentColor = mainTextColor,
                dayContentColor = mainTextColor
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialCal = Calendar.getInstance()
        if (editedTime != null) {
            try {
                val parsed = SimpleDateFormat("hh:mm a", Locale.getDefault()).parse(editedTime!!)
                if (parsed != null) initialCal.time = parsed
            } catch (e: Exception) { e.printStackTrace() }
        }

        val timeState = rememberTimePickerState(
            initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = initialCal.get(Calendar.MINUTE)
        )

        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, timeState.hour)
                cal.set(Calendar.MINUTE, timeState.minute)
                editedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
                showTimePicker = false
            },
            timeState = timeState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    timeState: TimePickerState
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select time", style = MaterialTheme.typography.labelLarge, color = StoneGray)
                Spacer(modifier = Modifier.height(24.dp))
                TimePicker(
                    state = timeState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MistGreen,
                        selectorColor = SageGreen,
                        periodSelectorSelectedContainerColor = SageGreen.copy(alpha = 0.2f),
                        periodSelectorSelectedContentColor = SageGreen
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = StoneGray) }
                    TextButton(onClick = onConfirm) { Text("OK", color = SageGreen) }
                }
            }
        }
    }
}

@Composable
fun FloatingNavBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {
    val isAppInDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val dockColor = if (isAppInDarkMode) Color(0xFF333333) else Color.White
    val inactiveIconColor = if (isAppInDarkMode) Color.LightGray else StoneGray

    Surface(
        color = dockColor,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Calendar
            NavBarIcon(
                // PHOSPHOR BOLD: CalendarBlank
                icon = PhosphorIcons.Bold.CalendarBlank,
                isSelected = currentScreen == AppScreen.Calendar,
                inactiveColor = inactiveIconColor,
                onClick = { onScreenSelected(AppScreen.Calendar) }
            )

            // 2. Tasks
            NavBarIcon(
                // PHOSPHOR BOLD: CheckCircle
                icon = PhosphorIcons.Bold.CheckCircle,
                isSelected = currentScreen == AppScreen.Tasks,
                inactiveColor = inactiveIconColor,
                onClick = { onScreenSelected(AppScreen.Tasks) }
            )

            // 3. Habits
            NavBarIcon(
                // PHOSPHOR BOLD: Repeat
                icon = PhosphorIcons.Bold.Repeat,
                isSelected = currentScreen == AppScreen.Habits,
                inactiveColor = inactiveIconColor,
                onClick = { onScreenSelected(AppScreen.Habits) }
            )

            // 4. Notes
            NavBarIcon(
                // PHOSPHOR BOLD: Notepad
                icon = PhosphorIcons.Bold.Notepad,
                isSelected = currentScreen == AppScreen.Notes,
                inactiveColor = inactiveIconColor,
                onClick = { onScreenSelected(AppScreen.Notes) }
            )
        }
    }
}

@Composable
fun NavBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) SageGreen else inactiveColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

enum class AppScreen {
    Calendar,
    Tasks,
    Habits,
    Notes
}