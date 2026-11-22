package com.rolando.komori.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import com.rolando.komori.KomoriApplication
import com.rolando.komori.MainActivity
import com.rolando.komori.R
import com.rolando.komori.data.Task
import com.rolando.komori.data.TaskRepeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FocusWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_WALLPAPER_CHANGED) {
            val manager = AppWidgetManager.getInstance(context)
            onUpdate(context, manager, manager.getAppWidgetIds(ComponentName(context, FocusWidgetProvider::class.java)))
            return
        }
        if (intent.action != ACTION_COMPLETE) {
            super.onReceive(context, intent)
            return
        }
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val renderedDate = intent.getStringExtra(EXTRA_DATE) ?: return
        val pendingResult = goAsync()
        scope.launch {
            try {
                completionMutex.withLock {
                    val repository = (context.applicationContext as KomoriApplication).repository
                    val task = repository.allTasks.first().firstOrNull { it.id == taskId }
                    // A stale tap must not complete tomorrow's repeating occurrence.
                    val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDate.now() else null
                    if (task != null && renderedDate == dateKey() && (today == null || task.isDueOn(today))) {
                        val alreadyCompleted = task.isCompleted &&
                            (task.repeat == TaskRepeat.NONE || task.lastCompletedDate == renderedDate)
                        if (!alreadyCompleted) {
                            if (task.subtasks.any { !it.isCompleted }) {
                                showMessage(context, R.string.widget_subtasks_required)
                            } else {
                                repository.updateTask(task.copy(isCompleted = true,
                                    lastCompletedDate = if (task.repeat != TaskRepeat.NONE) renderedDate else null))
                                showMessage(context, R.string.widget_completed)
                            }
                        }
                    }
                    update(context, repository.allTasks.first())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("FocusWidget", "Unable to complete widget task", e)
                showMessage(context, R.string.widget_update_failed)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        onUpdate(context, manager, intArrayOf(id))
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                val tasks = (context.applicationContext as KomoriApplication).repository.allTasks.first()
                update(context, tasks, ids)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("FocusWidget", "Unable to refresh widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.rolando.komori.widget.COMPLETE_TASK"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_DATE = "date"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val completionMutex = Mutex()

        private fun dateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())

        private suspend fun showMessage(context: Context, message: Int) = withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        fun update(context: Context, tasks: List<Task>, ids: IntArray = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, FocusWidgetProvider::class.java))) {
            if (ids.isEmpty()) return
            val manager = AppWidgetManager.getInstance(context)
            ids.forEach { widgetId ->
                val height = manager.getAppWidgetOptions(widgetId)
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 280)
                val rowCount = ((height - 112) / 56).coerceIn(1, 3)
                manager.updateAppWidget(widgetId, createViews(context, tasks, rowCount))
            }
        }

        internal fun createViews(context: Context, tasks: List<Task>, rowCount: Int = 3,
            darkWallpaper: Boolean = WidgetWallpaper.isDark(context)): RemoteViews {
            val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDate.now() else null
            val renderedDate = dateKey()
            val active = tasks.filter { task ->
                if (today == null) !task.isCompleted else {
                    val completedToday = task.isCompleted &&
                        (task.repeat == TaskRepeat.NONE || task.lastCompletedDate == today.toString())
                    !completedToday && task.isDueOn(today)
                }
            }.sortedWith(compareBy<Task> { it.priority.ordinal * -1 }.thenBy(nullsLast()) { it.time })
            val views = RemoteViews(context.packageName, R.layout.widget_focus)
            val glass = WidgetAppearance.read(context) == WidgetTheme.Glass
            val background = when {
                glass && darkWallpaper -> R.drawable.widget_auto_glass_dark
                glass -> R.drawable.widget_auto_glass_light
                darkWallpaper -> R.drawable.widget_auto_solid_dark
                else -> R.drawable.widget_auto_solid_light
            }
            val card = when {
                glass && darkWallpaper -> R.drawable.widget_auto_card_glass_dark
                glass -> R.drawable.widget_auto_card_glass_light
                darkWallpaper -> R.drawable.widget_auto_card_dark
                else -> R.drawable.widget_auto_card_light
            }
            val foreground = context.getColor(if (darkWallpaper) R.color.widget_auto_dark_text else R.color.widget_auto_light_text)
            val secondary = context.getColor(if (darkWallpaper) R.color.widget_auto_dark_secondary else R.color.widget_auto_light_secondary)
            val accent = context.getColor(if (darkWallpaper) R.color.widget_auto_dark_accent else R.color.widget_auto_light_accent)
            views.setInt(R.id.widget_root, "setBackgroundResource", background)
            views.setInt(R.id.widget_summary, "setBackgroundResource", card)
            views.setTextColor(R.id.widget_focus_title, foreground)
            views.setTextColor(R.id.widget_summary, accent)
            views.setTextColor(R.id.widget_date, secondary)
            views.setTextColor(R.id.widget_empty, secondary)
            val checkbox = requireNotNull(context.getDrawable(R.drawable.widget_checkbox)).mutate()
            checkbox.setTint(accent)
            val checkboxSize = (22 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            val checkboxBitmap = checkbox.toBitmap(checkboxSize, checkboxSize)
            views.setTextViewText(R.id.widget_date, today?.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                ?: context.getString(R.string.app_name))
            val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setTextViewText(R.id.widget_summary, active.size.toString())
            views.setContentDescription(R.id.widget_summary, context.resources.getQuantityString(
                R.plurals.widget_task_count, active.size, active.size))
            views.removeAllViews(R.id.widget_tasks)
            active.take(rowCount).forEach { task ->
                val row = RemoteViews(context.packageName, R.layout.widget_task_row)
                row.setInt(R.id.widget_task_card, "setBackgroundResource", card)
                row.setTextColor(R.id.widget_task_title, foreground)
                row.setTextColor(R.id.widget_task_detail, secondary)
                row.setImageViewBitmap(R.id.widget_task_check, checkboxBitmap)
                row.setTextViewText(R.id.widget_task_title, task.title)
                val details = buildList {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        task.time?.let { add(it.format(DateTimeFormatter.ofPattern("h:mm a"))) }
                    }
                    if (task.subtasks.isNotEmpty()) {
                        add(context.getString(R.string.widget_subtask_detail,
                            task.subtasks.count { it.isCompleted }, task.subtasks.size))
                    } else {
                        add(context.getString(R.string.widget_task_detail, task.priority.label))
                    }
                }.joinToString(" · ")
                row.setTextViewText(R.id.widget_task_detail, details)
                row.setContentDescription(R.id.widget_task_check, context.getString(R.string.widget_complete_named, task.title))
                val complete = Intent(context, FocusWidgetProvider::class.java).apply {
                    action = ACTION_COMPLETE
                    data = Uri.Builder().scheme("komori").authority("complete")
                        .appendPath(task.id).appendPath(renderedDate).build()
                    putExtra(EXTRA_TASK_ID, task.id)
                    putExtra(EXTRA_DATE, renderedDate)
                }
                row.setOnClickPendingIntent(R.id.widget_task_check, PendingIntent.getBroadcast(context, 0, complete,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                row.setOnClickPendingIntent(R.id.widget_task_content, openApp)
                views.addView(R.id.widget_tasks, row)
            }
            views.setViewVisibility(R.id.widget_empty, if (active.isEmpty()) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_tasks, if (active.isEmpty()) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_summary, if (active.isEmpty()) View.GONE else View.VISIBLE)
            views.setOnClickPendingIntent(R.id.widget_header, openApp)
            views.setOnClickPendingIntent(R.id.widget_empty, openApp)
            return views
        }
    }
}
