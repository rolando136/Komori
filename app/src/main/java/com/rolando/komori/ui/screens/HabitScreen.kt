package com.rolando.komori.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.*
import com.rolando.komori.data.Habit
import com.rolando.komori.ui.components.KomoriDialog
import com.rolando.komori.ui.theme.AppFont
import com.rolando.komori.ui.theme.StoneGray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

val AvailableHabitIcons = listOf(
    "FlowerLotus" to PhosphorIcons.Bold.FlowerLotus,
    "BookOpen" to PhosphorIcons.Bold.BookOpen,
    "Drop" to PhosphorIcons.Bold.Drop,
    "Barbell" to PhosphorIcons.Bold.Barbell,
    "PencilSimple" to PhosphorIcons.Bold.PencilSimple,
    "Heart" to PhosphorIcons.Bold.Heart,
    "Sun" to PhosphorIcons.Bold.Sun,
    "Moon" to PhosphorIcons.Bold.Moon,
    "Footprints" to PhosphorIcons.Bold.Footprints,
    "Bicycle" to PhosphorIcons.Bold.Bicycle,
    "Sparkle" to PhosphorIcons.Bold.Sparkle,
    "Coffee" to PhosphorIcons.Bold.Coffee,
    "Plant" to PhosphorIcons.Bold.Plant
)

fun getHabitIcon(iconName: String): ImageVector {
    return AvailableHabitIcons.firstOrNull { it.first == iconName }?.second
        ?: PhosphorIcons.Bold.FlowerLotus
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HabitsScreen(
    habits: List<Habit>,
    onDayToggle: (Habit, Int) -> Unit = { _, _ -> },
    onDateToggle: (Habit, LocalDate) -> Unit = { habit, date ->
        val dayIndex = date.dayOfWeek.value - 1
        onDayToggle(habit, dayIndex)
    },
    onHabitClick: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryColor = MaterialTheme.colorScheme.primary

    var weekOffset by remember { mutableIntStateOf(0) }
    val today = remember { LocalDate.now() }
    val currentWeekMonday = remember(today) { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val selectedWeekMonday = remember(weekOffset, currentWeekMonday) {
        currentWeekMonday.plusWeeks(weekOffset.toLong())
    }
    val selectedWeekSunday = remember(selectedWeekMonday) { selectedWeekMonday.plusDays(6) }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val rangeText = remember(selectedWeekMonday, selectedWeekSunday) {
        if (selectedWeekMonday.year == selectedWeekSunday.year) {
            "${selectedWeekMonday.format(monthFormatter)} - ${selectedWeekSunday.format(monthFormatter)}"
        } else {
            "${selectedWeekMonday.format(monthFormatter)}, ${selectedWeekMonday.year} - ${selectedWeekSunday.format(monthFormatter)}, ${selectedWeekSunday.year}"
        }
    }

    if (habits.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No rituals yet.\nTap + to start.",
                color = tertiaryColor,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- WEEK NAVIGATION BAR ---
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { weekOffset-- },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.CaretLeft,
                                contentDescription = "Previous Week",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (weekOffset == 0) "This Week" else if (weekOffset == -1) "Last Week" else if (weekOffset == 1) "Next Week" else "Week",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = AppFont,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = primaryColor
                                )
                                if (weekOffset != 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Today",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = AppFont,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = primaryColor,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(primaryColor.copy(alpha = 0.12f))
                                            .clickable { weekOffset = 0 }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = rangeText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = AppFont,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { weekOffset++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.CaretRight,
                                contentDescription = "Next Week",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // --- HABIT CARDS ---
            items(habits, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    startOfWeek = selectedWeekMonday,
                    onDateToggle = { date -> onDateToggle(habit, date) },
                    onClick = { onHabitClick(habit) }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HabitCard(
    habit: Habit,
    startOfWeek: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    onDateToggle: (LocalDate) -> Unit,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val today = LocalDate.now()

    val daysInWeek = remember(startOfWeek) {
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }
    val completedThisWeek = remember(habit.completedDates, startOfWeek) {
        habit.getCompletionsInWeek(startOfWeek)
    }
    val targetDays = habit.targetDaysPerWeek.coerceIn(1, 7)
    val progressFraction = (completedThisWeek.toFloat() / targetDays.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = surfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Icon, Title, Streak Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Habit Icon Container
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getHabitIcon(habit.iconName),
                        contentDescription = habit.name,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Habit Title & Target Label
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFont
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (targetDays == 7) "Daily ritual • $completedThisWeek/7 this week"
                               else "Target: $targetDays d/wk • $completedThisWeek/$targetDays completed",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppFont),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Streak badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Current Streak
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFA726).copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Bold.Fire,
                            contentDescription = "Streak",
                            tint = Color(0xFFFFA726),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${habit.streak}d",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFont
                            ),
                            color = Color(0xFFFFA726)
                        )
                    }

                    // Best streak badge (if available)
                    if (habit.bestStreak > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(primaryColor.copy(alpha = 0.10f))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.Trophy,
                                contentDescription = "Best Streak",
                                tint = primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${habit.bestStreak}d",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = AppFont
                                ),
                                color = primaryColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mini Progress Bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = primaryColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 7 Days Grid for the selected week
            val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                daysInWeek.forEachIndexed { index, date ->
                    val isCompleted = habit.isCompletedOn(date)
                    val isToday = date == today
                    val isFuture = date.isAfter(today)

                    val bgColor by animateColorAsState(
                        targetValue = when {
                            isCompleted -> primaryColor
                            isToday -> primaryColor.copy(alpha = 0.14f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        label = "dayBg"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = when {
                            isCompleted -> Color.White
                            isToday -> primaryColor
                            isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "dayContent"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Day name label (M, T, W...)
                        Text(
                            text = dayNames[index],
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = AppFont
                            ),
                            color = if (isToday) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Day circle / square button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .then(
                                    if (isToday && !isCompleted) {
                                        Modifier.border(
                                            width = 1.5.dp,
                                            color = primaryColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    } else {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = outlineColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                )
                                .clickable {
                                    onDateToggle(date)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = PhosphorIcons.Bold.Check,
                                    contentDescription = "Done",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "${date.dayOfMonth}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = AppFont
                                    ),
                                    color = contentColor
                                )
                            }
                        }

                        // Subtle today indicator dot
                        Spacer(modifier = Modifier.height(3.dp))
                        if (isToday) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(4.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- DETAILED HABIT DIALOG WITH CALENDAR HEATMAP & METRICS ---

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HabitDetailDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDateToggle: (LocalDate) -> Unit,
    onDelete: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerHigh
    val today = LocalDate.now()

    var monthOffset by remember { mutableIntStateOf(0) }
    val displayMonth = remember(monthOffset) {
        YearMonth.now().plusMonths(monthOffset.toLong())
    }
    val monthTitleFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }

    KomoriDialog(
        title = habit.name,
        titleAlign = TextAlign.Center,
        onDismiss = onDismiss,
        onDelete = onDelete,
        saveButtonText = "Close",
        onSave = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon & Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getHabitIcon(habit.iconName),
                        contentDescription = habit.name,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (habit.targetDaysPerWeek == 7) "Daily Habit" else "Target: ${habit.targetDaysPerWeek} days / week",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFont
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap any day to toggle check-in",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppFont),
                        color = tertiaryColor
                    )
                }
            }

            // Key Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "CURRENT",
                    value = "${habit.streak}d",
                    icon = PhosphorIcons.Bold.Fire,
                    tint = Color(0xFFFFA726),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "BEST",
                    value = "${habit.bestStreak}d",
                    icon = PhosphorIcons.Bold.Trophy,
                    tint = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "TOTAL",
                    value = "${habit.completedDates.size}",
                    icon = PhosphorIcons.Bold.Check,
                    tint = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "30-DAY",
                    value = "${(habit.calculateCompletionRate(30) * 100).toInt()}%",
                    icon = PhosphorIcons.Bold.ChartPieSlice,
                    tint = primaryColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Monthly Calendar Heatmap
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Month Navigation Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { monthOffset-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.CaretLeft,
                                contentDescription = "Previous Month",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = displayMonth.format(monthTitleFormatter),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFont
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { monthOffset++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.CaretRight,
                                contentDescription = "Next Month",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Day of Week Header (Mon..Sun)
                    val daysHeader = listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysHeader.forEach { d ->
                            Text(
                                text = d,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = AppFont
                                ),
                                color = tertiaryColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Days Grid
                    val firstDayOfMonth = displayMonth.atDay(1)
                    val daysInMonth = displayMonth.lengthOfMonth()
                    // 1 = Monday, 7 = Sunday
                    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value - 1

                    val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        var cellIndex = 0
                        while (cellIndex < totalCells) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (col in 0..6) {
                                    val dayNum = cellIndex - firstDayOfWeek + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val cellDate = displayMonth.atDay(dayNum)
                                        val isCompleted = habit.isCompletedOn(cellDate)
                                        val isToday = cellDate == today
                                        val isFuture = cellDate.isAfter(today)

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isCompleted) primaryColor
                                                    else if (isToday) primaryColor.copy(alpha = 0.12f)
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                                .then(
                                                    if (isToday && !isCompleted) {
                                                        Modifier.border(
                                                            1.5.dp,
                                                            primaryColor,
                                                            RoundedCornerShape(10.dp)
                                                        )
                                                    } else {
                                                        Modifier.border(
                                                            0.5.dp,
                                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                            RoundedCornerShape(10.dp)
                                                        )
                                                    }
                                                )
                                                .clickable {
                                                    onDateToggle(cellDate)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isToday || isCompleted) FontWeight.Bold else FontWeight.Normal,
                                                    fontFamily = AppFont
                                                ),
                                                color = when {
                                                    isCompleted -> Color.White
                                                    isToday -> primaryColor
                                                    isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(36.dp))
                                    }
                                    cellIndex++
                                }
                            }
                        }
                    }
                }
            }

            // Edit Ritual Button
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = primaryColor
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = PhosphorIcons.Bold.PencilSimple,
                    contentDescription = "Edit",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Ritual Details & Target",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = AppFont,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = AppFont
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- ADD & EDIT HABIT DIALOGS ---

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var habitName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(AvailableHabitIcons.first().first) }
    var targetDays by remember { mutableIntStateOf(7) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val itemBgColor = MaterialTheme.colorScheme.surfaceContainerHigh

    KomoriDialog(
        title = "New Ritual",
        titleAlign = TextAlign.Center,
        onDismiss = onDismiss,
        saveButtonText = "Save Ritual",
        onSave = {
            if (habitName.isNotBlank()) {
                onSave(habitName.trim(), selectedIcon, targetDays)
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextField(
                value = habitName,
                onValueChange = { habitName = it },
                placeholder = {
                    Text(
                        "e.g., Morning Meditation",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = tertiaryColor.copy(alpha = 0.5f),
                            fontFamily = AppFont
                        )
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontFamily = AppFont
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

            // Target Frequency
            Text(
                text = "WEEKLY TARGET FREQUENCY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = AppFont
                ),
                color = tertiaryColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(7 to "Daily (7d)", 5 to "5 days", 3 to "3 days", 1 to "1 day").forEach { (days, label) ->
                    val isSelected = targetDays == days
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { targetDays = days },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) primaryColor else itemBgColor,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = AppFont
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // Choose Icon
            Text(
                text = "CHOOSE ICON",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = AppFont
                ),
                color = tertiaryColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvailableHabitIcons.forEach { (name, icon) ->
                    val isSelected = selectedIcon == name
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) primaryColor else itemBgColor)
                            .clickable { selectedIcon = name },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = if (isSelected) Color.White else tertiaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditHabitDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onDelete: () -> Unit
) {
    var editedName by remember { mutableStateOf(habit.name) }
    var selectedIcon by remember { mutableStateOf(habit.iconName) }
    var targetDays by remember { mutableIntStateOf(habit.targetDaysPerWeek) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val itemBgColor = MaterialTheme.colorScheme.surfaceContainerHigh

    KomoriDialog(
        title = "Edit Ritual",
        titleAlign = TextAlign.Center,
        onDismiss = onDismiss,
        onDelete = onDelete,
        saveButtonText = "Save",
        onSave = {
            if (editedName.isNotBlank()) {
                onSave(
                    habit.copy(
                        name = editedName.trim(),
                        iconName = selectedIcon,
                        targetDaysPerWeek = targetDays
                    )
                )
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextField(
                value = editedName,
                onValueChange = { editedName = it },
                placeholder = {
                    Text(
                        "Ritual name",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = tertiaryColor.copy(alpha = 0.5f),
                            fontFamily = AppFont
                        )
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontFamily = AppFont
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

            // Target Frequency
            Text(
                text = "WEEKLY TARGET FREQUENCY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = AppFont
                ),
                color = tertiaryColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(7 to "Daily (7d)", 5 to "5 days", 3 to "3 days", 1 to "1 day").forEach { (days, label) ->
                    val isSelected = targetDays == days
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { targetDays = days },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) primaryColor else itemBgColor,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = AppFont
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // Choose Icon
            Text(
                text = "CHOOSE ICON",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = AppFont
                ),
                color = tertiaryColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvailableHabitIcons.forEach { (name, icon) ->
                    val isSelected = selectedIcon == name
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) primaryColor else itemBgColor)
                            .clickable { selectedIcon = name },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = if (isSelected) Color.White else tertiaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
