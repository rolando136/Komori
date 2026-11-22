package com.rolando.komori

import android.app.Application
import android.app.WallpaperManager
import android.os.Build
import android.os.Handler
import android.util.Log
import com.rolando.komori.data.KomoriDatabase
import com.rolando.komori.data.TaskRepository
import com.rolando.komori.widget.FocusWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class KomoriApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                WallpaperManager.getInstance(this).addOnColorsChangedListener({ _, which ->
                    if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                        applicationScope.launch {
                            FocusWidgetProvider.update(this@KomoriApplication, repository.allTasks.first())
                        }
                    }
                }, Handler(mainLooper))
            } catch (e: SecurityException) {
                Log.w("FocusWidget", "Wallpaper color notifications unavailable", e)
            }
        }
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
