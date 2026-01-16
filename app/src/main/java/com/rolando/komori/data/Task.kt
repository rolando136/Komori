package com.rolando.komori.data

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

    // Room needs TypeConverters for these two:
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val subtasks: List<Subtask> = emptyList()
)

