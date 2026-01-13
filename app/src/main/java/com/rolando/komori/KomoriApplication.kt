package com.rolando.komori

import android.app.Application
import com.rolando.komori.data.KomoriDatabase
import com.rolando.komori.data.TaskRepository

class KomoriApplication : Application() {
    // 1. Create the Database instance
    val database by lazy { KomoriDatabase.getDatabase(this) }

    // 2. Create the Repository (injecting the database DAO)
    val repository by lazy { TaskRepository(database.taskDao()) }
}