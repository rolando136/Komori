package com.rolando.komori

import android.content.BroadcastReceiver
import android.app.WallpaperColors
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.rolando.komori.data.Subtask
import com.rolando.komori.data.Task
import com.rolando.komori.data.TaskRepeat
import com.rolando.komori.widget.FocusWidgetProvider
import com.rolando.komori.widget.WidgetAppearance
import com.rolando.komori.widget.WidgetTheme
import com.rolando.komori.widget.WidgetWallpaper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class FocusWidgetTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val repository = (context.applicationContext as KomoriApplication).repository

    private fun task(title: String, repeat: TaskRepeat = TaskRepeat.NONE) = Task(
        title = title, details = "", date = LocalDate.now(), time = null, repeat = repeat
    )

    private fun sendCompletion(task: Task, date: LocalDate = LocalDate.now()) {
        val finished = CountDownLatch(1)
        val intent = Intent(context, FocusWidgetProvider::class.java).apply {
            action = FocusWidgetProvider.ACTION_COMPLETE
            putExtra(FocusWidgetProvider.EXTRA_TASK_ID, task.id)
            putExtra(FocusWidgetProvider.EXTRA_DATE, date.toString())
        }
        context.sendOrderedBroadcast(intent, null, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { finished.countDown() }
        }, null, 0, null, null)
        assertTrue("Widget receiver should finish its database work", finished.await(10, TimeUnit.SECONDS))
    }

    @Test fun checkboxCompletesTaskAndUpdatesDatabase() = runBlocking {
        val task = task("Widget checkbox test")
        repository.insertTask(task)
        try {
            instrumentation.runOnMainSync {
                val view = FocusWidgetProvider.createViews(context, listOf(task)).apply(context, FrameLayout(context))
                assertTrue(view.findViewById<ImageButton>(R.id.widget_task_check).performClick())
            }
            val saved = withTimeout(10_000) {
                repository.allTasks.first { tasks -> tasks.any { it.id == task.id && it.isCompleted } }
            }.first { it.id == task.id }
            assertTrue(saved.isCompleted)
            assertNull(saved.lastCompletedDate)
        } finally { repository.deleteTask(task) }
    }

    @Test fun repeatingCompletionIsIdempotentAndStampedForToday() = runBlocking {
        val task = task("Widget recurring test", TaskRepeat.DAILY)
        repository.insertTask(task)
        try {
            sendCompletion(task)
            sendCompletion(task)
            val saved = repository.allTasks.first().first { it.id == task.id }
            assertTrue(saved.isCompleted)
            assertEquals(LocalDate.now().toString(), saved.lastCompletedDate)
        } finally { repository.deleteTask(task) }
    }

    @Test fun unfinishedSubtasksAndStaleTapsCannotCompleteTasks() = runBlocking {
        val blocked = task("Widget blocked test").copy(subtasks = listOf(Subtask(title = "Not done")))
        val stale = task("Widget stale test", TaskRepeat.DAILY)
        repository.insertTask(blocked)
        repository.insertTask(stale)
        try {
            sendCompletion(blocked)
            sendCompletion(stale, LocalDate.now().minusDays(1))
            val saved = repository.allTasks.first()
            assertFalse(saved.first { it.id == blocked.id }.isCompleted)
            assertFalse(saved.first { it.id == stale.id }.isCompleted)
        } finally {
            repository.deleteTask(blocked)
            repository.deleteTask(stale)
        }
    }

    @Test fun widgetRendersCardsAndAccessibleWallpaperColors() {
        instrumentation.runOnMainSync {
            val tasks = (1..4).map { task("Focus task $it") }
            val view = FocusWidgetProvider.createViews(context, tasks).apply(context, FrameLayout(context))
            assertEquals(3, view.findViewById<LinearLayout>(R.id.widget_tasks).childCount)
            assertEquals("4", view.findViewById<TextView>(R.id.widget_summary).text.toString())
            assertEquals(View.GONE, view.findViewById<View>(R.id.widget_empty).visibility)
            val foreground = context.getColor(R.color.widget_foreground)
            val surface = context.getColor(R.color.widget_surface)
            assertTrue(ColorUtils.calculateContrast(foreground, surface) >= 4.5)
            val empty = FocusWidgetProvider.createViews(context, emptyList()).apply(context, FrameLayout(context))
            assertEquals(View.VISIBLE, empty.findViewById<View>(R.id.widget_empty).visibility)
            assertEquals(View.GONE, empty.findViewById<View>(R.id.widget_summary).visibility)
            assertEquals(View.GONE, empty.findViewById<View>(R.id.widget_tasks).visibility)
            val message = empty.findViewById<TextView>(R.id.widget_empty)
            assertNull(message.background)
            assertEquals(Gravity.CENTER, message.gravity)
            empty.measure(View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY))
            empty.layout(0, 0, 900, 900)
            assertEquals(empty.findViewById<View>(R.id.widget_content).height, message.height)
        }
    }

    @Test fun glassThemePersistsRendersTranslucentlyAndKeepsTitleBold() = runBlocking {
        val previous = WidgetAppearance.read(context)
        try {
            WidgetAppearance.save(context, WidgetTheme.Glass)
            assertEquals(WidgetTheme.Glass, WidgetAppearance.read(context))
            instrumentation.runOnMainSync {
                val view = FocusWidgetProvider.createViews(context, listOf(task("Glass task")))
                    .apply(context, FrameLayout(context))
                assertTrue(view.findViewById<TextView>(R.id.widget_focus_title).typeface.isBold)
                assertNotNull(view.findViewById<ImageButton>(R.id.widget_task_check))
                val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                val background = view.background
                background.setBounds(0, 0, 100, 100)
                background.draw(Canvas(bitmap))
                val alpha = Color.alpha(bitmap.getPixel(50, 50))
                assertTrue("Glass background should stay translucent", alpha in 1..254)
                bitmap.recycle()
            }
            WidgetAppearance.save(context, WidgetTheme.Solid)
            assertEquals(WidgetTheme.Solid, WidgetAppearance.read(context))
        } finally { WidgetAppearance.save(context, previous) }
    }

    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun wallpaperBrightnessChoosesDarkOrLight() {
        assertTrue(WidgetWallpaper.isDark(WallpaperColors(Color.valueOf(Color.BLACK), null, null)))
        assertFalse(WidgetWallpaper.isDark(WallpaperColors(Color.valueOf(Color.WHITE), null, null)))
    }

    @Test fun widgetPaletteFollowsWallpaperInsteadOfSystemAppearance() = runBlocking {
        val previous = WidgetAppearance.read(context)
        try {
            for (theme in WidgetTheme.entries) {
                WidgetAppearance.save(context, theme)
                instrumentation.runOnMainSync {
                    for (dark in listOf(true, false)) {
                        val view = FocusWidgetProvider.createViews(context, listOf(task("Adaptive task")), darkWallpaper = dark)
                            .apply(context, FrameLayout(context))
                        val text = view.findViewById<TextView>(R.id.widget_focus_title)
                        assertEquals(context.getColor(if (dark) R.color.widget_auto_dark_text else R.color.widget_auto_light_text),
                            text.currentTextColor)
                        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                        view.background.setBounds(0, 0, 100, 100)
                        view.background.draw(Canvas(bitmap))
                        assertEquals("Background should be flat, not a gradient",
                            bitmap.getPixel(50, 35), bitmap.getPixel(50, 65))
                        val opaque = ColorUtils.setAlphaComponent(bitmap.getPixel(50, 50), 255)
                        assertEquals(dark, ColorUtils.calculateLuminance(opaque) < 0.45)
                        assertTrue(ColorUtils.calculateContrast(text.currentTextColor, opaque) >= 4.5)
                        bitmap.recycle()
                    }
                }
            }
        } finally { WidgetAppearance.save(context, previous) }
    }
}
