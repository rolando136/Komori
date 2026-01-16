package com.rolando.komori.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import blurBackground
import com.rolando.komori.data.Task
import com.rolando.komori.ui.TaskViewModel
import com.rolando.komori.ui.components.TaskItem
import com.rolando.komori.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.rolando.komori.data.WeeklyEvent
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.random.Random
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.DatePickerDefaults.dateFormatter
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import androidx.room.Entity // <
import androidx.room.PrimaryKey

// --- PHOSPHOR ICONS IMPORTS (BOLD) ---
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.*
import com.rolando.komori.data.Subtask
import com.rolando.komori.data.TaskPriority
import com.rolando.komori.ui.components.CompactSelector
import com.rolando.komori.ui.components.KomoriButton
import com.rolando.komori.ui.components.KomoriDialog
import com.rolando.komori.ui.components.MonthYearPickerDialog

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
    // Change these definitions:
    var newTaskDate by remember { mutableStateOf<LocalDate?>(null) } // Was Long?
    var newTaskTime by remember { mutableStateOf<LocalTime?>(null) } // Was String?
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }



    // FIX: Ensure types are clear here
    var newTaskPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var newSubtaskText by remember { mutableStateOf("") }

    // FIX: You must import 'mutableStateListOf' and 'Subtask'
    val newTaskSubtasks = remember { mutableStateListOf<Subtask>() }

    val isDark = isSystemInDarkTheme()
    val buttonBgColor = if (isDark) Color(0xFF333333) else MistGreen
    val dialogSurfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val dialogTextColor = MaterialTheme.colorScheme.onSurface

    // --- NAVIGATION STATE ---
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 1) { 4 }
    val currentScreen = AppScreen.entries[pagerState.currentPage]

    // --- DOCK STATE ---
    var isDockHidden by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) { isDockHidden = false }

    // --- SCHEDULE STATE ---
    // 1. Observe the live data from the ViewModel (Room Database)
    val scheduleEvents by viewModel.scheduleEvents.collectAsState()

    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedScheduleEvent by remember { mutableStateOf<WeeklyEvent?>(null) }
    var isEditingSchedule by remember { mutableStateOf(false) }

    // --- SAFE LIST HANDLING & SORTING ---
    val allActiveTasks = remember(taskList) {
        taskList?.filter { !it.isCompleted }
            ?.sortedWith(
                compareBy<Task, LocalDate?>(nullsLast()) { it.date }
                    .thenBy(nullsLast()) { it.time }
            ) ?: emptyList()
    }

    // 1. High Priority (Top Section)
    val highPriorityTasks = remember(allActiveTasks) {
        allActiveTasks.filter { it.priority == TaskPriority.HIGH }
    }

    // 2. Standard Tasks (Low/Medium)
    val standardTasks = remember(allActiveTasks) {
        allActiveTasks.filter { it.priority != TaskPriority.HIGH }
    }

    val completedTasks = remember(taskList) {
        taskList?.filter { it.isCompleted } ?: emptyList()
    }

    var showMonthPicker by remember { mutableStateOf(false) }
    // --- HEADER STATE ---
    // Default to "January 2026" (or current)
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    var headerTitle by remember { mutableStateOf(LocalDate.now().format(monthFormatter)) }

    // State for the jump command
    var targetJumpDate by remember { mutableStateOf<LocalDate?>(null) }

    // --- HEADER DATE ---
    val currentMonthYear = remember {
        val now = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        now.format(formatter)
    }

    val isAnyDialogOpen = showDialog || showScheduleDialog || selectedTask != null || selectedScheduleEvent != null || showMonthPicker


    val blurRadius by animateDpAsState(
        targetValue = if (isAnyDialogOpen) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "BlurAnimation"
    )

    // --- ADD THESE TWO LINES HERE ---
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return "Pick a Date"
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            // Apply the custom blur modifier here
            modifier = Modifier.blur(blurRadius),
            containerColor = MaterialTheme.colorScheme.background,

            // --- DYNAMIC HEADER ---
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        // LOGIC: If Calendar, show Clickable Row. Else, show simple Text.
                        if (currentScreen == AppScreen.Calendar) {
                            // 1. CLICKABLE CALENDAR HEADER
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showMonthPicker = true } // <--- MAKES IT CLICKABLE
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = headerTitle, // <--- USE DYNAMIC VARIABLE (Not currentMonthYear)
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = AppFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = PhosphorIcons.Bold.CaretDown,
                                    contentDescription = "Select Month",
                                    tint = SageGreen, // Use your theme color
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // 2. STANDARD HEADER (Tasks, Habits, Notes)
                            Text(
                                text = when (currentScreen) {
                                    AppScreen.Tasks -> "My Focus"
                                    AppScreen.Habits -> "Rituals"
                                    AppScreen.Notes -> "Notes"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = AppFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(
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

            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp

                // 1. DYNAMIC SIZES
                val fabSize = when {
                    screenWidth < 380 -> 56.dp
                    screenWidth > 550 -> 72.dp
                    else -> 64.dp
                }

                // --- A. CALCULATE WIDTHS ---
                val dockHeight = fabSize
                val iconSize = dockHeight * 0.45f
                val buttonWidth = iconSize + 24.dp
                val spacing = 4.dp
                val itemCount = 4

                val verticalSpace = (dockHeight - iconSize) / 2
                val sidePadding = verticalSpace * 2
                val internalButtonSpace = (buttonWidth - iconSize) / 2
                val rowPadding = (sidePadding - internalButtonSpace).coerceAtLeast(0.dp)

                // 1. Ideal Width (Full size needed for all icons)
                val idealDockWidth = (buttonWidth * itemCount) + (spacing * (itemCount - 1)) + (rowPadding * 2)

                // 2. Constraints
                val screenEdgePadding = 16.dp
                val gapSize = 8.dp

                // 3. Calculate "Clamped" Width (When FAB is Visible)
                // We sacrifice width to make room for the FAB
                val maxWithFab = screenWidth.dp - (screenEdgePadding * 2) - fabSize - gapSize
                val clampedDockWidth = minOf(idealDockWidth, maxWithFab)

                // 4. Calculate "Expanded" Width (When FAB is Hidden)
                // We use all available screen space (minus edges)
                val maxWithoutFab = screenWidth.dp - (screenEdgePadding * 2)
                val expandedDockWidth = minOf(idealDockWidth, maxWithoutFab)

                // 2. ANIMATION STATE
                val isFabVisible = currentScreen == AppScreen.Tasks || currentScreen == AppScreen.Calendar
                val transition = updateTransition(targetState = isFabVisible, label = "FabTransition")

                // --- B. ANIMATIONS ---

                // 1. Dock Width Animation (NEW)
                // Expands when FAB hides, Shrinks when FAB appears
                val animatedDockWidth by transition.animateDp(
                    transitionSpec = {
                        if (targetState) {
                            // Shrink quickly so we don't hit the incoming FAB
                            tween(500, easing = FastOutSlowInEasing)
                        } else {
                            // Expand slightly delayed (wait for FAB to drop)
                            tween(500, delayMillis = 100, easing = FastOutSlowInEasing)
                        }
                    },
                    label = "DockWidth"
                ) { visible ->
                    if (visible) clampedDockWidth else expandedDockWidth
                }

                // 2. Dock Slide (Moves Left)
                // Note: The shift amount is based on what we are making room FOR (the FAB),
                // so the logic remains the same regardless of dock width.
                val dockSlideX by transition.animateDp(
                    transitionSpec = { tween(500, easing = FastOutSlowInEasing) },
                    label = "DockSlide"
                ) { visible ->
                    if (visible) -((gapSize + fabSize) / 2) else 0.dp
                }

                // 3. FAB Vertical Drop
                val fabOffsetY by transition.animateDp(
                    transitionSpec = {
                        if (targetState) tween(500, delayMillis = 150, easing = FastOutSlowInEasing)
                        else tween(500, easing = FastOutSlowInEasing)
                    },
                    label = "FabOffsetY"
                ) { visible -> if (visible) 0.dp else fabSize * 2 }

                // 4. Alpha
                val fabAlpha by transition.animateFloat(
                    transitionSpec = {
                        if (targetState) tween(300, delayMillis = 200)
                        else tween(500, easing = LinearEasing)
                    },
                    label = "FabAlpha"
                ) { visible -> if (visible) 1f else 0f }

                val dockOffset by animateDpAsState(if (isDockHidden) 150.dp else 0.dp, tween(500))
                val fabRotation by animateFloatAsState(if (currentScreen == AppScreen.Calendar) 90f else 0f, tween(500))
                val fabIconSize = fabSize * 0.5f

                // 3. LAYOUT
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .offset(y = dockOffset),
                    contentAlignment = Alignment.BottomCenter
                ) {

                    // --- DOCK ---
                    Box(
                        modifier = Modifier
                            .width(animatedDockWidth) // <--- Use the Animated Width
                            .height(fabSize)
                            .offset(x = dockSlideX),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingNavBar(
                            currentScreen = currentScreen,
                            onScreenSelected = { screen ->
                                scope.launch { pagerState.animateScrollToPage(screen.ordinal) }
                            },
                            height = fabSize,
                            // Pass the animated width so the surface clips/scrolls correctly
                            modifier = Modifier.width(animatedDockWidth)
                        )
                    }

                    // --- FAB ---
                    if (fabAlpha > 0f) {
                        // Position based on the CURRENT animated width.
                        // This keeps the FAB glued to the dock edge even while the dock is shrinking/growing.
                        val fabFixedOffsetX = (animatedDockWidth / 2) + (gapSize / 2)

                        FloatingActionButton(
                            onClick = {
                                if (currentScreen == AppScreen.Tasks) {
                                    newTaskTitle = ""; newTaskDetails = ""; newTaskTime = null; newTaskDate = null
                                    showDialog = true
                                } else if (currentScreen == AppScreen.Calendar) {
                                    showScheduleDialog = true
                                }
                            },
                            containerColor = SageGreen,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(18.dp),
                            elevation = FloatingActionButtonDefaults.elevation(4.dp),
                            modifier = Modifier
                                .size(fabSize)
                                .offset(x = fabFixedOffsetX)
                                .graphicsLayer {
                                    translationY = fabOffsetY.toPx()
                                    rotationZ = fabRotation
                                    alpha = fabAlpha
                                }
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.Plus,
                                contentDescription = "Add",
                                modifier = Modifier.size(fabIconSize)
                            )
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
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            ScheduleScreen(
                                events = scheduleEvents,
                                jumpToDate = targetJumpDate,
                                onVisibleDateChanged = { date ->
                                    headerTitle = date.format(monthFormatter)
                                    if (targetJumpDate == date) targetJumpDate = null
                                },
                                onEventClick = { event -> selectedScheduleEvent = event },

                                // FIX: Use 'pagerState.currentPage' to get the real-time active page
                                onBottomReached = { isBottom ->
                                    if (pagerState.currentPage == AppScreen.Calendar.ordinal) {
                                        isDockHidden = isBottom
                                    }
                                }
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Calendar requires Android O or newer", color = StoneGray)
                            }
                        }
                    }

                    AppScreen.Tasks -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
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
                                            color = StoneGray,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                // --- GLOBAL HEADER (Only if active tasks exist) ---
                                if (allActiveTasks.isNotEmpty()) {
                                    item {
                                        SectionHeader("ACTIVE FOCUS")
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                // --- SECTION 1: HIGH PRIORITY ---
                                if (highPriorityTasks.isNotEmpty()) {
                                    item {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, Color(TaskPriority.HIGH.color).copy(alpha = 0.3f)),
                                            shadowElevation = 0.dp
                                        ) {
                                            Column {
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
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isCompletedExpanded = !isCompletedExpanded }
                                            .padding(horizontal = 24.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "COMPLETED (${completedTasks.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = StoneGray
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
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
    }

    if (selectedTask != null) {
        // STATE: Track if we are in "View" mode or "Edit" mode
        // We use a local state that resets whenever selectedTask changes
        var isEditing by remember(selectedTask) { mutableStateOf(false) }

        if (isEditing) {
            // 2. EDIT MODE
            EditTaskDialog(
                task = selectedTask!!,
                onDismiss = { isEditing = false }, // Go back to View mode
                onDelete = {
                    viewModel.deleteTask(selectedTask!!)
                    selectedTask = null
                },
                onSave = { updatedTask ->
                    viewModel.updateTask(updatedTask)
                    selectedTask = null // Close everything
                }
            )
        } else {
            // 1. VIEW MODE (Updated)
            TaskDetailDialog(
                task = selectedTask!!,
                onDismiss = { selectedTask = null },
                onEdit = { isEditing = true },
                onUpdateTask = { updatedTask ->
                    viewModel.updateTask(updatedTask)
                    selectedTask = updatedTask
                }
            )
        }
    }

    // --- MAIN ADD TASK DIALOG (Redesigned) ---
    if (showDialog) {
        KomoriDialog(
            title = "New Focus",
            onDismiss = { showDialog = false },
            onDelete = null,
            saveButtonText = "Add Focus",
            onSave = {
                if (newTaskTitle.isNotBlank()) {
                    viewModel.addTask(
                        title = newTaskTitle,
                        details = newTaskDetails,
                        date = newTaskDate,
                        time = newTaskTime,
                        priority = newTaskPriority,
                        subtasks = newTaskSubtasks.toList()
                    )
                    // Reset State
                    newTaskTitle = ""; newTaskDetails = ""; newTaskPriority = TaskPriority.MEDIUM
                    newTaskDate = null; newTaskTime = null; newTaskSubtasks.clear()
                    showDialog = false
                }
            }
        ) {
            // --- DIALOG CONTENT ---

            // 1. VISIBILITY STATES (Hidden by default)
            var showDetails by remember { mutableStateOf(false) }
            var showPriority by remember { mutableStateOf(false) }
            var showDateTime by remember { mutableStateOf(false) }
            var showSubtasks by remember { mutableStateOf(false) }

            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            val optionBgColor = if (isDark) Color(0xFF333333) else MistGreen.copy(alpha = 0.15f)
            val optionShape = RoundedCornerShape(12.dp)

            // --- A. MAIN INPUT (Always Visible) ---
            CalmInputTextField(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                label = "What is the core task?",
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- B. OPTIONAL SECTIONS ---

            // 1. DETAILS
            if (!showDetails) {
                AddOptionRow(
                    icon = PhosphorIcons.Bold.TextAlignLeft,
                    label = "Add Details",
                    onClick = { showDetails = true },
                    backgroundColor = optionBgColor,
                    shape = optionShape
                )
            } else {
                SectionHeaderWithClose(title = "DETAILS", onClose = { showDetails = false; newTaskDetails = "" })
                CalmInputTextField(
                    value = newTaskDetails,
                    onValueChange = { newTaskDetails = it },
                    label = "Add notes or context...",
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2. SUBTASKS
            if (!showSubtasks) {
                AddOptionRow(
                    icon = PhosphorIcons.Bold.ListPlus,
                    label = "Add Subtasks",
                    onClick = { showSubtasks = true },
                    backgroundColor = optionBgColor,
                    shape = optionShape
                )
            } else {
                SectionHeaderWithClose(title = "SUBTASKS", onClose = { showSubtasks = false; newTaskSubtasks.clear() })

                // Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalmInputTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        label = "Next step...",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                newTaskSubtasks.add(Subtask(title = newSubtaskText))
                                newSubtaskText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(SageGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(PhosphorIcons.Bold.Plus, "Add", tint = SageGreen)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List
                newTaskSubtasks.forEachIndexed { index, subtask ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(optionBgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(SageGreen, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(subtask.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = PhosphorIcons.Bold.X,
                            contentDescription = "Remove",
                            tint = StoneGray,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { newTaskSubtasks.removeAt(index) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 3. PRIORITY
            if (!showPriority) {
                AddOptionRow(
                    icon = PhosphorIcons.Bold.Flag,
                    label = "Set Priority",
                    onClick = { showPriority = true },
                    backgroundColor = optionBgColor,
                    shape = optionShape
                )
            } else {
                SectionHeaderWithClose(title = "PRIORITY", onClose = { showPriority = false; newTaskPriority = TaskPriority.MEDIUM })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { priority ->
                        val isSelected = newTaskPriority == priority
                        val priorityColor = Color(priority.color)
                        val targetColor = if (isSelected) priorityColor else Color.Transparent
                        val targetBorder = if (isSelected) priorityColor else Color.Gray.copy(alpha = 0.5f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(targetColor)
                                .border(1.dp, targetBorder, RoundedCornerShape(8.dp))
                                .clickable { newTaskPriority = priority },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = priority.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.Black else StoneGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. DATE & TIME
            if (!showDateTime) {
                AddOptionRow(
                    icon = PhosphorIcons.Bold.CalendarPlus,
                    label = "Set Date & Time",
                    onClick = { showDateTime = true },
                    backgroundColor = optionBgColor,
                    shape = optionShape
                )
            } else {
                SectionHeaderWithClose(title = "WHEN", onClose = { showDateTime = false; newTaskDate = null; newTaskTime = null })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactSelector(
                        label = "Date",
                        value = newTaskDate?.format(dateFormatter) ?: "Set Date",
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    CompactSelector(
                        label = "Time",
                        value = newTaskTime?.format(timeFormatter) ?: "Set Time",
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

        }
    }

    // --- 1. DATE PICKER (Fixed for LocalDate) ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), // safe fallback color
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(0.88f),
            confirmButton = {
                TextButton(onClick = {
                    // FIX: Convert Milliseconds (Long) -> LocalDate
                    datePickerState.selectedDateMillis?.let { millis ->
                        newTaskDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
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
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = StoneGray,
                    subheadContentColor = MaterialTheme.colorScheme.onSurface,
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    todayContentColor = SageGreen,
                    todayDateBorderColor = SageGreen,
                    selectedDayContainerColor = SageGreen,
                    selectedDayContentColor = Color.White,
                    navigationContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }

    // --- 2. TIME PICKER (Fixed for LocalTime) ---
    if (showTimePicker) {
        val timeState = rememberTimePickerState()

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
                            clockDialColor = MaterialTheme.colorScheme.surface,
                            selectorColor = SageGreen,
                            periodSelectorSelectedContainerColor = SageGreen.copy(alpha = 0.2f),
                            periodSelectorSelectedContentColor = SageGreen,
                            timeSelectorSelectedContainerColor = SageGreen.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = SageGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = StoneGray) }

                        TextButton(onClick = {
                            // FIX: Create LocalTime directly (No more Calendar/SimpleDateFormat)
                            newTaskTime = java.time.LocalTime.of(timeState.hour, timeState.minute)
                            showTimePicker = false
                        }) { Text("OK", color = SageGreen) }
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            initialDate = LocalDate.now(), // or parse current headerTitle if precise
            onDismiss = { showMonthPicker = false },
            onConfirm = { selectedDate ->
                targetJumpDate = selectedDate // Trigger the jump in ScheduleScreen
                showMonthPicker = false
            }
        )
    }
    // --- ADD SCHEDULE DIALOG ---
    if (showScheduleDialog) {
        val today = LocalDate.now()
        val defaultTime = LocalTime.of(8, 0)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AddScheduleDialog(
                initialDate = today,
                initialStartTime = defaultTime,
                onDismiss = { showScheduleDialog = false },
                // FIX: Add 'recurrence' to the lambda parameters
                onSave = { title, description, category, date, recurrence, start, end, colorInt ->
                    viewModel.addScheduleEvent(
                        title = title,
                        description = description,
                        category = category,
                        startDate = date,
                        recurrence = recurrence, // Pass it correctly
                        start = start,
                        end = end,
                        color = colorInt
                    )
                    showScheduleDialog = false
                }
            )
        }
    }
    // --- EDIT SCHEDULE DIALOG LOGIC ---
    // --- SCHEDULE DIALOGS LOGIC ---

    if (selectedScheduleEvent != null) {
        if (isEditingSchedule) {
            // 2. SHOW EDIT DIALOG (If user clicked "Edit")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                EditScheduleDialog(
                    event = selectedScheduleEvent!!,
                    onDismiss = {
                        // On Cancel, go back to details (optional) or close
                        isEditingSchedule = false
                    },
                    onDelete = {
                        viewModel.deleteScheduleEvent(selectedScheduleEvent!!)
                        selectedScheduleEvent = null
                        isEditingSchedule = false
                    },
                    onSave = { updatedEvent ->
                        viewModel.updateScheduleEvent(updatedEvent)
                        selectedScheduleEvent = null
                        isEditingSchedule = false
                    }
                )
            }
        } else {
            // 1. SHOW DETAIL DIALOG (First Screen)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ScheduleDetailDialog(
                    event = selectedScheduleEvent!!,
                    onDismiss = { selectedScheduleEvent = null },
                    onEdit = {
                        // Switch to Edit Mode
                        isEditingSchedule = true
                    }
                )
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
    maxLines: Int = 1,
    // 1. Parameter is correctly defined here:
    modifier: Modifier = Modifier
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
        // 2. FIX: Use the 'modifier' parameter passed to the function!
        // We chain .fillMaxWidth() to it so it defaults to full width
        // unless the parent overrides it (like with weight).
        modifier = modifier.fillMaxWidth()
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskDetailDialog(
    task: Task,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onUpdateTask: (Task) -> Unit
) {
    // 1. CALCULATE PROGRESS
    val totalSubtasks = task.subtasks.size
    val completedSubtasks = task.subtasks.count { it.isCompleted }
    val progress = if (totalSubtasks > 0) {
        completedSubtasks.toFloat() / totalSubtasks
    } else {
        if (task.isCompleted) 1f else 0f
    }
    val percentage = (progress * 100).toInt()

    // 2. COLORS & FORMATTERS
    val priorityColor = Color(task.priority.color)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM dd") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val shape = RoundedCornerShape(28.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = shape,
            color = containerColor,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                // =========================================================
                // 1. HEADER ROW: Title & Progress
                // =========================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFont,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        lineHeight = 28.sp
                    )

                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(44.dp),
                            color = SageGreen,
                            trackColor = StoneGray.copy(alpha = 0.15f),
                            strokeWidth = 4.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // =========================================================
                // 2. SUBTASKS (Directly under Title)
                // =========================================================
                if (task.subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        task.subtasks.forEachIndexed { index, subtask ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val newSubtasks = task.subtasks.toMutableList()
                                        newSubtasks[index] = subtask.copy(isCompleted = !subtask.isCompleted)
                                        onUpdateTask(task.copy(subtasks = newSubtasks))
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (subtask.isCompleted) PhosphorIcons.Bold.CheckCircle else PhosphorIcons.Bold.Circle,
                                    contentDescription = null,
                                    tint = if (subtask.isCompleted) SageGreen else StoneGray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = subtask.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                    color = if (subtask.isCompleted) StoneGray else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null
                                )
                            }
                        }
                    }
                }

                // =========================================================
                // 3. DIVIDER
                // =========================================================
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = StoneGray.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(20.dp))

                // =========================================================
                // 4. METADATA (Priority, Date, Details)
                // =========================================================
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // A. Priority (First item in metadata)
                    DetailRow(
                        icon = PhosphorIcons.Bold.Flag,
                        label = "Priority",
                        value = task.priority.label,
                        iconColor = priorityColor
                    )

                    // B. Date & Time
                    if (task.date != null) {
                        val dateStr = task.date.format(dateFormatter)
                        val timeStr = task.time?.format(timeFormatter)
                        val fullDateTime = if (timeStr != null) "$dateStr • $timeStr" else dateStr

                        DetailRow(
                            icon = PhosphorIcons.Bold.CalendarBlank,
                            label = "Due Date",
                            value = fullDateTime,
                            iconColor = SageGreen
                        )
                    }

                    // C. Details
                    if (task.details.isNotBlank()) {
                        DetailRow(
                            icon = PhosphorIcons.Bold.TextAlignLeft,
                            label = "Description",
                            value = task.details,
                            iconColor = SageGreen,
                            alignTop = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // =========================================================
                // 5. ACTION BUTTONS
                // =========================================================

                // Save/Close Button
                KomoriButton(
                    text = "Save",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Edit Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SageGreen.copy(alpha = 0.15f))
                        .border(1.dp, SageGreen, RoundedCornerShape(14.dp))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.PencilSimple,
                        contentDescription = "Edit",
                        tint = SageGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// --- HELPER COMPOSABLE (Paste this at the bottom of TasksScreen.kt) ---


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Task) -> Unit
) {
    // 1. DATA STATE
    var editedTitle by remember { mutableStateOf(task.title) }
    var editedDetails by remember { mutableStateOf(task.details) }
    var editedDate by remember { mutableStateOf(task.date) }
    var editedTime by remember { mutableStateOf(task.time) }
    var isCompleted by remember { mutableStateOf(task.isCompleted) }
    var editedPriority by remember { mutableStateOf(task.priority) }
    val editedSubtasks = remember { mutableStateListOf<Subtask>().apply { addAll(task.subtasks) } }
    var newSubtaskText by remember { mutableStateOf("") }

    // 2. VISIBILITY STATE
    var showDetails by remember { mutableStateOf(task.details.isNotBlank()) }
    var showPriority by remember { mutableStateOf(task.priority != TaskPriority.MEDIUM) }
    var showDateTime by remember { mutableStateOf(task.date != null || task.time != null) }
    var showSubtasks by remember { mutableStateOf(task.subtasks.isNotEmpty()) }

    // 3. STYLING
    val isAppInDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val containerColor = if (isAppInDarkMode) Color(0xFF333333) else MistGreen.copy(alpha = 0.3f)
    val borderColor = if (isAppInDarkMode) SageGreen.copy(alpha = 0.5f) else MistGreen
    val optionBgColor = if (isAppInDarkMode) Color(0xFF333333) else MistGreen.copy(alpha = 0.15f)
    val optionShape = RoundedCornerShape(12.dp)

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    KomoriDialog(
        title = "Edit Focus",
        onDismiss = onDismiss,
        onDelete = onDelete,
        saveButtonText = "Save",
        onSave = {
            val updatedTask = task.copy(
                title = editedTitle,
                details = editedDetails,
                date = editedDate,
                time = editedTime,
                isCompleted = isCompleted,
                priority = editedPriority,
                subtasks = editedSubtasks.toList()
            )
            onSave(updatedTask)
        }
    ) {
        // --- CONTENT ---

        // 1. TITLE
        CalmInputTextField(
            value = editedTitle,
            onValueChange = { editedTitle = it },
            label = "Task Name",
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. STATUS (Moved to Top)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "STATUS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = StoneGray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(containerColor)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // In Progress
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isCompleted) SageGreen else Color.Transparent)
                        .clickable { isCompleted = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "In Progress",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontFamily = AppFont),
                        color = if (!isCompleted) Color.White else StoneGray
                    )
                }

                // Completed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCompleted) SageGreen else Color.Transparent)
                        .clickable { isCompleted = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCompleted) {
                            Icon(PhosphorIcons.Bold.Check, null, tint = Color.White, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                        }
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontFamily = AppFont),
                            color = if (isCompleted) Color.White else StoneGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. DETAILS SECTION
        if (!showDetails) {
            AddOptionRow(
                icon = PhosphorIcons.Bold.TextAlignLeft,
                label = "Add Details",
                onClick = { showDetails = true },
                backgroundColor = optionBgColor,
                shape = optionShape
            )
        } else {
            SectionHeaderWithClose(title = "DETAILS", onClose = { showDetails = false; editedDetails = "" })
            CalmInputTextField(
                value = editedDetails,
                onValueChange = { editedDetails = it },
                label = "Details",
                singleLine = false,
                minLines = 3,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4. SUBTASKS SECTION (Moved Below Details)
        if (!showSubtasks) {
            AddOptionRow(
                icon = PhosphorIcons.Bold.ListPlus,
                label = "Add Subtasks",
                onClick = { showSubtasks = true },
                backgroundColor = optionBgColor,
                shape = optionShape
            )
        } else {
            SectionHeaderWithClose(title = "SUBTASKS", onClose = { showSubtasks = false; editedSubtasks.clear() })

            // Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalmInputTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    label = "Next step...",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            editedSubtasks.add(Subtask(title = newSubtaskText))
                            newSubtaskText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(SageGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Icon(PhosphorIcons.Bold.Plus, "Add", tint = SageGreen)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List
            editedSubtasks.forEachIndexed { index, subtask ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(optionBgColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(1.dp, if (subtask.isCompleted) SageGreen else StoneGray, CircleShape)
                            .background(if (subtask.isCompleted) SageGreen else Color.Transparent, CircleShape)
                            .clickable {
                                editedSubtasks[index] = subtask.copy(isCompleted = !subtask.isCompleted)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (subtask.isCompleted) {
                            Icon(PhosphorIcons.Bold.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = subtask.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = PhosphorIcons.Bold.X,
                        contentDescription = "Remove",
                        tint = StoneGray,
                        modifier = Modifier.size(16.dp).clickable { editedSubtasks.removeAt(index) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 5. PRIORITY SECTION
        if (!showPriority) {
            AddOptionRow(
                icon = PhosphorIcons.Bold.Flag,
                label = "Set Priority",
                onClick = { showPriority = true },
                backgroundColor = optionBgColor,
                shape = optionShape
            )
        } else {
            SectionHeaderWithClose(title = "PRIORITY", onClose = { showPriority = false; editedPriority = TaskPriority.MEDIUM })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.entries.forEach { priority ->
                    val isSelected = editedPriority == priority
                    val priorityColor = Color(priority.color)
                    val targetColor = if (isSelected) priorityColor else Color.Transparent
                    val targetBorder = if (isSelected) priorityColor else Color.Gray.copy(alpha = 0.5f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(targetColor)
                            .border(1.dp, targetBorder, RoundedCornerShape(8.dp))
                            .clickable { editedPriority = priority },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = priority.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.Black else StoneGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 6. DATE & TIME SECTION
        if (!showDateTime) {
            AddOptionRow(
                icon = PhosphorIcons.Bold.CalendarPlus,
                label = "Set Date & Time",
                onClick = { showDateTime = true },
                backgroundColor = optionBgColor,
                shape = optionShape
            )
        } else {
            SectionHeaderWithClose(title = "WHEN", onClose = { showDateTime = false; editedDate = null; editedTime = null })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactSelector(
                    label = "Due Date",
                    value = editedDate?.format(dateFormatter) ?: "Set Date",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
                CompactSelector(
                    label = "Time",
                    value = editedTime?.format(timeFormatter) ?: "Set Time",
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // --- DIALOGS ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editedDate?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { editedDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("OK", color = SageGreen) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = StoneGray) } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = editedTime?.hour ?: LocalTime.now().hour, initialMinute = editedTime?.minute ?: LocalTime.now().minute)
        Dialog(onDismissRequest = { showTimePicker = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth(0.94f).padding(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(24.dp))
                    TimePicker(state = timeState, colors = TimePickerDefaults.colors(clockDialColor = MaterialTheme.colorScheme.surface, selectorColor = SageGreen, periodSelectorSelectedContainerColor = SageGreen.copy(alpha = 0.2f), periodSelectorSelectedContentColor = SageGreen, timeSelectorSelectedContainerColor = SageGreen.copy(alpha = 0.2f), timeSelectorSelectedContentColor = SageGreen))
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = StoneGray) }
                        TextButton(onClick = { editedTime = LocalTime.of(timeState.hour, timeState.minute); showTimePicker = false }) { Text("OK", color = SageGreen) }
                    }
                }
            }
        }
    }
}

// ... imports ...

@Composable
fun FloatingNavBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    height: Dp = 72.dp,
    modifier: Modifier = Modifier // <--- NEW PARAMETER
) {
    val iconSize = height * 0.45f
    val verticalSpace = (height - iconSize) / 2
    val sidePadding = verticalSpace * 2
    val buttonWidth = iconSize + 24.dp
    val internalButtonSpace = (buttonWidth - iconSize) / 2
    val rowPadding = (sidePadding - internalButtonSpace).coerceAtLeast(0.dp)
    val cornerRadius = height * 0.35f

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(cornerRadius),
        // FIX: Apply the passed 'modifier' here, then chain your height
        modifier = modifier.height(height),
        shadowElevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = rowPadding)
                .horizontalScroll(rememberScrollState()), // Scroll logic takes over if width is clamped
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppScreen.entries.forEach { screen ->
                val isSelected = currentScreen == screen

                val targetAlpha = if (isSelected) 1f else 0.4f
                val targetScale = if (isSelected) 1.1f else 1f
                val targetColor = if (isSelected) SageGreen else MaterialTheme.colorScheme.onSurface

                val animatedAlpha by animateFloatAsState(targetValue = targetAlpha, label = "alpha")
                val animatedScale by animateFloatAsState(targetValue = targetScale, label = "scale")
                val animatedColor by animateColorAsState(targetValue = targetColor, label = "color")

                IconButton(
                    onClick = { onScreenSelected(screen) },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(buttonWidth)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            alpha = animatedAlpha
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
                        tint = animatedColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
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

@Composable
fun AddOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    shape: androidx.compose.ui.graphics.Shape
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(shape)
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SageGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = StoneGray)
        Spacer(modifier = Modifier.weight(1f))
        Icon(imageVector = PhosphorIcons.Bold.Plus, contentDescription = null, tint = StoneGray.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
    }
}

@Composable
fun SectionHeaderWithClose(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = StoneGray,
            letterSpacing = 1.sp
        )
        Icon(
            imageVector = PhosphorIcons.Bold.Minus,
            contentDescription = "Remove",
            tint = StoneGray.copy(alpha = 0.7f),
            modifier = Modifier
                .size(16.dp)
                .clickable { onClose() }
        )
    }
}
