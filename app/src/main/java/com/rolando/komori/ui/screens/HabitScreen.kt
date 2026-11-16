package com.rolando.komori.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.*
import com.rolando.komori.data.Habit
import com.rolando.komori.ui.components.KomoriDialog
import com.rolando.komori.ui.theme.AppFont
import java.time.LocalDate

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
    onDayToggle: (Habit, Int) -> Unit,
    onHabitClick: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

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
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(habits, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    onDayToggle = { dayIndex -> onDayToggle(habit, dayIndex) },
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
    onDayToggle: (Int) -> Unit,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val todayIndex = LocalDate.now().dayOfWeek.value - 1 // 0 = Mon, 6 = Sun

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = surfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // --- TOP ROW: Icon + Name + Streak ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Habit Icon Container
                Box(
                    modifier = Modifier
                        .size(44.dp)
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

                // Habit Title
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFont
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Fire Icon & Streak Count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.Fire,
                        contentDescription = "Streak",
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${habit.streak}d",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = tertiaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- BOTTOM ROW: 7 Days of the Week ---
            val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEachIndexed { index, dayLabel ->
                    val isCompleted = habit.isDayCompletedInCurrentWeek(index)
                    val isToday = index == todayIndex

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = if (isToday) primaryColor else tertiaryColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // Rounded Square Checkbox
                        val checkboxShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(checkboxShape)
                                .background(if (isCompleted) primaryColor else Color.Transparent)
                                .border(
                                    width = if (isToday && !isCompleted) 1.5.dp else 1.dp,
                                    color = if (isCompleted) primaryColor else if (isToday) primaryColor.copy(alpha = 0.6f) else outlineColor,
                                    shape = checkboxShape
                                )
                                .clickable { onDayToggle(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = PhosphorIcons.Bold.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String) -> Unit
) {
    var habitName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("FlowerLotus") }
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
                onSave(habitName.trim(), selectedIcon)
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

            Text(
                text = "CHOOSE ICON",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = tertiaryColor.copy(alpha = 0.7f),
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
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedIcon = name },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = if (isSelected) Color.White else primaryColor,
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
    onDelete: () -> Unit,
    onSave: (Habit) -> Unit
) {
    var editedName by remember { mutableStateOf(habit.name) }
    var selectedIcon by remember { mutableStateOf(habit.iconName) }
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
                onSave(habit.copy(name = editedName.trim(), iconName = selectedIcon))
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

            Text(
                text = "CHOOSE ICON",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = tertiaryColor.copy(alpha = 0.7f),
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
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedIcon = name },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = if (isSelected) Color.White else primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
