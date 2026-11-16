package com.rolando.komori.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.*
import com.rolando.komori.data.Habit
import com.rolando.komori.data.Task
import com.rolando.komori.data.WeeklyEvent
import com.rolando.komori.ui.TaskViewModel
import com.rolando.komori.ui.theme.AppFont
import com.rolando.komori.ui.theme.StoneGray
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory)
) {
    val taskList by viewModel.allTasks.collectAsState()
    val tasks = taskList ?: emptyList()
    val habits by viewModel.allHabits.collectAsState()
    val weeklyEvents by viewModel.scheduleEvents.collectAsState()
    val notes by viewModel.allNotes.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    // Task Analytics
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val pendingTasks = totalTasks - completedTasks
    val taskCompletionRate = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks) else 0f

    val highPriorityCount = tasks.count { it.priority == com.rolando.komori.data.TaskPriority.HIGH }
    val mediumPriorityCount = tasks.count { it.priority == com.rolando.komori.data.TaskPriority.MEDIUM }
    val lowPriorityCount = tasks.count { it.priority == com.rolando.komori.data.TaskPriority.LOW }

    // Ritual / Habit Analytics
    val totalHabits = habits.size
    val activeStreaksCount = habits.count { it.streak > 0 }
    val maxStreak = habits.maxOfOrNull { maxOf(it.streak, it.bestStreak) } ?: 0
    val averageStreak = if (totalHabits > 0) habits.map { it.streak }.average().toInt() else 0

    val today = remember { LocalDate.now() }
    val habitsCompletedToday = habits.count { it.isCompletedOn(today) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics & Insights",
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // --- HEADER SUMMARY CARDS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsStatCard(
                    title = "Rituals Today",
                    value = "$habitsCompletedToday / $totalHabits",
                    subtitle = if (totalHabits > 0 && habitsCompletedToday == totalHabits) "All done today!" else "In progress",
                    icon = PhosphorIcons.Bold.FlowerLotus,
                    accentColor = primaryColor,
                    modifier = Modifier.weight(1f)
                )

                AnalyticsStatCard(
                    title = "Top Streak",
                    value = "${maxStreak}d",
                    subtitle = "Average: ${averageStreak}d",
                    icon = PhosphorIcons.Bold.Fire,
                    accentColor = Color(0xFFFFA726),
                    modifier = Modifier.weight(1f)
                )
            }

            // --- SECTION 1: HABIT CONSISTENCY & RITUALS ---
            Text(
                text = "RITUALS CONSISTENCY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = AppFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = StoneGray
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Habit Performance",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFont
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "$activeStreaksCount Active Streaks",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AppFont
                            ),
                            color = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (habits.isEmpty()) {
                        Text(
                            text = "No rituals created yet. Add rituals to see consistency analytics.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppFont),
                            color = StoneGray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        habits.forEachIndexed { index, habit ->
                            val rate30 = habit.calculateCompletionRate(30, today)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(primaryColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getHabitIcon(habit.iconName),
                                            contentDescription = habit.name,
                                            tint = primaryColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = habit.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = AppFont
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "${(rate30 * 100).toInt()}% consistency (30d)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppFont),
                                            color = StoneGray
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = PhosphorIcons.Bold.Fire,
                                            contentDescription = "Streak",
                                            tint = Color(0xFFFFA726),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${habit.streak}d",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = AppFont
                                            ),
                                            color = Color(0xFFFFA726)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { rate30 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = primaryColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )

                                if (index < habits.lastIndex) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 2: FOCUS TASKS PROGRESS ---
            Text(
                text = "TASK COMPLETION METRICS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = AppFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = StoneGray
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Overall Tasks",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFont
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${(taskCompletionRate * 100).toInt()}% Done",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFont
                            ),
                            color = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { taskCompletionRate },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = primaryColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatLabel(label = "Completed", count = completedTasks, color = primaryColor)
                        StatLabel(label = "Pending", count = pendingTasks, color = Color(0xFFFFA726))
                        StatLabel(label = "Total", count = totalTasks, color = MaterialTheme.colorScheme.onBackground)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Priority Breakdown
                    Text(
                        text = "Priority Breakdown",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFont
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PriorityBar(label = "High", count = highPriorityCount, total = totalTasks, color = Color(0xFFE57373), modifier = Modifier.weight(1f))
                        PriorityBar(label = "Medium", count = mediumPriorityCount, total = totalTasks, color = Color(0xFFFFB74D), modifier = Modifier.weight(1f))
                        PriorityBar(label = "Low", count = lowPriorityCount, total = totalTasks, color = Color(0xFF81C784), modifier = Modifier.weight(1f))
                    }
                }
            }

            // --- SECTION 3: SYSTEM OVERVIEW ---
            Text(
                text = "KOMORI OVERVIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = AppFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = StoneGray
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OverviewPill(
                    icon = PhosphorIcons.Bold.CalendarCheck,
                    label = "Events",
                    value = "${weeklyEvents.size}",
                    color = primaryColor,
                    modifier = Modifier.weight(1f)
                )

                OverviewPill(
                    icon = PhosphorIcons.Bold.Note,
                    label = "Notes",
                    value = "${notes.size}",
                    color = primaryColor,
                    modifier = Modifier.weight(1f)
                )

                OverviewPill(
                    icon = PhosphorIcons.Bold.CheckCircle,
                    label = "Completed",
                    value = "$completedTasks",
                    color = primaryColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun AnalyticsStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFont
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppFont),
                color = StoneGray
            )
        }
    }
}

@Composable
private fun StatLabel(label: String, count: Int, color: Color) {
    Column {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = AppFont
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppFont),
            color = StoneGray
        )
    }
}

@Composable
private fun PriorityBar(label: String, count: Int, total: Int, color: Color, modifier: Modifier = Modifier) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AppFont
                    ),
                    color = color
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFont
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun OverviewPill(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppFont),
                color = StoneGray
            )
        }
    }
}
