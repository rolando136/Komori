package com.rolando.komori.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.ui.graphics.Color // Ensure you have this if using Color in Enum
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

// 1. Define Priority Enum
enum class TaskPriority(val label: String, val color: Long) {
    // NOTE: Room can't store "Color" object directly. Store the color as a Long (ARGB) or String.
    // I changed Color to Long here for easier database storage.
    LOW("Low", 0xFF81C784),
    MEDIUM("Medium", 0xFFFFB74D),
    HIGH("High", 0xFFE57373);
}

// 2. Define Subtask Data Class
data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

// 2b. Define Task Repeat Enum
enum class TaskRepeat(val label: String, val intervalDays: Int = 0) {
    NONE("None", 0),
    DAILY("Daily", 1),
    EVERY_OTHER_DAY("Every 2 days", 2),
    EVERY_3_DAYS("Every 3 days", 3),
    WEEKLY("Weekly", 7),
    BIWEEKLY("Biweekly", 14),
    MONTHLY("Monthly", 30);

    companion object {
        fun fromInterval(days: Int): TaskRepeat {
            return entries.firstOrNull { it.intervalDays == days && it != NONE } ?: NONE
        }
    }
}

// 3. Main Task Entity
@Entity(tableName = "tasks") // <--- REQUIRED: Tells Room this is a table
data class Task(
    @PrimaryKey // <--- REQUIRED: Unique ID for the row
    val id: String = UUID.randomUUID().toString(),

    val title: String,
    val details: String,
    val date: LocalDate?,
    val time: LocalTime?,
    val isCompleted: Boolean = false,

    // Room needs TypeConverters for these:
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val subtasks: List<Subtask> = emptyList(),
    val repeat: TaskRepeat = TaskRepeat.NONE,
    val lastCompletedDate: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun isDueOn(today: LocalDate = LocalDate.now()): Boolean {
        if (repeat == TaskRepeat.NONE) {
            // Non-repeating task: if date is set, it is due on that date (or overdue before today)
            // If date is null, it's always active
            return date == null || date <= today
        }

        val anchor = date ?: java.time.Instant.ofEpochMilli(createdAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        if (today.isBefore(anchor)) return false

        if (repeat == TaskRepeat.DAILY) {
            return true
        }

        if (repeat == TaskRepeat.MONTHLY) {
            if (lastCompletedDate != null) {
                val lastDate = try { LocalDate.parse(lastCompletedDate) } catch (e: Exception) { null }
                if (lastDate != null) {
                    if (lastDate == today) return true
                    if (lastDate.year == today.year && lastDate.monthValue == today.monthValue) {
                        return false
                    }
                }
            }
            val effectiveDay = anchor.dayOfMonth.coerceAtMost(today.lengthOfMonth())
            return today.dayOfMonth >= effectiveDay
        }

        // Interval-based repeating (every 2 days, every 3 days, weekly, biweekly)
        val intervalDays = repeat.intervalDays
        if (lastCompletedDate != null) {
            val lastDate = try { LocalDate.parse(lastCompletedDate) } catch (e: Exception) { null }
            if (lastDate != null) {
                if (lastDate == today) {
                    return true // Completed today, so it shows in completed tasks
                }
                val daysSinceLast = java.time.temporal.ChronoUnit.DAYS.between(lastDate, today)
                return daysSinceLast >= intervalDays
            }
        }

        return !today.isBefore(anchor)
    }

    /**
     * Checks if this task should be displayed on a calendar for a specific date.
     * Unlike isDueOn (which handles overdue/active filtering for today),
     * isOnCalendarDate determines if the specific date is a scheduled occurrence.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun isOnCalendarDate(targetDate: LocalDate): Boolean {
        val anchor = date ?: java.time.Instant.ofEpochMilli(createdAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        if (targetDate.isBefore(anchor)) return false

        return when (repeat) {
            TaskRepeat.NONE -> date != null && targetDate.isEqual(date)
            TaskRepeat.DAILY -> true
            TaskRepeat.MONTHLY -> {
                val effectiveDay = anchor.dayOfMonth.coerceAtMost(targetDate.lengthOfMonth())
                targetDate.dayOfMonth == effectiveDay
            }
            else -> {
                val intervalDays = repeat.intervalDays
                if (intervalDays > 0) {
                    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(anchor, targetDate)
                    daysBetween >= 0 && (daysBetween % intervalDays.toLong() == 0L)
                } else {
                    date != null && targetDate.isEqual(date)
                }
            }
        }
    }
}

