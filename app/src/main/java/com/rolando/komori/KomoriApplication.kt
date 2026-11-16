package com.rolando.komori

import android.app.Application
import com.rolando.komori.data.KomoriDatabase
import com.rolando.komori.data.TaskRepository

class KomoriApplication : Application() {
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
