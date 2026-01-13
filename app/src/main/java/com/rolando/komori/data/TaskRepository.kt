package com.rolando.komori.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    // This Flow acts as your "Hot Cache".
    // It emits new data immediately whenever the database changes.
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    // Write operations
    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
}