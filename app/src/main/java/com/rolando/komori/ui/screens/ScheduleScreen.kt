package com.rolando.komori.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rolando.komori.data.WeeklyEvent
import com.rolando.komori.ui.theme.AppFont
import com.rolando.komori.ui.theme.SageGreen
import com.rolando.komori.ui.theme.StoneGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.luminance
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.Trash
import androidx.compose.ui.graphics.toArgb
import com.rolando.komori.ui.theme.MistGreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.rolando.komori.ui.components.CompactSelector
import com.rolando.komori.ui.components.KomoriButton
import com.rolando.komori.ui.components.KomoriDialog
import androidx.compose.ui.graphics.toArgb
import com.adamglin.phosphoricons.bold.ArrowsClockwise
import com.adamglin.phosphoricons.bold.CalendarBlank
import com.adamglin.phosphoricons.bold.CaretDown
import com.adamglin.phosphoricons.bold.Clock
import com.adamglin.phosphoricons.bold.PencilSimple
import com.adamglin.phosphoricons.bold.Tag
import com.adamglin.phosphoricons.bold.TextAlignLeft
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleScreen(
    events: List<WeeklyEvent>,
    jumpToDate: LocalDate?, // <--- NEW: Triggers jump
    onVisibleDateChanged: (LocalDate) -> Unit, // <--- NEW: Updates Parent Header
    onBottomReached: (Boolean) -> Unit = {},
    onEventClick: (WeeklyEvent) -> Unit
) {
    val density = LocalDensity.current
    val hourHeight = 64.dp
    val startHour = 0
    val endHour = 23
    val totalHours = endHour - startHour + 1
    val gridHeight = hourHeight * totalHours

    // --- STATE ---
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    val todayDate = remember { LocalDate.now() }

    // Pager Config (Start at middle for infinite scroll)
    val initialPage = 500
    val pagerState = rememberPagerState(initialPage = initialPage) { 1000 }
    val coroutineScope = rememberCoroutineScope()

    // Keep time updated
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(60000)
        }
    }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("h a") }
    val scrollState = rememberScrollState()

    // Auto-scroll to current time (run once)
    var hasScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasScrolled) {
            val now = LocalTime.now()
            val scrollOffsetDp = (now.hour * 64) - 128
            val scrollOffsetPx = with(density) { scrollOffsetDp.dp.toPx() }
            scrollState.animateScrollTo(scrollOffsetPx.toInt().coerceAtLeast(0))
            hasScrolled = true
        }
    }

    // --- 1. REPORT VISIBLE DATE ---
    // Whenever the page changes, calculate the date and tell the parent
    LaunchedEffect(pagerState.currentPage) {
        val weeksOffset = pagerState.currentPage - initialPage
        val today = LocalDate.now()
        // Calculate Monday of the current page
        val currentViewMonday = today
            .minusDays((today.dayOfWeek.value - 1).toLong())
            .plusWeeks(weeksOffset.toLong())

        onVisibleDateChanged(currentViewMonday)
    }

    // --- 2. HANDLE JUMP COMMAND ---
    // If parent passes a new date, scroll to it
    LaunchedEffect(jumpToDate) {
        jumpToDate?.let { target ->
            val today = LocalDate.now()
            val weeksDiff = ChronoUnit.WEEKS.between(today, target)
            pagerState.scrollToPage(initialPage + weeksDiff.toInt())
        }
    }

    // Scroll Detection
    LaunchedEffect(scrollState) {
        androidx.compose.runtime.snapshotFlow {
            val buffer = 100
            scrollState.maxValue > 0 && scrollState.value >= (scrollState.maxValue - buffer)
        }
            .distinctUntilChanged()
            .collect { onBottomReached(it) }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // [REMOVED HEADER ROW HERE]
        // The Header is gone, but the Pager below still works for swiping weeks.

        // --- HORIZONTAL PAGER (SWIPEABLE WEEKS) ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) { page ->

            val weeksOffset = page - initialPage
            val currentDayOfWeekVal = todayDate.dayOfWeek.value
            val baseMonday = todayDate.minusDays((currentDayOfWeekVal - 1).toLong())
            val pageMonday = baseMonday.plusWeeks(weeksOffset.toLong())
            val weekDays = (0..6).map { pageMonday.plusDays(it.toLong()) }

            Column(modifier = Modifier.fillMaxSize()) {

                // --- DAYS HEADER (Mon, Tue, etc.) ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 48.dp, bottom = 12.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEach { date ->
                        val isToday = date == LocalDate.now()
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase().take(1),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = AppFont, fontWeight = FontWeight.Normal),
                                color = if (isToday) SageGreen else StoneGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(if (isToday) SageGreen else Color.Transparent)
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontFamily = AppFont, fontWeight = FontWeight.Normal),
                                    color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // --- GRID ---
                Box(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                    Row(modifier = Modifier.fillMaxWidth().height(gridHeight)) {

                        // TIME SIDEBAR
                        Column(modifier = Modifier.width(48.dp)) {
                            for (hour in startHour..endHour) {
                                Box(modifier = Modifier.height(hourHeight), contentAlignment = Alignment.TopCenter) {
                                    Text(
                                        text = LocalTime.of(hour, 0).format(timeFormatter),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal, fontFamily = AppFont),
                                        color = StoneGray,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        // EVENT COLUMNS
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Column {
                                for (hour in startHour..endHour) {
                                    Box(modifier = Modifier.fillMaxWidth().height(hourHeight).border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                                }
                            }

                            Row(modifier = Modifier.fillMaxSize()) {
                                weekDays.forEach { date ->
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))) {

                                        // RECURRENCE FILTERING
                                        val dayEvents = events.filter { event ->
                                            if (date.isBefore(event.startDate)) return@filter false
                                            when (event.recurrence) {
                                                "Once" -> date.isEqual(event.startDate)
                                                "Daily" -> true
                                                "Weekly" -> date.dayOfWeek == event.startDate.dayOfWeek
                                                "Monthly" -> date.dayOfMonth == event.startDate.dayOfMonth
                                                "Yearly" -> date.dayOfYear == event.startDate.dayOfYear
                                                else -> false
                                            }
                                        }

                                        dayEvents.forEach { event ->
                                            val eventStart = event.startTime.hour + (event.startTime.minute / 60f)
                                            val eventEnd = event.endTime.hour + (event.endTime.minute / 60f)

                                            if (eventStart >= startHour) {
                                                val offset = hourHeight * (eventStart - startHour)
                                                val height = hourHeight * (eventEnd - eventStart)

                                                EventCard(
                                                    event = event,
                                                    modifier = Modifier.padding(horizontal = 2.dp).offset(y = offset).height(height).fillMaxWidth().clickable { onEventClick(event) }
                                                )
                                            }
                                        }

                                        // Realtime Indicator
                                        if (date == LocalDate.now()) {
                                            val now = currentTime
                                            val currentHourVal = now.hour + (now.minute / 60f)
                                            val lineOffset = hourHeight * (currentHourVal - startHour)
                                            Canvas(modifier = Modifier.fillMaxWidth().height(2.dp).offset(y = lineOffset)) {
                                                drawLine(color = SageGreen, start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 4f)
                                                drawCircle(color = SageGreen, radius = 6f, center = Offset(0f, 0f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



// 1. UPDATED ADD DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddScheduleDialog(
    initialDate: LocalDate,
    initialStartTime: LocalTime,
    onDismiss: () -> Unit,
    onSave: (String, String, String, LocalDate, String, LocalTime, LocalTime, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Default States
    var selectedColor by remember { mutableStateOf(SageGreen) }
    var selectedCategory by remember { mutableStateOf("Custom") }

    var selectedDate by remember { mutableStateOf(initialDate) }
    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialStartTime.plusHours(1)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM dd") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    var selectedRecurrence by remember { mutableStateOf("Once") }

    // Constants
    val categories = listOf("Work", "Study", "Health", "Personal", "Other")
    val colorPalette = listOf(SageGreen, Color(0xFFFFB74D), Color(0xFF64B5F6), Color(0xFF9575CD), Color(0xFFF06292), StoneGray)

    KomoriDialog(
        title = "New Schedule",
        onDismiss = onDismiss,
        onDelete = null,
        saveButtonText = "Save Schedule",
        onSave = {
            if (title.isNotBlank() && endTime.isAfter(startTime)) {
                // Pass selectedCategory and selectedColor.toArgb()
                onSave(title, description, selectedCategory, selectedDate, selectedRecurrence, startTime, endTime, selectedColor.toArgb())
            }
        }
    ) {
        // ... Title & Description Inputs (Same as before) ...
        CalmInputTextField(value = title, onValueChange = { title = it }, label = "Title", singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))
        CalmInputTextField(value = description, onValueChange = { description = it }, label = "Description", singleLine = false, minLines = 2, maxLines = 4)
        Spacer(modifier = Modifier.height(24.dp))

        // CATEGORIES (Decoupled Logic)
        Text("Category", style = MaterialTheme.typography.labelSmall, color = StoneGray, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        // CHANGE: Only set category. DO NOT change color.
                        selectedCategory = category
                        // Optional: Still auto-fill title if empty
                        if (title.isEmpty()) title = category
                    },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SageGreen, // Uniform selection color
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = StoneGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = StoneGray.copy(0.5f),
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // COLORS (Decoupled Logic)
        Text("Color Tag", style = MaterialTheme.typography.labelSmall, color = StoneGray, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            colorPalette.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(width = if (selectedColor == color) 3.dp else 0.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                        .clickable {
                            // CHANGE: Only set color. DO NOT reset category.
                            selectedColor = color
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        RecurrenceSelector(
            selectedRecurrence = selectedRecurrence,
            onRecurrenceSelected = { selectedRecurrence = it }
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 4. Date & Time
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Label changes based on recurrence
            val dateLabel = if (selectedRecurrence == "Once") "Date" else "Start Date"
            CompactSelector(label = dateLabel, value = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd")), onClick = { showDatePicker = true }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompactSelector(label = "Start", value = startTime.format(DateTimeFormatter.ofPattern("h:mm a")), onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f))
            CompactSelector(label = "End", value = endTime.format(DateTimeFormatter.ofPattern("h:mm a")), onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f))
        }
    }

    // --- PICKERS (Same as before) ---
    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dateState.selectedDateMillis?.let { selectedDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("OK", color = SageGreen) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = StoneGray) } }
        ) { DatePicker(state = dateState) }
    }
    if (showStartTimePicker) {
        TimePickerDialog(onDismiss = { showStartTimePicker = false }, onConfirm = { h, m -> startTime = LocalTime.of(h, m); if (endTime.isBefore(startTime)) endTime = startTime.plusHours(1); showStartTimePicker = false }, initialTime = startTime)
    }
    if (showEndTimePicker) {
        TimePickerDialog(onDismiss = { showEndTimePicker = false }, onConfirm = { h, m -> endTime = LocalTime.of(h, m); showEndTimePicker = false }, initialTime = endTime)
    }
}

// 2. UPDATED EDIT DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EditScheduleDialog(
    event: WeeklyEvent,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (WeeklyEvent) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    // INITIALIZE WITH SAVED CATEGORY AND COLOR
    var selectedCategory by remember { mutableStateOf(event.category) }
    var selectedColor by remember { mutableStateOf(Color(event.colorArgb)) }

    val today = LocalDate.now()
    val daysDiff = event.day.value - today.dayOfWeek.value
    var selectedRecurrence by remember { mutableStateOf(event.recurrence) }
    var selectedDate by remember { mutableStateOf(event.startDate) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }

    // Pickers & Formatters
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM dd") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    val categories = listOf("Work", "Study", "Health", "Personal", "Other")
    val colorPalette = listOf(SageGreen, Color(0xFFFFB74D), Color(0xFF64B5F6), Color(0xFF9575CD), Color(0xFFF06292), StoneGray)

    KomoriDialog(
        title = "Edit Schedule",
        onDismiss = onDismiss,
        onDelete = onDelete,
        saveButtonText = "Save",
        onSave = {
            if (title.isNotBlank() && endTime.isAfter(startTime)) {
                val updatedEvent = event.copy(
                    title = title,
                    description = description,
                    category = selectedCategory, // Save the updated category
                    startDate = selectedDate,   // Save Start Date
                    recurrence = selectedRecurrence, // Save Recurrence
                    endTime = endTime,
                    colorArgb = selectedColor.toArgb() // Save the updated color
                )
                onSave(updatedEvent)
            }
        }
    ) {
        // Inputs
        CalmInputTextField(value = title, onValueChange = { title = it }, label = "Title", singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))
        CalmInputTextField(value = description, onValueChange = { description = it }, label = "Description", singleLine = false, minLines = 2, maxLines = 4)
        Spacer(modifier = Modifier.height(24.dp))

        // CATEGORIES (Same decoupled logic as Add)
        Text("Category", style = MaterialTheme.typography.labelSmall, color = StoneGray, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SageGreen, selectedLabelColor = Color.White, containerColor = MaterialTheme.colorScheme.surface, labelColor = StoneGray),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = StoneGray.copy(0.5f), selectedBorderColor = Color.Transparent)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // COLORS (Same decoupled logic as Add)
        Text("Color Tag", style = MaterialTheme.typography.labelSmall, color = StoneGray, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            colorPalette.forEach { color ->
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(color)
                        .border(width = if (selectedColor == color) 3.dp else 0.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                        .clickable { selectedColor = color }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- NEW: RECURRENCE SELECTOR ---
        RecurrenceSelector(
            selectedRecurrence = selectedRecurrence,
            onRecurrenceSelected = { selectedRecurrence = it }
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Date/Time
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompactSelector(label = "Date", value = selectedDate.format(dateFormatter), onClick = { showDatePicker = true }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompactSelector(label = "Start", value = startTime.format(timeFormatter), onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f))
            CompactSelector(label = "End", value = endTime.format(timeFormatter), onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f))
        }
    }

    // --- PICKERS (Same logic) ---
    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dateState.selectedDateMillis?.let { selectedDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("OK", color = SageGreen) } }
        ) { DatePicker(state = dateState) }
    }
    if (showStartTimePicker) {
        TimePickerDialog(onDismiss = { showStartTimePicker = false }, onConfirm = { h, m -> startTime = LocalTime.of(h, m); if (endTime.isBefore(startTime)) endTime = startTime.plusHours(1); showStartTimePicker = false }, initialTime = startTime)
    }
    if (showEndTimePicker) {
        TimePickerDialog(onDismiss = { showEndTimePicker = false }, onConfirm = { h, m -> endTime = LocalTime.of(h, m); showEndTimePicker = false }, initialTime = endTime)
    }
}

// --- HELPER COMPOSABLES ---


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialTime: LocalTime
) {
    val timeState = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Time", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = timeState, colors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.surface,
                    selectorColor = SageGreen,
                    periodSelectorSelectedContainerColor = SageGreen.copy(alpha = 0.2f),
                    periodSelectorSelectedContentColor = SageGreen,
                    timeSelectorSelectedContainerColor = SageGreen.copy(alpha = 0.2f),
                    timeSelectorSelectedContentColor = SageGreen
                ))
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = StoneGray) }
                    TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) { Text("OK", color = SageGreen) }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventCard(event: WeeklyEvent, modifier: Modifier = Modifier) {
    Surface(
        color = event.color.copy(alpha = 0.25f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, event.color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(2.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFont
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleDetailDialog(
    event: WeeklyEvent,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    KomoriDialog(
        title = event.title,
        onDismiss = onDismiss,
        onDelete = null,
        saveButtonText = "Edit Event",
        saveButtonIcon = PhosphorIcons.Bold.PencilSimple,
        onSave = onEdit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. CATEGORY
            DetailRow(
                icon = PhosphorIcons.Bold.Tag,
                label = "Category",
                value = event.category,
                iconColor = event.color
            )

            // 2. DATE
            DetailRow(
                icon = PhosphorIcons.Bold.CalendarBlank,
                label = "Date",
                // Show full date since it might be a specific start date now
                value = event.startDate.format(DateTimeFormatter.ofPattern("EEEE, MMM dd")),
                iconColor = SageGreen
            )

            // 3. TIME
            DetailRow(
                icon = PhosphorIcons.Bold.Clock,
                label = "Time",
                value = "${event.startTime.format(timeFormatter)} - ${event.endTime.format(timeFormatter)}",
                iconColor = SageGreen
            )

            // 4. RECURRENCE (NEW)
            // Only show if it repeats
            if (event.recurrence != "Once") {
                DetailRow(
                    icon = PhosphorIcons.Bold.ArrowsClockwise, // Repeat Icon
                    label = "Repeat",
                    value = event.recurrence,
                    iconColor = SageGreen
                )
            }

            // 5. DESCRIPTION
            if (event.description.isNotBlank()) {
                DetailRow(
                    icon = PhosphorIcons.Bold.TextAlignLeft,
                    label = "Description",
                    value = event.description,
                    iconColor = SageGreen,
                    alignTop = true
                )
            }
        }
    }
}
// --- HELPER COMPOSABLE: The "Task Item" Style Row ---
@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconColor: Color,
    alignTop: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = if (alignTop) Alignment.Top else Alignment.CenterVertically
    ) {
        // 1. ICON BOX (Left side, replacing checkbox)
        Surface(
            color = iconColor.copy(alpha = 0.15f), // Soft background matching the color
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. TEXT DETAILS (Right side)
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = StoneGray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFont
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RecurrenceSelector(
    selectedRecurrence: String,
    onRecurrenceSelected: (String) -> Unit
) {
    val options = listOf("Once", "Daily", "Weekly", "Monthly", "Yearly")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Repeat",
            style = MaterialTheme.typography.labelSmall,
            color = StoneGray,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(options) { option ->
                val isSelected = selectedRecurrence == option
                FilterChip(
                    selected = isSelected,
                    onClick = { onRecurrenceSelected(option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SageGreen,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = StoneGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = StoneGray.copy(0.5f),
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}