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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex

// --- PHOSPHOR ICONS IMPORTS (BOLD) ---
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.*
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
    var newTaskDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var newTaskTime by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }

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

    // --- SAFE LIST HANDLING ---
    val activeTasks = remember(taskList) {
        taskList?.filter { !it.isCompleted } ?: emptyList()
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
                            if (activeTasks.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxSize()
                                            .padding(bottom = 100.dp),
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
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 20.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowElevation = 0.dp
                                    ) {
                                        Column {
                                            activeTasks.forEachIndexed { index, task ->
                                                TaskItem(
                                                    task = task,
                                                    onCheckedChange = { isChecked ->
                                                        viewModel.toggleTask(
                                                            task,
                                                            isChecked
                                                        )
                                                    },
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
                                            .clickable {
                                                isCompletedExpanded = !isCompletedExpanded
                                            }
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
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(horizontal = 20.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        ) {
                                            Column {
                                                completedTasks.forEachIndexed { index, task ->
                                                    TaskItem(
                                                        task = task,
                                                        onCheckedChange = { isChecked ->
                                                            viewModel.toggleTask(
                                                                task,
                                                                isChecked
                                                            )
                                                        },
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
        KomoriDialog(
            title = "New Focus",
            onDismiss = { showDialog = false },
            onDelete = null, // Passing null automatically shows the "X" close button
            saveButtonText = "Add Focus",
            onSave = {
                if (newTaskTitle.isNotBlank()) {
                    viewModel.addTask(newTaskTitle, newTaskDetails, newTaskDate, newTaskTime)
                    showDialog = false
                }
            }
        ) {
            // --- DIALOG CONTENT ---

            // 1. Title Input
            CalmInputTextField(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                label = "What's the core task?",
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Details Input
            CalmInputTextField(
                value = newTaskDetails,
                onValueChange = { newTaskDetails = it },
                label = "Any details?",
                singleLine = false,
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Date & Time Selectors (Using CompactSelector)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date
                CompactSelector(
                    label = "Date",
                    value = if (newTaskDate != null) formatDate(newTaskDate) else "Set Date",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                )

                // Time
                CompactSelector(
                    label = "Time",
                    value = newTaskTime ?: "Set Time",
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // No need to add Buttons here, KomoriDialog adds them automatically at the bottom!
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
    // 1. STATE
    var editedTitle by remember { mutableStateOf(task.title) }
    var editedDetails by remember { mutableStateOf(task.details) }
    var editedDate by remember { mutableStateOf(task.dueDate) }
    var editedTime by remember { mutableStateOf(task.dueTime) }
    var isCompleted by remember { mutableStateOf(task.isCompleted) }

    // 2. COLORS & DEFINITIONS
    val isAppInDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val containerColor = if (isAppInDarkMode) Color(0xFF333333) else MistGreen.copy(alpha = 0.3f)
    val borderColor = if (isAppInDarkMode) SageGreen.copy(alpha = 0.5f) else MistGreen

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = if (editedDate != null) dateFormat.format(Date(editedDate!!)) else "Set Date"
    val timeString = editedTime ?: "Set Time"

    // Pickers visibility
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 3. MAIN DIALOG WRAPPER
    KomoriDialog(
        title = "Edit Focus",
        onDismiss = onDismiss,
        onDelete = onDelete, // Shows Trash icon in header
        saveButtonText = "Save",
        onSave = {
            val updatedTask = task.copy(
                title = editedTitle,
                details = editedDetails,
                dueDate = editedDate,
                dueTime = editedTime,
                isCompleted = isCompleted
            )
            onSave(updatedTask)
        }
    ) {
        // --- DIALOG CONTENT ---

        // 1. Inputs
        CalmInputTextField(value = editedTitle, onValueChange = { editedTitle = it }, label = "Task Name", singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))

        CalmInputTextField(value = editedDetails, onValueChange = { editedDetails = it }, label = "Details", singleLine = false, minLines = 3, maxLines = 5)
        Spacer(modifier = Modifier.height(24.dp))

        // 2. Date & Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactSelector(
                label = "Due Date",
                value = dateString,
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            )
            CompactSelector(
                label = "Time",
                value = timeString,
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Status Toggle
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.labelSmall,
                color = StoneGray,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
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
                // In Progress Segment
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

                // Completed Segment
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
                            Icon(
                                imageVector = PhosphorIcons.Bold.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
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
    }

    // --- REUSED DATE/TIME PICKERS (For Edit Dialog) ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { editedDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK", color = SageGreen) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = StoneGray) } }
        ) { DatePicker(state = datePickerState) }
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
            color = MaterialTheme.colorScheme.background,
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
                        clockDialColor = MaterialTheme.colorScheme.surface,
                        selectorColor = SageGreen,
                        periodSelectorSelectedContainerColor = SageGreen.copy(alpha = 0.2f),
                        periodSelectorSelectedContentColor = SageGreen,
                        timeSelectorSelectedContainerColor = SageGreen.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = SageGreen
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