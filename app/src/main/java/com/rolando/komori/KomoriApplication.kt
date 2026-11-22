package com.rolando.komori

import android.app.Application
import com.rolando.komori.data.KomoriDatabase
import com.rolando.komori.data.TaskRepository
import com.rolando.komori.widget.FocusWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KomoriApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.allTasks.collect { tasks -> FocusWidgetProvider.update(this@KomoriApplication, tasks) }
        }
    }

    val database by lazy { KomoriDatabase.getDatabase(this) }

    val repository by lazy {
        TaskRepository(
            database.taskDao(),
            database.weeklyEventDao(),
            database.habitDao(),
            database.noteDao()
        )
    }
}
