package com.rolando.komori.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rolando.komori.KomoriApplication
import com.rolando.komori.data.Subtask
import com.rolando.komori.data.Task
import com.rolando.komori.data.TaskPriority
import com.rolando.komori.data.TaskRepository
import com.rolando.komori.data.WeeklyEvent
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // --- TASKS ---
    val allTasks: StateFlow<List<Task>?> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun addTask(
        title: String,
        details: String,
        date: LocalDate?,
        time: LocalTime?,
        priority: TaskPriority,
        subtasks: List<Subtask>
    ) {
        val newTask = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            details = details,
            date = date,
            time = time,
            priority = priority,
            subtasks = subtasks
        )

        // FIX: Save to Repository (Database) instead of local list
        viewModelScope.launch {
            repository.insertTask(newTask)
        }
    }

    fun toggleTask(task: Task, isChecked: Boolean) {
        viewModelScope.launch { repository.updateTask(task.copy(isCompleted = isChecked)) }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { repository.updateTask(task) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    // --- SCHEDULE / WEEKLY EVENTS (NEW) ---
    val scheduleEvents: StateFlow<List<WeeklyEvent>> = repository.allScheduleEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addScheduleEvent(
        title: String,
        description: String,
        category: String,
        startDate: LocalDate, // Changed from DayOfWeek
        recurrence: String,   // New Param
        start: LocalTime,
        end: LocalTime,
        color: Int
    ) {
        val newEvent = WeeklyEvent(
            title = title,
            description = description,
            category = category,
            startDate = startDate,
            recurrence = recurrence,
            startTime = start,
            endTime = end,
            colorArgb = color
        )
        viewModelScope.launch { repository.insertScheduleEvent(newEvent) }
    }

    // Update logic remains similar, just ensure it passes the full object
    fun updateScheduleEvent(event: WeeklyEvent) {
        viewModelScope.launch { repository.insertScheduleEvent(event) }
    }

    fun deleteScheduleEvent(event: WeeklyEvent) {
        viewModelScope.launch {
            repository.deleteScheduleEvent(event)
        }
    }


    // Factory remains the same because 'repository' in Application was already updated
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as KomoriApplication)
                TaskViewModel(application.repository)
            }
        }
    }
}