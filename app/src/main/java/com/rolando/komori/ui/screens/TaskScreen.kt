package com.rolando.komori.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.ArrowsClockwise
import com.adamglin.phosphoricons.bold.CalendarBlank
import com.adamglin.phosphoricons.bold.CalendarPlus
import com.adamglin.phosphoricons.bold.CaretDown
import com.adamglin.phosphoricons.bold.CaretUp
import com.adamglin.phosphoricons.bold.Check
import com.adamglin.phosphoricons.bold.CheckCircle
import com.adamglin.phosphoricons.bold.Circle
import com.adamglin.phosphoricons.bold.Flag
import com.adamglin.phosphoricons.bold.ChartPieSlice
import com.adamglin.phosphoricons.bold.Gear
import com.adamglin.phosphoricons.bold.ListPlus
import com.adamglin.phosphoricons.bold.Minus
import com.adamglin.phosphoricons.bold.Note
import com.adamglin.phosphoricons.bold.PencilSimple
import com.adamglin.phosphoricons.bold.Plus
import com.adamglin.phosphoricons.bold.TextAlignLeft
import com.rolando.komori.data.Habit
import com.rolando.komori.data.Note
import com.rolando.komori.data.Subtask
import com.rolando.komori.data.Task
import com.rolando.komori.data.TaskPriority
import com.rolando.komori.data.WeeklyEvent
import com.rolando.komori.ui.TaskViewModel
import com.rolando.komori.ui.components.*
import com.rolando.komori.ui.theme.AppFont
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// --- 1. ENUM DEFINITION (Must be top-level) ---
enum class AppScreen {
    Calendar,
    Tasks,
    Habits,
    Notes
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    navController: NavController,
    viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory)
) {
    val taskList by viewModel.allTasks.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var isCompletedExpanded by remember { mutableStateOf(false) }

    // Task Dialog States
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDetails by remember { mutableStateOf("") }
    var newTaskDate by remember { mutableStateOf<LocalDate?>(null) }
    var newTaskTime by remember { mutableStateOf<LocalTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    val weeklyEvents by viewModel.weeklyEvents.collectAsState(initial = emptyList())
    var jumpToDate by remember { mutableStateOf<LocalDate?>(null) }
    var currentMonth by remember { mutableStateOf(LocalDate.now()) }

    var newTaskPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    val newTaskSubtasks = remember { mutableStateListOf<Subtask>() }

    // --- THEME COLORS ---
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // --- NAVIGATION STATE ---
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 1) { 4 }
    val currentScreen = AppScreen.entries[pagerState.currentPage]

    val scheduleEvents by viewModel.scheduleEvents.collectAsState()
    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedScheduleEvent by remember { mutableStateOf<WeeklyEvent?>(null) }
    var isEditingSchedule by remember { mutableStateOf(false) }

    // Habit States
    val habits by viewModel.allHabits.collectAsState()
    var showHabitDialog by remember { mutableStateOf(false) }
    var selectedHabit by remember { mutableStateOf<Habit?>(null) }

    // Note States
    val notes by viewModel.allNotes.collectAsState()
    var showNoteDialog by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }

    // --- SAFE LIST HANDLING & SORTING ---
    val allActiveTasks = remember(taskList) {
        taskList?.filter { !it.isCompleted }
            ?.sortedWith(
                compareBy<Task, LocalDate?>(nullsLast()) { it.date }
                    .thenBy(nullsLast()) { it.time }
            ) ?: emptyList()
    }

    val highPriorityTasks = remember(allActiveTasks) { allActiveTasks.filter { it.priority == TaskPriority.HIGH } }
    val standardTasks = remember(allActiveTasks) { allActiveTasks.filter { it.priority != TaskPriority.HIGH } }
    val completedTasks = remember(taskList) { taskList?.filter { it.isCompleted } ?: emptyList() }

    var showMonthPicker by remember { mutableStateOf(false) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    var headerTitle by remember { mutableStateOf(LocalDate.now().format(monthFormatter)) }
    var targetJumpDate by remember { mutableStateOf<LocalDate?>(null) }

    val isAnyDialogOpen = showDialog || showScheduleDialog || selectedTask != null ||
            selectedScheduleEvent != null || showMonthPicker ||
            showHabitDialog || selectedHabit != null ||
            showNoteDialog || selectedNote != null
    val blurRadius by animateDpAsState(targetValue = if (isAnyDialogOpen) 12.dp else 0.dp, animationSpec = tween(durationMillis = 200), label = "BlurAnimation")

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    // --- NAVBAR HIDE ON SCROLL LOGIC ---
    val bottomBarHeight = 56.dp // <-- REDUCED HEIGHT
    val bottomBarHeightPx = with(LocalDensity.current) { bottomBarHeight.toPx() }
    var bottomBarOffsetHeightPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If scrolling down (available.y < 0), we increase offset to push bar off-screen
                // If scrolling up (available.y > 0), we decrease offset to pull bar back on-screen
                val delta = available.y
                val newOffset = bottomBarOffsetHeightPx - delta
                bottomBarOffsetHeightPx = newOffset.coerceIn(0f, bottomBarHeightPx)
                return Offset.Zero
            }
        }
    }

    // Outer Box allows the Navbar to overlay the Scaffold content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Scaffold(
            modifier = Modifier.blur(blurRadius),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        if (currentScreen == AppScreen.Calendar) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { showMonthPicker = true }.padding(8.dp)
                            ) {
                                Text(text = headerTitle, style = MaterialTheme.typography.titleLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = PhosphorIcons.Bold.CaretDown, contentDescription = "Select Month", tint = primaryColor, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Text(
                                text = when (currentScreen) {
                                    AppScreen.Tasks -> "My Focus"
                                    AppScreen.Habits -> "Rituals"
                                    AppScreen.Notes -> "Notes"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { navController.navigate("analytics") },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = PhosphorIcons.Bold.ChartPieSlice,
                                        contentDescription = "Reports & Analytics",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(16.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                )
                                IconButton(
                                    onClick = { navController.navigate("settings") },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = PhosphorIcons.Bold.Gear,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { innerPadding ->
            HorizontalPager(state = pagerState, modifier = Modifier.padding(innerPadding).fillMaxSize(), beyondViewportPageCount = 3) { pageIndex ->
                when (AppScreen.entries[pageIndex]) {
                    AppScreen.Calendar -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ScheduleScreen(
                                events = weeklyEvents, jumpToDate = jumpToDate, onVisibleDateChanged = { date -> currentMonth = date },
                                onBottomReached = {}, onEventClick = { event -> selectedScheduleEvent = event },
                                onAddEvent = { newEvent -> viewModel.insertWeeklyEvent(newEvent) },
                                onFabVisibilityChange = { /* Not needed for fixed navbar */ }
                            )
                        } else { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Calendar requires Android O or newer", color = tertiaryColor) } }
                    }
                    AppScreen.Tasks -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            // FIX 1: Added top = 24.dp to push the containers down from the "My Focus" header
                            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
                        ) {
                            // EMPTY STATE (No tasks at all)
                            if (allActiveTasks.isEmpty() && completedTasks.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxSize().padding(bottom = 100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No focus yet.\nTap + to start.",
                                            color = tertiaryColor,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                // --- SECTION 1: HIGH PRIORITY ---
                                if (highPriorityTasks.isNotEmpty()) {
                                    item {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, Color(TaskPriority.LOW.color).copy(alpha = 0.3f)),
                                            shadowElevation = 0.dp
                                        ) {
                                            Column {
                                                SectionHeader("ACTIVE FOCUS")
                                                // FIX 2: Added spacer between the text and the first task
                                                Spacer(modifier = Modifier.height(8.dp))

                                                highPriorityTasks.forEachIndexed { index, task ->
                                                    TaskItem(
                                                        task = task,
                                                        onCheckedChange = { isChecked -> viewModel.toggleTask(task, isChecked) },
                                                        onItemClick = { selectedTask = task },
                                                        showDivider = index < highPriorityTasks.lastIndex
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                }

                                // --- SECTION 2: STANDARD TASKS ---
                                if (standardTasks.isNotEmpty()) {
                                    item {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            shadowElevation = 0.dp
                                        ) {
                                            Column {
                                                if (highPriorityTasks.isEmpty()) {
                                                    SectionHeader("ACTIVE FOCUS")
                                                    // FIX 2: Added spacer here too
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }

                                                standardTasks.forEachIndexed { index, task ->
                                                    TaskItem(
                                                        task = task,
                                                        onCheckedChange = { isChecked -> viewModel.toggleTask(task, isChecked) },
                                                        onItemClick = { selectedTask = task },
                                                        showDivider = index < standardTasks.lastIndex
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // --- SECTION 3: COMPLETED ---
                            if (completedTasks.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))

                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { isCompletedExpanded = !isCompletedExpanded }
                                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "COMPLETED (${completedTasks.size})",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    // Fades the text
                                                    color = tertiaryColor.copy(alpha = 0.5f)
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(
                                                    imageVector = if (isCompletedExpanded) PhosphorIcons.Bold.CaretUp else PhosphorIcons.Bold.CaretDown,
                                                    contentDescription = "Expand",
                                                    // Fades the arrow icon to match
                                                    tint = tertiaryColor.copy(alpha = 0.5f)
                                                )
                                            }

                                            if (isCompletedExpanded) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 24.dp),
                                                    thickness = 1.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                )

                                                // FIX 3: Added spacer to match the active focus spacing
                                                Spacer(modifier = Modifier.height(8.dp))

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
                        HabitsScreen(
                            habits = habits,
                            onDateToggle = { habit, date ->
                                viewModel.toggleHabitDate(habit, date)
                            },
                            onHabitClick = { habit ->
                                selectedHabit = habit
                            }
                        )
                    }
                    AppScreen.Notes -> {
                        NotesScreen(
                            notes = notes,
                            onNoteClick = { note ->
                                selectedNote = note
                            },
                            onPinToggle = { note ->
                                viewModel.toggleNotePin(note)
                            }
                        )
                    }
                }
            }
        }

        // --- BOTTOM NAVBAR ---
        BottomNavBar(
            currentScreen = currentScreen,
            onScreenSelected = { screen -> scope.launch { pagerState.animateScrollToPage(screen.ordinal) } },
            onAddClick = {
                when (currentScreen) {
                    AppScreen.Tasks -> {
                        newTaskTitle = ""; newTaskDetails = ""; newTaskTime = null; newTaskDate = null; showDialog = true
                    }
                    AppScreen.Calendar -> {
                        showScheduleDialog = true
                    }
                    AppScreen.Habits -> {
                        showHabitDialog = true
                    }
                    AppScreen.Notes -> {
                        showNoteDialog = true
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Animate the Y offset to hide/show the navbar
                .offset { IntOffset(x = 0, y = bottomBarOffsetHeightPx.roundToInt()) }
        )
    }

    if (selectedTask != null) {
        var isEditing by remember(selectedTask) { mutableStateOf(false) }
        if (isEditing) {
            EditTaskDialog(task = selectedTask!!, onDismiss = { isEditing = false }, onDelete = { viewModel.deleteTask(selectedTask!!); selectedTask = null }, onSave = { updatedTask -> viewModel.updateTask(updatedTask); selectedTask = null })
        } else {
            TaskDetailDialog(task = selectedTask!!, onDismiss = { selectedTask = null }, onEdit = { isEditing = true }, onDelete = { viewModel.deleteTask(selectedTask!!); selectedTask = null }, onUpdateTask = { updatedTask -> viewModel.updateTask(updatedTask); selectedTask = updatedTask })
        }
    }

    if (showDialog) {
        val itemBgColor = MaterialTheme.colorScheme.surfaceContainerHigh
        val expandedBgColor = MaterialTheme.colorScheme.surface
        val expandedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

        val itemShape = RoundedCornerShape(14.dp)
        var showDetails by remember { mutableStateOf(false) }
        var showPriority by remember { mutableStateOf(false) }
        var showDateTime by remember { mutableStateOf(false) }
        var showSubtasks by remember { mutableStateOf(newTaskSubtasks.isNotEmpty()) }

        KomoriDialog(
            title = "New Focus",
            titleAlign = TextAlign.Center,
            onDismiss = { showDialog = false; newTaskTitle = ""; newTaskDetails = ""; newTaskDate = null; newTaskTime = null; newTaskPriority = TaskPriority.MEDIUM; newTaskSubtasks.clear() },
            onDelete = null,
            saveButtonText = "Save Focus",
            onSave = {
                if (newTaskTitle.isNotBlank()) {
                    val taskToSave = Task(title = newTaskTitle, priority = newTaskPriority, date = newTaskDate, time = newTaskTime, details = newTaskDetails, subtasks = newTaskSubtasks.toList(), isCompleted = false)
                    viewModel.addTask(taskToSave); showDialog = false; newTaskTitle = ""; newTaskDetails = ""; newTaskDate = null; newTaskTime = null; newTaskPriority = TaskPriority.MEDIUM; newTaskSubtasks.clear()
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = newTaskTitle, onValueChange = { newTaskTitle = it },
                    placeholder = { Text("What needs to be done?", style = MaterialTheme.typography.headlineSmall.copy(color = tertiaryColor.copy(alpha = 0.5f))) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = primaryColor),
                    modifier = Modifier.fillMaxWidth(), singleLine = false, maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))

                @Composable fun GreenAddOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
                    Surface(shape = itemShape, color = itemBgColor, modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = tertiaryColor)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(imageVector = PhosphorIcons.Bold.Plus, contentDescription = "Add", tint = primaryColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                @Composable fun ExpandedOption(content: @Composable () -> Unit) {
                    Surface(shape = itemShape, color = expandedBgColor, border = BorderStroke(1.dp, expandedBorderColor), modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(16.dp)) { content() } }
                }

                if (!showSubtasks) GreenAddOption("Add Subtasks", PhosphorIcons.Bold.ListPlus) { showSubtasks = true } else ExpandedOption {
                    SectionHeaderWithClose("SUBTASKS", onClose = { showSubtasks = false })
                    SubtaskEntrySection(subtasks = newTaskSubtasks, onAddSubtask = { newTaskSubtasks.add(Subtask(title = it)) }, onRemoveSubtask = { newTaskSubtasks.removeAt(it) })
                }
                if (!showDetails) GreenAddOption("Add Details", PhosphorIcons.Bold.TextAlignLeft) { showDetails = true } else ExpandedOption {
                    SectionHeaderWithClose("DETAILS", onClose = { showDetails = false })
                    OutlinedTextField(value = newTaskDetails, onValueChange = { newTaskDetails = it }, placeholder = { Text("Add notes...", color = tertiaryColor) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor.copy(alpha = 0.5f), unfocusedBorderColor = Color.Transparent, focusedContainerColor = itemBgColor, unfocusedContainerColor = itemBgColor, cursorColor = primaryColor), shape = RoundedCornerShape(12.dp), minLines = 3)
                }
                if (!showDateTime) GreenAddOption("Add Date & Time", PhosphorIcons.Bold.CalendarPlus) { showDateTime = true } else ExpandedOption {
                    SectionHeaderWithClose("WHEN", onClose = { showDateTime = false })
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompactSelector(label = "Date", value = newTaskDate?.format(dateFormatter) ?: "Set Date", onClick = { showDatePicker = true }, modifier = Modifier.weight(1f))
                        CompactSelector(label = "Time", value = newTaskTime?.format(timeFormatter) ?: "Set Time", onClick = { showTimePicker = true }, modifier = Modifier.weight(1f))
                    }
                }
                if (!showPriority) GreenAddOption("Set Priority", PhosphorIcons.Bold.Flag) { showPriority = true } else ExpandedOption {
                    SectionHeaderWithClose("PRIORITY", onClose = { showPriority = false })
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskPriority.entries.forEach { priority ->
                            val isSelected = newTaskPriority == priority
                            val priorityColor = Color(priority.color)
                            val targetColor = if (isSelected) priorityColor else itemBgColor
                            val textColor = if (isSelected) Color.Black else tertiaryColor
                            Box(modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp)).background(targetColor).clickable { newTaskPriority = priority }, contentAlignment = Alignment.Center) {
                                Text(text = priority.label, style = MaterialTheme.typography.labelMedium, color = textColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = newTaskDate?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { newTaskDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("OK", color = primaryColor) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = tertiaryColor) } },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) { DatePicker(state = datePickerState) }
    }
    if (showTimePicker) {
        val initialTime = newTaskTime ?: LocalTime.now()
        val timeState = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute)
        TimePickerDialogShim(onDismiss = { showTimePicker = false }, onConfirm = { newTaskTime = LocalTime.of(timeState.hour, timeState.minute); showTimePicker = false }, state = timeState)
    }

    if (showMonthPicker) { MonthYearPickerDialog(initialDate = LocalDate.now(), onDismiss = { showMonthPicker = false }, onConfirm = { selectedDate -> targetJumpDate = selectedDate; showMonthPicker = false }) }
    if (showScheduleDialog) {
        val today = LocalDate.now(); val defaultTime = LocalTime.of(8, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AddScheduleDialog(initialDate = today, initialStartTime = defaultTime, onDismiss = { showScheduleDialog = false }, onSave = { title, description, category, date, recurrence, start, end, colorInt -> viewModel.addScheduleEvent(title = title, description = description, category = category, startDate = date, recurrence = recurrence, start = start, end = end, color = colorInt); showScheduleDialog = false })
        }
    }
    if (selectedScheduleEvent != null) {
        if (isEditingSchedule) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                EditScheduleDialog(event = selectedScheduleEvent!!, onDismiss = { isEditingSchedule = false }, onDelete = { scope -> viewModel.deleteRecurringScheduleEvent(selectedScheduleEvent!!, scope); selectedScheduleEvent = null; isEditingSchedule = false }, onSave = { updatedEvent -> viewModel.updateScheduleEvent(updatedEvent); selectedScheduleEvent = null; isEditingSchedule = false })
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ScheduleDetailDialog(event = selectedScheduleEvent!!, onDismiss = { selectedScheduleEvent = null }, onEdit = { isEditingSchedule = true }, onDelete = { scope -> viewModel.deleteRecurringScheduleEvent(selectedScheduleEvent!!, scope); selectedScheduleEvent = null })
            }
        }
    }

    if (showHabitDialog) {
        AddHabitDialog(
            onDismiss = { showHabitDialog = false },
            onSave = { name, iconName, targetDays ->
                viewModel.addHabit(name, iconName, targetDays)
                showHabitDialog = false
            }
        )
    }

    if (selectedHabit != null) {
        var isEditingHabit by remember(selectedHabit) { mutableStateOf(false) }
        val currentHabit = habits.firstOrNull { it.id == selectedHabit!!.id } ?: selectedHabit!!

        if (isEditingHabit) {
            EditHabitDialog(
                habit = currentHabit,
                onDismiss = { isEditingHabit = false },
                onSave = { updatedHabit ->
                    viewModel.updateHabit(updatedHabit)
                    selectedHabit = null
                    isEditingHabit = false
                },
                onDelete = {
                    viewModel.deleteHabit(currentHabit)
                    selectedHabit = null
                    isEditingHabit = false
                }
            )
        } else {
            HabitDetailDialog(
                habit = currentHabit,
                onDismiss = { selectedHabit = null },
                onEdit = { isEditingHabit = true },
                onDateToggle = { date ->
                    viewModel.toggleHabitDate(currentHabit, date)
                },
                onDelete = {
                    viewModel.deleteHabit(currentHabit)
                    selectedHabit = null
                }
            )
        }
    }

    if (showNoteDialog) {
        AddNoteDialog(
            onDismiss = { showNoteDialog = false },
            onSave = { title, content, isPinned ->
                viewModel.addNote(title, content, isPinned)
                showNoteDialog = false
            }
        )
    }

    if (selectedNote != null) {
        EditNoteDialog(
            note = selectedNote!!,
            onDismiss = { selectedNote = null },
            onSave = { updatedNote ->
                viewModel.updateNote(updatedNote)
                selectedNote = null
            },
            onDelete = {
                viewModel.deleteNote(selectedNote!!)
                selectedNote = null
            }
        )
    }
}

// --- HELPER COMPONENTS ---

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        // FIX: Add the .copy(alpha = 0.5f) here
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp, top = 16.dp)
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskDetailDialog(
    task: Task,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateTask: (Task) -> Unit
) {
    val totalSubtasks = task.subtasks.size
    val completedSubtasks = task.subtasks.count { it.isCompleted }
    val progress = if (totalSubtasks > 0) completedSubtasks.toFloat() / totalSubtasks else if (task.isCompleted) 1f else 0f
    val percentage = (progress * 100).toInt()
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val priorityColor = Color(task.priority.color)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM dd") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = task.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontFamily = AppFont, fontSize = 22.sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(end = 12.dp), lineHeight = 28.sp)
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(44.dp), color = primaryColor, trackColor = tertiaryColor.copy(alpha = 0.15f), strokeWidth = 4.dp, strokeCap = StrokeCap.Round)
                        Text(text = "$percentage%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
                if (task.subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        task.subtasks.forEachIndexed { index, subtask ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { val newSubtasks = task.subtasks.toMutableList(); newSubtasks[index] = subtask.copy(isCompleted = !subtask.isCompleted); onUpdateTask(task.copy(subtasks = newSubtasks)) }.padding(vertical = 6.dp, horizontal = 4.dp)) {
                                Icon(imageVector = if (subtask.isCompleted) PhosphorIcons.Bold.CheckCircle else PhosphorIcons.Bold.Circle, contentDescription = null, tint = if (subtask.isCompleted) primaryColor else tertiaryColor.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = subtask.title, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), color = if (subtask.isCompleted) tertiaryColor else MaterialTheme.colorScheme.onSurface, textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp)); HorizontalDivider(color = tertiaryColor.copy(alpha = 0.15f)); Spacer(modifier = Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailRow(icon = PhosphorIcons.Bold.Flag, label = "Priority", value = task.priority.label, iconColor = priorityColor)
                    if (task.date != null) DetailRow(icon = PhosphorIcons.Bold.CalendarBlank, label = "Due Date", value = if (task.time != null) "${task.date.format(dateFormatter)} • ${task.time.format(timeFormatter)}" else task.date.format(dateFormatter), iconColor = primaryColor)
                    if (task.details.isNotBlank()) DetailRow(icon = PhosphorIcons.Bold.TextAlignLeft, label = "Description", value = task.details, iconColor = primaryColor, alignTop = true)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(primaryColor.copy(alpha = 0.15f)).border(1.dp, primaryColor, RoundedCornerShape(14.dp)).clickable { onEdit() }, contentAlignment = Alignment.Center) {
                    Icon(imageVector = PhosphorIcons.Bold.PencilSimple, contentDescription = "Edit", tint = primaryColor, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Task) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    var editedTitle by remember { mutableStateOf(task.title) }
    var editedDetails by remember { mutableStateOf(task.details) }
    var editedDate by remember { mutableStateOf(task.date) }
    var editedTime by remember { mutableStateOf(task.time) }
    var editedPriority by remember { mutableStateOf(task.priority) }
    var isCompleted by remember { mutableStateOf(task.isCompleted) }
    val editedSubtasks = remember { mutableStateListOf(*task.subtasks.toTypedArray()) }

    var showDetails by remember { mutableStateOf(task.details.isNotBlank()) }
    var showPriority by remember { mutableStateOf(task.priority != TaskPriority.MEDIUM) }
    var showDateTime by remember { mutableStateOf(task.date != null || task.time != null) }
    var showSubtasks by remember { mutableStateOf(editedSubtasks.isNotEmpty()) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val itemBgColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val expandedBgColor = if (isDark) Color(0xFF333333) else MaterialTheme.colorScheme.surface
    val expandedBorderColor = if (isDark) Color(0xFF555555) else primaryColor.copy(alpha = 0.5f)
    val itemShape = RoundedCornerShape(14.dp)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    KomoriDialog(title = "Edit Focus", onDismiss = onDismiss, onDelete = onDelete, saveButtonText = "Save", titleAlign = TextAlign.Center, onSave = {
        onSave(task.copy(title = editedTitle, priority = editedPriority, date = editedDate, time = editedTime, details = editedDetails, subtasks = editedSubtasks.toList(), isCompleted = isCompleted))
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = editedTitle, onValueChange = { editedTitle = it },
                placeholder = { Text("Task Name", style = MaterialTheme.typography.headlineSmall.copy(color = tertiaryColor.copy(alpha = 0.5f))) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = primaryColor),
                modifier = Modifier.fillMaxWidth(), singleLine = false, maxLines = 2
            )

            Row(modifier = Modifier.fillMaxWidth().height(48.dp).clip(itemShape).background(itemBgColor).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(if (!isCompleted) primaryColor else Color.Transparent).clickable { isCompleted = false }, contentAlignment = Alignment.Center) {
                    Text(text = "In Progress", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = if (!isCompleted) Color.White else tertiaryColor)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(if (isCompleted) primaryColor else Color.Transparent).clickable { isCompleted = true }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCompleted) Icon(PhosphorIcons.Bold.Check, null, tint = Color.White, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                        Text(text = "Completed", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = if (isCompleted) Color.White else tertiaryColor)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            @Composable fun GreenAddOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
                Surface(shape = itemShape, color = itemBgColor, modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = tertiaryColor)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(imageVector = PhosphorIcons.Bold.Plus, contentDescription = "Add", tint = primaryColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            @Composable fun ExpandedOption(content: @Composable () -> Unit) {
                Surface(shape = itemShape, color = expandedBgColor, border = BorderStroke(1.dp, expandedBorderColor), modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(16.dp)) { content() } }
            }

            if (!showSubtasks) GreenAddOption("Add Subtasks", PhosphorIcons.Bold.ListPlus) { showSubtasks = true } else ExpandedOption {
                SectionHeaderWithClose("SUBTASKS", onClose = { showSubtasks = false })
                SubtaskEntrySection(subtasks = editedSubtasks, onAddSubtask = { title -> editedSubtasks.add(Subtask(title = title)) }, onRemoveSubtask = { index -> editedSubtasks.removeAt(index) })
            }
            if (!showDetails) GreenAddOption("Add Details", PhosphorIcons.Bold.TextAlignLeft) { showDetails = true } else ExpandedOption {
                SectionHeaderWithClose("DETAILS", onClose = { showDetails = false })
                OutlinedTextField(value = editedDetails, onValueChange = { editedDetails = it }, placeholder = { Text("Add notes...", color = tertiaryColor) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor.copy(alpha = 0.5f), unfocusedBorderColor = Color.Transparent, focusedContainerColor = itemBgColor, unfocusedContainerColor = itemBgColor, cursorColor = primaryColor), shape = RoundedCornerShape(12.dp), minLines = 3)
            }
            if (!showDateTime) GreenAddOption("Set Date & Time", PhosphorIcons.Bold.CalendarPlus) { showDateTime = true } else ExpandedOption {
                SectionHeaderWithClose("WHEN", onClose = { showDateTime = false })
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactSelector(label = "Date", value = editedDate?.format(dateFormatter) ?: "Set Date", onClick = { showDatePicker = true }, modifier = Modifier.weight(1f))
                    CompactSelector(label = "Time", value = editedTime?.format(timeFormatter) ?: "Set Time", onClick = { showTimePicker = true }, modifier = Modifier.weight(1f))
                }
            }
            if (!showPriority) GreenAddOption("Set Priority", PhosphorIcons.Bold.Flag) { showPriority = true } else ExpandedOption {
                SectionHeaderWithClose("PRIORITY", onClose = { showPriority = false })
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { priority ->
                        val isSelected = editedPriority == priority
                        val priorityColor = Color(priority.color)
                        val targetColor = if (isSelected) priorityColor else itemBgColor
                        val textColor = if (isSelected) Color.Black else tertiaryColor
                        Box(modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp)).background(targetColor).clickable { editedPriority = priority }, contentAlignment = Alignment.Center) {
                            Text(text = priority.label, style = MaterialTheme.typography.labelMedium, color = textColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editedDate?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { editedDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("OK", color = primaryColor) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = tertiaryColor) } },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) { DatePicker(state = datePickerState) }
    }
    if (showTimePicker) {
        val initialTime = editedTime ?: LocalTime.now()
        val timeState = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute)
        TimePickerDialogShim(onDismiss = { showTimePicker = false }, onConfirm = { editedTime = LocalTime.of(timeState.hour, timeState.minute); showTimePicker = false }, state = timeState)
    }
}

// --- NEW BOTTOM NAVBAR ---
@Composable
fun BottomNavBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background

    Surface(
        color = bgColor,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Items
            NavBarItem(AppScreen.Calendar, currentScreen, onScreenSelected)
            NavBarItem(AppScreen.Tasks, currentScreen, onScreenSelected)

            // Center Add Button (With Rounded Square Stroke)
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(40.dp) // Overall size of the rounded square
                    .border(
                        width = 2.dp, // Thickness of the stroke
                        color = primaryColor, // Matches the icon color
                        shape = RoundedCornerShape(12.dp) // Rounded square shape
                    )
                    .clip(RoundedCornerShape(12.dp)) // Keeps the tap ripple inside the border
            ) {
                Icon(
                    imageVector = PhosphorIcons.Bold.Plus,
                    contentDescription = "Add",
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp) // Slightly smaller icon to fit inside the 40dp box
                )
            }

            // Right Items
            NavBarItem(AppScreen.Habits, currentScreen, onScreenSelected)
            NavBarItem(AppScreen.Notes, currentScreen, onScreenSelected)
        }
    }
}

// FIX: Removed "RowScope." since we no longer need the .weight() modifier
@Composable
fun NavBarItem(
    screen: AppScreen,
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {
    val isSelected = currentScreen == screen
    val color by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "color"
    )
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.15f else 1f, label = "scale")

    // Removed the wrapping Box and weight(1f)
    IconButton(
        onClick = { onScreenSelected(screen) },
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Icon(
            imageVector = when (screen) {
                AppScreen.Calendar -> PhosphorIcons.Bold.CalendarBlank
                AppScreen.Tasks -> PhosphorIcons.Bold.CheckCircle
                AppScreen.Habits -> PhosphorIcons.Bold.ArrowsClockwise
                AppScreen.Notes -> PhosphorIcons.Bold.Note
            },
            contentDescription = screen.name,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SectionHeaderWithClose(title: String, onClose: () -> Unit) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = tertiaryColor, letterSpacing = 1.sp)
        Icon(imageVector = PhosphorIcons.Bold.Minus, contentDescription = "Remove", tint = tertiaryColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp).clickable { onClose() })
    }
}