package com.rolando.komori.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rolando.komori.KomoriApplication
import com.rolando.komori.data.Task
import com.rolando.komori.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Change: Takes 'TaskRepository' instead of 'TaskDao'
class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // 1. Cache the data in a StateFlow (Hot Observable)
    // stateIn makes this efficient: it holds the last value in memory for the UI
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. Add Task (Through Repository)
    fun addTask(title: String, details: String, date: Long?, time: String?) {
        val newTask = Task(
            title = title,
            details = details,
            dueDate = date,
            dueTime = time,
            isCompleted = false
        )
        viewModelScope.launch {
            repository.insertTask(newTask)
        }
    }

    // 3. Toggle Complete (Through Repository)
    fun toggleTask(task: Task, isChecked: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = isChecked))
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    // In TaskViewModel.kt
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // 4. Factory Update
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Get the Application singleton
                val application = (this[APPLICATION_KEY] as KomoriApplication)
                // Pass the Repository we created in Step 2
                TaskViewModel(application.repository)
            }
        }
    }
}