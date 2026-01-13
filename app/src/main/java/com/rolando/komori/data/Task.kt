
// In data/Task.kt
package com.rolando.komori.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// In data/Task.kt
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val details: String = "",
    val dueDate: Long? = null,
    val dueTime: String? = null, // <--- NEW FIELD
    val isCompleted: Boolean = false,
    val category: String = "General"
)