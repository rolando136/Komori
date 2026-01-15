package com.rolando.komori.data

import kotlinx.coroutines.flow.Flow

// Update constructor to take BOTH DAOs
class TaskRepository(
    private val taskDao: TaskDao,
    private val weeklyEventDao: WeeklyEventDao
) {

    // --- TASKS ---
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    // --- SCHEDULE EVENTS ---
    val allScheduleEvents: Flow<List<WeeklyEvent>> = weeklyEventDao.getAllEvents()

    suspend fun insertScheduleEvent(event: WeeklyEvent) {
        weeklyEventDao.insertEvent(event)
    }

    suspend fun deleteScheduleEvent(event: WeeklyEvent) {
        weeklyEventDao.deleteEvent(event)
    }
}