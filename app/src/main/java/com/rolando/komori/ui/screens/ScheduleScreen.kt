package com.rolando.komori.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import java.time.format.TextStyle as JavaTextStyle
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.TimePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import androidx.palette.graphics.Palette
import com.adamglin.phosphoricons.bold.CaretUp
import com.adamglin.phosphoricons.bold.Palette
import com.adamglin.phosphoricons.bold.Plus
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.floor
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleScreen(
    events: List<WeeklyEvent>,
    jumpToDate: LocalDate?,
    onVisibleDateChanged: (LocalDate) -> Unit,
    onBottomReached: (Boolean) -> Unit = {},
    onEventClick: (WeeklyEvent) -> Unit,
    onAddEvent: (WeeklyEvent) -> Unit,
    onFabVisibilityChange: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val hourHeight = 64.dp
    val startHour = 0
    val endHour = 23
    val totalHours = endHour - startHour + 1
    val gridHeight = hourHeight * totalHours

    // --- NEW GHOST STATE ---
    var ghostDate by remember { mutableStateOf<LocalDate?>(null) }
    var ghostStartTime by remember { mutableStateOf<LocalTime?>(null) }
    var ghostEndTime by remember { mutableStateOf<LocalTime?>(null) }

    // --- STATE ---
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    val todayDate = remember { LocalDate.now() }

    val initialPage = 500
    val pagerState = rememberPagerState(initialPage = initialPage) { 1000 }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(60000)
        }
    }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("h a") }

    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictingEvents by remember { mutableStateOf<List<WeeklyEvent>>(emptyList()) }

    val hourHeightPx = with(density) { hourHeight.toPx() }

    var showAddDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // --- THEME COLORS ---
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary // Used for Gray text
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    LaunchedEffect(scrollState.value) {
        val currentScroll = scrollState.value
        val maxScroll = scrollState.maxValue
        val isAtBottom = maxScroll > 0 && currentScroll >= (maxScroll - 50)

        if (isAtBottom) {
            onFabVisibilityChange(false)
            onBottomReached(true)
        } else {
            onFabVisibilityChange(true)
            onBottomReached(false)
        }
    }

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

    LaunchedEffect(pagerState.currentPage) {
        val weeksOffset = pagerState.currentPage - initialPage
        val today = LocalDate.now()
        val currentViewMonday = today
            .minusDays((today.dayOfWeek.value - 1).toLong())
            .plusWeeks(weeksOffset.toLong())

        onVisibleDateChanged(currentViewMonday)
    }

    LaunchedEffect(jumpToDate) {
        jumpToDate?.let { target ->
            val today = LocalDate.now()
            val weeksDiff = ChronoUnit.WEEKS.between(today, target)
            pagerState.scrollToPage(initialPage + weeksDiff.toInt())
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

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

                // --- DAYS HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 48.dp, bottom = 12.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEach { date ->
                        val isToday = date == LocalDate.now()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = date.dayOfWeek.getDisplayName(
                                    JavaTextStyle.SHORT,
                                    Locale.getDefault()
                                ).uppercase().take(1),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontFamily = AppFont,
                                    fontWeight = FontWeight.Normal
                                ),
                                // FIX: Use Theme Colors
                                color = if (isToday) primaryColor else tertiaryColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50))
                                    // FIX: Use Theme Colors
                                    .background(if (isToday) primaryColor else Color.Transparent)
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 14.sp,
                                        fontFamily = AppFont,
                                        fontWeight = FontWeight.Normal
                                    ),
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
                                Box(
                                    modifier = Modifier.height(hourHeight),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        text = LocalTime.of(hour, 0).format(timeFormatter),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Normal,
                                            fontFamily = AppFont
                                        ),
                                        // FIX: Use Theme Color
                                        color = tertiaryColor,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        // EVENT COLUMNS
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Column {
                                for (hour in startHour..endHour) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(hourHeight)
                                            .border(
                                                width = 0.5.dp,
                                                color = outlineColor.copy(alpha = 0.2f)
                                            )
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxSize()) {
                                weekDays.forEach { date ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(width = 0.5.dp, color = outlineColor.copy(alpha = 0.15f))
                                            .pointerInput(Unit) {
                                                detectTapGestures { offset ->
                                                    if (ghostDate != null) {
                                                        val gStart = ghostStartTime!!
                                                        val gEnd = ghostEndTime!!
                                                        val startPx = (gStart.hour * hourHeightPx) + (gStart.minute / 60f * hourHeightPx)
                                                        val endPx = (gEnd.hour * hourHeightPx) + (gEnd.minute / 60f * hourHeightPx)
                                                        val safetyBuffer = 12.dp.toPx()

                                                        val isTapOnGhost = (ghostDate == date) &&
                                                                (offset.y >= (startPx - safetyBuffer) && offset.y <= (endPx + safetyBuffer))

                                                        if (isTapOnGhost) {
                                                            showAddDialog = true
                                                        } else {
                                                            ghostDate = null
                                                            ghostStartTime = null
                                                            ghostEndTime = null
                                                        }
                                                    } else {
                                                        val clickedHour = (offset.y / hourHeightPx).toInt().coerceIn(0, 23)
                                                        val clickedMinute = ((offset.y / hourHeightPx % 1) * 60).toInt()
                                                        val roundedMinute = if (clickedMinute < 30) 0 else 30
                                                        ghostDate = date
                                                        ghostStartTime = LocalTime.of(clickedHour, roundedMinute)
                                                        ghostEndTime = LocalTime.of(clickedHour, roundedMinute).plusHours(1)
                                                    }
                                                }
                                            }
                                    ) {

                                        // FILTER EVENTS
                                        val dayEvents = events.filter { event ->
                                            if (date.isBefore(event.startDate)) return@filter false
                                            val recEnd = event.recurrenceEnd
                                            if (recEnd != null && date.isAfter(recEnd)) return@filter false
                                            val dateString = date.toString()
                                            val exclusions = event.excludedDates.split(",")
                                            if (exclusions.contains(dateString)) return@filter false
                                            when (event.recurrence) {
                                                "Once" -> date.isEqual(event.startDate)
                                                "Daily" -> true
                                                "Weekly" -> date.dayOfWeek == event.startDate.dayOfWeek
                                                "Monthly" -> date.dayOfMonth == event.startDate.dayOfMonth
                                                "Yearly" -> date.dayOfYear == event.startDate.dayOfYear
                                                else -> false
                                            }
                                        }

                                        val groupedEvents = dayEvents.groupBy { it.startTime to it.endTime }

                                        groupedEvents.forEach { (_, group) ->
                                            val firstEvent = group.first()
                                            val eventStart = firstEvent.startTime.hour + (firstEvent.startTime.minute / 60f)
                                            val eventEnd = firstEvent.endTime.hour + (firstEvent.endTime.minute / 60f)
                                            val duration = eventEnd - eventStart
                                            val zIndex = if (group.size > 1) 100f else (1f / duration.coerceAtLeast(0.1f))

                                            if (eventStart >= startHour) {
                                                val offset = hourHeight * (eventStart - startHour)
                                                val height = hourHeight * (eventEnd - eventStart)

                                                if (group.size == 1) {
                                                    EventCard(
                                                        event = firstEvent,
                                                        modifier = Modifier
                                                            .padding(horizontal = 2.dp)
                                                            .offset(y = offset)
                                                            .height(height)
                                                            .fillMaxWidth()
                                                            .zIndex(zIndex)
                                                            .clickable { onEventClick(firstEvent.copy(startDate = date)) }
                                                    )
                                                } else {
                                                    GroupedEventCard(
                                                        events = group,
                                                        modifier = Modifier
                                                            .padding(horizontal = 2.dp)
                                                            .offset(y = offset)
                                                            .height(height)
                                                            .fillMaxWidth()
                                                            .zIndex(zIndex)
                                                            .clickable {
                                                                conflictingEvents = group.map { it.copy(startDate = date) }
                                                                showConflictDialog = true
                                                            }
                                                    )
                                                }
                                            }
                                        }

                                        if (ghostDate == date && ghostStartTime != null && ghostEndTime != null) {
                                            InteractiveGhostCard(
                                                startTime = ghostStartTime!!,
                                                endTime = ghostEndTime!!,
                                                hourHeightPx = hourHeightPx,
                                                onStartChanged = { ghostStartTime = it },
                                                onEndChanged = { ghostEndTime = it },
                                                onMove = { start, end -> ghostStartTime = start; ghostEndTime = end },
                                                onClick = { showAddDialog = true }
                                            )
                                        }

                                        if (date == LocalDate.now()) {
                                            val now = currentTime
                                            val currentHourVal = now.hour + (now.minute / 60f)
                                            val lineOffset = hourHeight * (currentHourVal - startHour)
                                            Canvas(
                                                modifier = Modifier.fillMaxWidth().height(2.dp).offset(y = lineOffset)
                                            ) {
                                                // FIX: Use Theme Color
                                                drawLine(
                                                    color = primaryColor,
                                                    start = Offset(0f, 0f),
                                                    end = Offset(size.width, 0f),
                                                    strokeWidth = 4f
                                                )
                                                drawCircle(
                                                    color = primaryColor,
                                                    radius = 6f,
                                                    center = Offset(0f, 0f)
                                                )
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

        if (showAddDialog && ghostDate != null) {
            AddScheduleDialog(
                initialDate = ghostDate!!,
                initialStartTime = ghostStartTime!!,
                initialEndTime = ghostEndTime!!,
                onDismiss = { showAddDialog = false },
                onSave = { title, desc, cat, d, rec, start, end, color ->
                    onAddEvent(WeeklyEvent(
                        title = title, description = desc, category = cat,
                        startDate = d, recurrence = rec, startTime = start, endTime = end,
                        colorArgb = color
                    ))
                    showAddDialog = false
                    ghostDate = null
                }
            )
        }

        if (showConflictDialog) {
            EventDisambiguationDialog(
                events = conflictingEvents,
                onDismiss = { showConflictDialog = false },
                onSelectEvent = { selectedEvent ->
                    showConflictDialog = false
                    onEventClick(selectedEvent)
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GroupedEventCard(events: List<WeeklyEvent>, modifier: Modifier = Modifier) {
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    // FIX: Use Theme Color
    val stripeColor = MaterialTheme.colorScheme.tertiary

    Surface(
        color = cardColor,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, stripeColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row {
            Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(stripeColor))
            Column(modifier = Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.Center) {
                Surface(color = stripeColor, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(bottom = 2.dp)) {
                    Text(
                        text = "${events.size} Events",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = "Tap to select",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventDisambiguationDialog(events: List<WeeklyEvent>, onDismiss: () -> Unit, onSelectEvent: (WeeklyEvent) -> Unit) {
    KomoriDialog(title = "Select Event", onDismiss = onDismiss, onSave = null, saveButtonText = null, onDelete = null, titleAlign = TextAlign.Start) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Multiple events are scheduled at this time:",
                style = MaterialTheme.typography.bodyMedium,
                // FIX: Use Theme Color
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            events.forEach { event ->
                Row(modifier = Modifier.fillMaxWidth().height(60.dp).clickable { onSelectEvent(event) }) {
                    EventCard(event = event, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleDialog(
    initialDate: LocalDate = LocalDate.now(),
    initialStartTime: LocalTime? = null,
    initialEndTime: LocalTime? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, LocalDate, String, LocalTime, LocalTime, Int) -> Unit
) {
    // FIX: Use Theme Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Work") }
    var selectedColor by remember { mutableIntStateOf(primaryColor.toArgb()) } // Default to Theme Color

    var startDate by remember { mutableStateOf(initialDate) }
    var startTime by remember { mutableStateOf(initialStartTime ?: LocalTime.now()) }
    var endTime by remember { mutableStateOf(initialEndTime ?: startTime.plusHours(1)) }
    var recurrence by remember { mutableStateOf("Once") }

    var showMoreOptions by remember { mutableStateOf(false) }
    var showCategory by remember { mutableStateOf(false) }
    var showColor by remember { mutableStateOf(false) }
    var showRecurrence by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // FIX: Use dynamic background based on Theme
    // --- DYNAMIC & COMPLIMENTARY COLORS ---
    // 1. Collapsed Item: Uses a distinct container color (MistGreen/DarkGrey)
    val itemBgColor = MaterialTheme.colorScheme.surfaceContainerHigh

    // 2. Expanded Container: Uses the standard surface color (White/Black) for contrast
    val expandedBgColor = MaterialTheme.colorScheme.surface

    // 3. Border: Uses the Primary color (Sage/Coffee/Ocean) to compliment the theme
    val expandedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    val itemShape = RoundedCornerShape(14.dp)

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val categoryNames = listOf("Work", "Personal", "School", "Health", "Finance", "Social", "Family", "Other")
    // FIX: Add primaryColor to options
    val colorOptions = listOf(primaryColor, Color(0xFF64B5F6), Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF4DB6AC), Color(0xFF7986CB), Color(0xFFF06292), Color(0xFF90A4AE), Color(0xFFA1887F))

    KomoriDialog(title = "New Event", onDismiss = onDismiss, onDelete = null, saveButtonText = "Save Event", titleAlign = TextAlign.Center, onSave = {
        if (title.isNotBlank()) {
            onSave(title, description, category, startDate, recurrence, startTime, endTime, selectedColor)
            onDismiss()
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = title, onValueChange = { title = it },
                placeholder = { Text("Event Title", style = MaterialTheme.typography.headlineSmall.copy(color = tertiaryColor.copy(alpha = 0.5f))) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = primaryColor),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            @Composable fun GreenAddOption(label: String, icon: ImageVector, onClick: () -> Unit) {
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
                Surface(shape = itemShape, color = expandedBgColor, border = BorderStroke(1.dp, expandedBorderColor), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) { content() }
                }
            }

            ExpandedOption {
                Text("WHEN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = tertiaryColor, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactSelector(label = "Date", value = startDate.format(dateFormatter), onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompactSelector(label = "Start", value = startTime.format(timeFormatter), onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f))
                        CompactSelector(label = "End", value = endTime.format(timeFormatter), onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { showMoreOptions = !showMoreOptions }.padding(vertical = 8.dp, horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (showMoreOptions) "Less Options" else "More Options", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = tertiaryColor)
                Icon(imageVector = if (showMoreOptions) PhosphorIcons.Bold.CaretUp else PhosphorIcons.Bold.CaretDown, contentDescription = "Toggle", tint = tertiaryColor)
            }

            if (showMoreOptions) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!showCategory) GreenAddOption(if (category != "Work") "Category: $category" else "Set Category", PhosphorIcons.Bold.Tag) { showCategory = true } else ExpandedOption {
                        SectionHeaderWithClose("CATEGORY", onClose = { showCategory = false })
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categoryNames.forEach { name ->
                                FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor, selectedLabelColor = Color.White, containerColor = itemBgColor, labelColor = tertiaryColor), border = null, shape = CircleShape)
                            }
                        }
                    }

                    if (!showColor) GreenAddOption("Set Color", PhosphorIcons.Bold.Palette) { showColor = true } else ExpandedOption {
                        SectionHeaderWithClose("COLOR CODE", onClose = { showColor = false })
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            colorOptions.forEach { color ->
                                val isSelected = selectedColor == color.toArgb()
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color).border(if (isSelected) 2.dp else 0.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape).clickable { selectedColor = color.toArgb() })
                            }
                        }
                    }

                    if (!showRecurrence) GreenAddOption("Repeat", PhosphorIcons.Bold.ArrowsClockwise) { showRecurrence = true } else ExpandedOption {
                        SectionHeaderWithClose("REPEAT", onClose = { showRecurrence = false })
                        val options = listOf("Once", "Daily", "Weekly", "Monthly")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { option ->
                                val isSelected = recurrence == option
                                Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp)).background(if (isSelected) primaryColor else itemBgColor).clickable { recurrence = option }, contentAlignment = Alignment.Center) {
                                    Text(text = option, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else tertiaryColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (!showDetails) GreenAddOption("Add Details", PhosphorIcons.Bold.TextAlignLeft) { showDetails = true } else ExpandedOption {
                        SectionHeaderWithClose("DETAILS", onClose = { showDetails = false })
                        OutlinedTextField(value = description, onValueChange = { description = it }, placeholder = { Text("Add notes...", color = tertiaryColor) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor.copy(alpha = 0.5f), unfocusedBorderColor = Color.Transparent, focusedContainerColor = itemBgColor, unfocusedContainerColor = itemBgColor, cursorColor = primaryColor), shape = RoundedCornerShape(12.dp), minLines = 3)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { startDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("OK", color = primaryColor) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = tertiaryColor) } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showStartTimePicker) {
        val timeState = rememberTimePickerState(initialHour = startTime.hour, initialMinute = startTime.minute)
        TimePickerDialogShim(onDismiss = { showStartTimePicker = false }, onConfirm = { startTime = LocalTime.of(timeState.hour, timeState.minute); showStartTimePicker = false }, state = timeState)
    }

    if (showEndTimePicker) {
        val timeState = rememberTimePickerState(initialHour = endTime.hour, initialMinute = endTime.minute)
        TimePickerDialogShim(onDismiss = { showEndTimePicker = false }, onConfirm = { endTime = LocalTime.of(timeState.hour, timeState.minute); showEndTimePicker = false }, state = timeState)
    }

}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleDetailDialog(event: WeeklyEvent, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: (DeleteScope) -> Unit) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    var showRecurrenceOptions by remember { mutableStateOf(false) }
    // FIX: Use Theme Color
    val primaryColor = MaterialTheme.colorScheme.primary

    KomoriDialog(title = event.title, onDismiss = onDismiss, onDelete = {
        if (event.recurrence == "Once" || event.recurrence == "None") onDelete(DeleteScope.THIS_EVENT) else showRecurrenceOptions = true
    }, onSave = null, saveButtonText = null, titleAlign = TextAlign.Start) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailRow(icon = PhosphorIcons.Bold.Tag, label = "Category", value = event.category, iconColor = event.color)
            DetailRow(icon = PhosphorIcons.Bold.CalendarBlank, label = "Date", value = event.startDate.format(DateTimeFormatter.ofPattern("EEEE, MMM dd")), iconColor = primaryColor)
            DetailRow(icon = PhosphorIcons.Bold.Clock, label = "Time", value = "${event.startTime.format(timeFormatter)} - ${event.endTime.format(timeFormatter)}", iconColor = primaryColor)
            if (event.recurrence != "Once") DetailRow(icon = PhosphorIcons.Bold.ArrowsClockwise, label = "Repeat", value = event.recurrence, iconColor = primaryColor)
            if (event.description.isNotBlank()) DetailRow(icon = PhosphorIcons.Bold.TextAlignLeft, label = "Description", value = event.description, iconColor = primaryColor, alignTop = true)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(primaryColor.copy(alpha = 0.1f)).border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).clickable { onEdit() }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = PhosphorIcons.Bold.PencilSimple, contentDescription = "Edit", tint = primaryColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Edit Event", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = primaryColor)
                }
            }
        }
    }
    if (showRecurrenceOptions) RecurrenceDeleteDialog(onDismiss = { showRecurrenceOptions = false }, onConfirm = { showRecurrenceOptions = false; onDelete(it) })
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleDialog(event: WeeklyEvent, onDismiss: () -> Unit, onDelete: (DeleteScope) -> Unit, onSave: (WeeklyEvent) -> Unit) {
    // FIX: Theme Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    var category by remember { mutableStateOf(event.category) }
    var selectedColor by remember { mutableIntStateOf(event.colorArgb) }
    var startDate by remember { mutableStateOf(event.startDate) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }
    var recurrence by remember { mutableStateOf(event.recurrence) }
    var showRecurrenceOptions by remember { mutableStateOf(false) }

    var showDetails by remember { mutableStateOf(event.description.isNotBlank()) }
    var showTime by remember { mutableStateOf(true) }
    var showCategory by remember { mutableStateOf(true) }
    var showColor by remember { mutableStateOf(true) }
    var showRecurrence by remember { mutableStateOf(event.recurrence != "Once") }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // --- DYNAMIC & COMPLIMENTARY COLORS ---
    // 1. Collapsed Item: Uses a distinct container color (MistGreen/DarkGrey)
    val itemBgColor = MaterialTheme.colorScheme.surfaceContainerHigh

    // 2. Expanded Container: Uses the standard surface color (White/Black) for contrast
    val expandedBgColor = MaterialTheme.colorScheme.surface

    // 3. Border: Uses the Primary color (Sage/Coffee/Ocean) to compliment the theme
    val expandedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    val itemShape = RoundedCornerShape(14.dp)

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val categoryNames = listOf("Work", "Personal", "School", "Health", "Finance", "Social", "Family", "Other")
    val colorOptions = listOf(primaryColor, Color(0xFF64B5F6), Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF4DB6AC), Color(0xFF7986CB), Color(0xFFF06292), Color(0xFF90A4AE), Color(0xFFA1887F))

    KomoriDialog(title = "Edit Event", onDismiss = onDismiss, onDelete = { if (event.recurrence == "Once" || event.recurrence == "None") onDelete(DeleteScope.THIS_EVENT) else showRecurrenceOptions = true }, saveButtonText = "Save", titleAlign = TextAlign.Center, onSave = {
        if (title.isNotBlank()) {
            onSave(event.copy(title = title, description = description, category = category, colorArgb = selectedColor, startDate = startDate, startTime = startTime, endTime = endTime, recurrence = recurrence))
            onDismiss()
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = title, onValueChange = { title = it },
                placeholder = { Text("Event Title", style = MaterialTheme.typography.headlineSmall.copy(color = tertiaryColor.copy(alpha = 0.5f))) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = primaryColor),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            @Composable fun GreenAddOption(label: String, icon: ImageVector, onClick: () -> Unit) {
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
                Surface(shape = itemShape, color = expandedBgColor, border = BorderStroke(1.dp, expandedBorderColor), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) { content() }
                }
            }
            if (!showTime) GreenAddOption("Set Date & Time", PhosphorIcons.Bold.Clock) { showTime = true } else ExpandedOption {
                SectionHeaderWithClose("WHEN", onClose = { showTime = false })
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactSelector(label = "Date", value = startDate.format(dateFormatter), onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompactSelector(label = "Start", value = startTime.format(timeFormatter), onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f))
                        CompactSelector(label = "End", value = endTime.format(timeFormatter), onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (!showCategory) GreenAddOption("Category: $category", PhosphorIcons.Bold.Tag) { showCategory = true } else ExpandedOption {
                SectionHeaderWithClose("CATEGORY", onClose = { showCategory = false })
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryNames.forEach { name ->
                        FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor, selectedLabelColor = Color.White, containerColor = itemBgColor, labelColor = tertiaryColor), border = null, shape = CircleShape)
                    }
                }
            }
            if (!showColor) GreenAddOption("Set Color", PhosphorIcons.Bold.Palette) { showColor = true } else ExpandedOption {
                SectionHeaderWithClose("COLOR CODE", onClose = { showColor = false })
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    colorOptions.forEach { color ->
                        val isSelected = selectedColor == color.toArgb()
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color).border(if (isSelected) 2.dp else 0.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape).clickable { selectedColor = color.toArgb() })
                    }
                }
            }
            if (!showRecurrence) GreenAddOption("Repeat", PhosphorIcons.Bold.ArrowsClockwise) { showRecurrence = true } else ExpandedOption {
                SectionHeaderWithClose("REPEAT", onClose = { showRecurrence = false })
                val options = listOf("Once", "Daily", "Weekly", "Monthly")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        val isSelected = recurrence == option
                        Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp)).background(if (isSelected) primaryColor else itemBgColor).clickable { recurrence = option }, contentAlignment = Alignment.Center) {
                            Text(text = option, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else tertiaryColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (!showDetails) GreenAddOption("Add Details", PhosphorIcons.Bold.TextAlignLeft) { showDetails = true } else ExpandedOption {
                SectionHeaderWithClose("DETAILS", onClose = { showDetails = false })
                OutlinedTextField(value = description, onValueChange = { description = it }, placeholder = { Text("Add notes...", color = tertiaryColor) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor.copy(alpha = 0.5f), unfocusedBorderColor = Color.Transparent, focusedContainerColor = itemBgColor, unfocusedContainerColor = itemBgColor, cursorColor = primaryColor), shape = RoundedCornerShape(12.dp), minLines = 3)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { startDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("OK", color = primaryColor) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = tertiaryColor) } }
        ) { DatePicker(state = datePickerState) }
    }
    if (showStartTimePicker) {
        val timeState = rememberTimePickerState(initialHour = startTime.hour, initialMinute = startTime.minute)
        TimePickerDialogShim(onDismiss = { showStartTimePicker = false }, onConfirm = { startTime = LocalTime.of(timeState.hour, timeState.minute); showStartTimePicker = false }, state = timeState)
    }
    if (showEndTimePicker) {
        val timeState = rememberTimePickerState(initialHour = endTime.hour, initialMinute = endTime.minute)
        TimePickerDialogShim(onDismiss = { showEndTimePicker = false }, onConfirm = { endTime = LocalTime.of(timeState.hour, timeState.minute); showEndTimePicker = false }, state = timeState)
    }
    if (showRecurrenceOptions) { RecurrenceDeleteDialog(onDismiss = { showRecurrenceOptions = false }, onConfirm = { showRecurrenceOptions = false; onDelete(it) }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogShim(onDismiss: () -> Unit, onConfirm: () -> Unit, state: androidx.compose.material3.TimePickerState) {
    // FIX: Theme Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val isAppInDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val unselectedBgColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val clockDialColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Select time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp))
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = clockDialColor, clockDialSelectedContentColor = Color.White, clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        selectorColor = primaryColor, periodSelectorBorderColor = primaryColor.copy(alpha = 0.5f), periodSelectorSelectedContainerColor = primaryColor.copy(alpha = 0.2f),
                        periodSelectorSelectedContentColor = primaryColor, periodSelectorUnselectedContainerColor = Color.Transparent, periodSelectorUnselectedContentColor = tertiaryColor,
                        timeSelectorSelectedContainerColor = primaryColor.copy(alpha = 0.2f), timeSelectorSelectedContentColor = primaryColor, timeSelectorUnselectedContainerColor = unselectedBgColor, timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = tertiaryColor, fontWeight = FontWeight.Medium) }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) { Text("OK", color = primaryColor, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES ---

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit, initialTime: LocalTime) {
    val timeState = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute)
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val unselectedBgColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val clockDialColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Time", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(
                    state = timeState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = clockDialColor, selectorColor = primaryColor, periodSelectorSelectedContainerColor = primaryColor.copy(alpha = 0.2f),
                        periodSelectorSelectedContentColor = primaryColor, timeSelectorSelectedContainerColor = primaryColor.copy(alpha = 0.2f), timeSelectorSelectedContentColor = primaryColor, timeSelectorUnselectedContainerColor = unselectedBgColor
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = tertiaryColor) }
                    TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) { Text("OK", color = primaryColor) }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventCard(event: WeeklyEvent, modifier: Modifier = Modifier) {
    Surface(color = event.color.copy(alpha = 0.25f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, event.color.copy(alpha = 0.5f)), modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(3.dp), verticalArrangement = Arrangement.Top) {
            Text(text = event.title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont, lineHeight = 11.sp), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String, iconColor: Color, alignTop: Boolean = false) {
    // FIX: Use Theme Colors for Text
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = if (alignTop) Alignment.Top else Alignment.CenterVertically) {
        Surface(color = iconColor.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp)) }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = tertiaryColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontFamily = AppFont), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun RecurrenceSelector(selectedRecurrence: String, onRecurrenceSelected: (String) -> Unit) {
    val options = listOf("Once", "Daily", "Weekly", "Monthly", "Yearly")
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Repeat", style = MaterialTheme.typography.labelSmall, color = tertiaryColor, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(options) { option ->
                val isSelected = selectedRecurrence == option
                FilterChip(
                    selected = isSelected, onClick = { onRecurrenceSelected(option) }, label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor, selectedLabelColor = Color.White, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, labelColor = tertiaryColor),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = tertiaryColor.copy(0.5f), selectedBorderColor = Color.Transparent)
                )
            }
        }
    }
}

enum class DeleteScope { THIS_EVENT, FOLLOWING_EVENTS, ALL_EVENTS }

@Composable
fun RecurrenceDeleteDialog(onDismiss: () -> Unit, onConfirm: (DeleteScope) -> Unit) {
    var selectedScope by remember { mutableStateOf(DeleteScope.THIS_EVENT) }
    KomoriDialog(title = "Delete repeating event", onDismiss = onDismiss, onDelete = null, saveButtonText = "Delete", onSave = { onConfirm(selectedScope) }, titleAlign = TextAlign.Start) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "This is a recurring event. Do you want to delete only this event, this and all following events, or all events in the series?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(bottom = 16.dp))
            DeleteOptionRow(text = "This event only", selected = selectedScope == DeleteScope.THIS_EVENT, onClick = { selectedScope = DeleteScope.THIS_EVENT })
            DeleteOptionRow(text = "This and following events", selected = selectedScope == DeleteScope.FOLLOWING_EVENTS, onClick = { selectedScope = DeleteScope.FOLLOWING_EVENTS })
            DeleteOptionRow(text = "All events", selected = selectedScope == DeleteScope.ALL_EVENTS, onClick = { selectedScope = DeleteScope.ALL_EVENTS })
        }
    }
}

@Composable
fun DeleteOptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = primaryColor, unselectedColor = tertiaryColor))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal), color = MaterialTheme.colorScheme.onSurface)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun InteractiveGhostCard(
    startTime: LocalTime,
    endTime: LocalTime,
    hourHeightPx: Float,
    onStartChanged: (LocalTime) -> Unit,
    onEndChanged: (LocalTime) -> Unit,
    onMove: (LocalTime, LocalTime) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pxPerMinute = hourHeightPx / 60f
    // FIX: Theme Colors
    val primaryColor = MaterialTheme.colorScheme.primary

    var draggingStart by remember { mutableStateOf<LocalTime?>(null) }
    var draggingEnd by remember { mutableStateOf<LocalTime?>(null) }
    var isDraggingBody by remember { mutableStateOf(false) }
    var anchorStartTime by remember { mutableStateOf<LocalTime?>(null) }
    var anchorEndTime by remember { mutableStateOf<LocalTime?>(null) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    val uiStartTime = draggingStart ?: startTime
    val uiEndTime = draggingEnd ?: endTime
    val startPx = (uiStartTime.hour * hourHeightPx) + (uiStartTime.minute / 60f * hourHeightPx)
    val endPx = (uiEndTime.hour * hourHeightPx) + (uiEndTime.minute / 60f * hourHeightPx)
    val heightPx = (endPx - startPx).coerceAtLeast(hourHeightPx / 4)

    Box(
        modifier = modifier
            .offset(y = with(density) { startPx.toDp() })
            .height(with(density) { heightPx.toDp() })
            .fillMaxWidth()
            .zIndex(if (isDraggingBody || draggingStart != null || draggingEnd != null) 300f else 200f)
            .graphicsLayer { if (isDraggingBody) { scaleX = 1.02f; scaleY = 1.02f; shadowElevation = 8.dp.toPx() } }
    ) {
        Surface(
            color = Color.Transparent, shape = RoundedCornerShape(6.dp), border = BorderStroke(2.dp, primaryColor),
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 1.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { isDraggingBody = true; anchorStartTime = startTime; anchorEndTime = endTime; totalDragY = 0f },
                        onDragEnd = { isDraggingBody = false; if (draggingStart != null && draggingEnd != null) { onMove(draggingStart!!, draggingEnd!!) }; draggingStart = null; draggingEnd = null },
                        onDragCancel = { isDraggingBody = false; draggingStart = null; draggingEnd = null }
                    ) { change, dragAmount ->
                        change.consume(); totalDragY += dragAmount.y
                        val rawMinutes = (totalDragY / pxPerMinute).toInt(); val snappedMinutes = (rawMinutes / 5) * 5
                        if (anchorStartTime != null && anchorEndTime != null) {
                            val newStart = anchorStartTime!!.plusMinutes(snappedMinutes.toLong()); val newEnd = anchorEndTime!!.plusMinutes(snappedMinutes.toLong())
                            if (newStart.hour >= 0 && newEnd.hour <= 23 && !(newEnd.hour == 23 && newEnd.minute > 59)) { draggingStart = newStart; draggingEnd = newEnd }
                        }
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isDraggingBody) Text(text = "${uiStartTime.format(DateTimeFormatter.ofPattern("h:mm"))} - ${uiEndTime.format(DateTimeFormatter.ofPattern("h:mm"))}", style = MaterialTheme.typography.labelSmall, color = primaryColor, fontWeight = FontWeight.Bold)
            }
        }
        Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-24).dp).size(48.dp).pointerInput(Unit) {
            detectDragGestures(onDragStart = { anchorStartTime = startTime; totalDragY = 0f }, onDragEnd = { if (draggingStart != null) onStartChanged(draggingStart!!); draggingStart = null }, onDragCancel = { draggingStart = null }) { change, dragAmount ->
                change.consume(); totalDragY += dragAmount.y
                val rawMinutes = (totalDragY / pxPerMinute).toInt(); val snappedMinutes = (rawMinutes / 5) * 5
                if (anchorStartTime != null) { val newStart = anchorStartTime!!.plusMinutes(snappedMinutes.toLong()); if (newStart.isBefore(uiEndTime.minusMinutes(15))) { draggingStart = newStart } }
            }
        }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).border(2.dp, primaryColor, CircleShape))
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 24.dp).size(48.dp).pointerInput(Unit) {
            detectDragGestures(onDragStart = { anchorEndTime = endTime; totalDragY = 0f }, onDragEnd = { if (draggingEnd != null) onEndChanged(draggingEnd!!); draggingEnd = null }, onDragCancel = { draggingEnd = null }) { change, dragAmount ->
                change.consume(); totalDragY += dragAmount.y
                val rawMinutes = (totalDragY / pxPerMinute).toInt(); val snappedMinutes = (rawMinutes / 5) * 5
                if (anchorEndTime != null) { val newEnd = anchorEndTime!!.plusMinutes(snappedMinutes.toLong()); if (newEnd.isAfter(uiStartTime.plusMinutes(15))) { draggingEnd = newEnd } }
            }
        }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).border(2.dp, primaryColor, CircleShape))
        }
    }
}