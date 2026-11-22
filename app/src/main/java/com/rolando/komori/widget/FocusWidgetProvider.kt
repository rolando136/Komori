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
import androidx.core.graphics.ColorUtils
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

    override fun onDeleted(context: Context, ids: IntArray) = WidgetAppearance.delete(context, ids)

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        WidgetAppearance.restore(context, oldWidgetIds, newWidgetIds)
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
                val options = manager.getAppWidgetOptions(widgetId)
                val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 280).coerceAtLeast(140)
                val rowCount = ((height - 112) / 64).coerceIn(1, 3)
                manager.updateAppWidget(widgetId, createViews(context, tasks, rowCount, widgetId))
            }
        }

        internal fun createViews(context: Context, tasks: List<Task>, rowCount: Int = 3,
            widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
            style: WidgetStyle = WidgetAppearance.read(context, widgetId)): RemoteViews {
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
            val foreground = style.foreground
            val secondary = ColorUtils.blendARGB(foreground, style.color, 0.18f)
            setSurface(views, R.id.widget_background_image, R.drawable.widget_plain_background, style.color, style.opacity)
            views.setTextColor(R.id.widget_header, foreground)
            views.setTextColor(R.id.widget_date, secondary)
            views.setTextColor(R.id.widget_empty, secondary)
            val checkbox = requireNotNull(context.getDrawable(R.drawable.widget_checkbox)).mutate()
            checkbox.setTint(foreground)
            val checkboxSize = (22 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            val checkboxBitmap = checkbox.toBitmap(checkboxSize, checkboxSize)
            val editIcon = requireNotNull(context.getDrawable(R.drawable.widget_edit_icon)).mutate()
            editIcon.setTint(foreground)
            views.setImageViewBitmap(R.id.widget_edit, editIcon.toBitmap(checkboxSize, checkboxSize))
            val edit = Intent(context, WidgetEditorActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = Uri.parse("komori://widget/$widgetId/edit")
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            views.setOnClickPendingIntent(R.id.widget_edit, PendingIntent.getActivity(context, widgetId, edit,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            views.setTextViewText(R.id.widget_date, today?.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                ?: context.getString(R.string.app_name))
            val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.removeAllViews(R.id.widget_tasks)
            active.take(rowCount).forEach { task ->
                val row = RemoteViews(context.packageName, R.layout.widget_task_row)
                setSurface(row, R.id.widget_card_image, R.drawable.widget_plain_card, style.surface, style.opacity)
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
            views.setOnClickPendingIntent(R.id.widget_header, openApp)
            views.setOnClickPendingIntent(R.id.widget_empty, openApp)
            return views
        }

        private fun setSurface(views: RemoteViews, id: Int, drawable: Int, color: Int, opacity: Float) {
            // Native shapes retain a circular dp radius at any launcher size; bitmaps stretch it.
            views.setImageViewResource(id, drawable)
            views.setInt(id, "setColorFilter", color)
            views.setInt(id, "setImageAlpha", (opacity * 255).toInt())
        }
    }
}
