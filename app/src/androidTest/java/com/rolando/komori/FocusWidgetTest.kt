package com.rolando.komori

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
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
import com.rolando.komori.widget.WidgetStyle
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

    @Test fun widgetRendersCardsAndAlwaysShowsEditButton() {
        instrumentation.runOnMainSync {
            val tasks = (1..4).map { task("Focus task $it") }
            val view = FocusWidgetProvider.createViews(context, tasks).apply(context, FrameLayout(context))
            assertEquals(3, view.findViewById<LinearLayout>(R.id.widget_tasks).childCount)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.widget_edit).visibility)
            assertTrue(view.findViewById<TextView>(R.id.widget_header).typeface.isBold)
            assertEquals(View.GONE, view.findViewById<View>(R.id.widget_empty).visibility)
            val foreground = context.getColor(R.color.widget_foreground)
            val surface = context.getColor(R.color.widget_surface)
            assertTrue(ColorUtils.calculateContrast(foreground, surface) >= 4.5)
            val empty = FocusWidgetProvider.createViews(context, emptyList()).apply(context, FrameLayout(context))
            assertEquals(View.VISIBLE, empty.findViewById<View>(R.id.widget_empty).visibility)
            assertEquals(View.VISIBLE, empty.findViewById<View>(R.id.widget_edit).visibility)
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

    @Test fun appearanceIsSavedPerWidgetAndRestoresToNewIds() = runBlocking {
        val ids = intArrayOf(90001, 90002, 90003)
        try {
            val first = WidgetStyle(Color.rgb(45, 65, 85), 0.42f)
            val second = WidgetStyle(Color.WHITE, 1f)
            WidgetAppearance.save(context, ids[0], first)
            WidgetAppearance.save(context, ids[1], second)
            assertEquals(first, WidgetAppearance.read(context, ids[0]))
            assertEquals(second, WidgetAppearance.read(context, ids[1]))
            WidgetAppearance.restore(context, intArrayOf(ids[0]), intArrayOf(ids[2]))
            assertEquals(first, WidgetAppearance.read(context, ids[2]))
        } finally { WidgetAppearance.delete(context, ids) }
    }

    @Test fun manualColorAndOpacityRenderWithoutDimmingText() {
        instrumentation.runOnMainSync {
            for (color in listOf(Color.BLACK, Color.WHITE, Color.RED, Color.GRAY)) {
                for (opacity in listOf(0f, 0.5f, 1f)) {
                    val style = WidgetStyle(color, opacity)
                    val view = FocusWidgetProvider.createViews(context, listOf(task("Custom task")), style = style)
                        .apply(context, FrameLayout(context))
                    val text = view.findViewById<TextView>(R.id.widget_header)
                    assertEquals(style.foreground, text.currentTextColor)
                    assertEquals(255, Color.alpha(text.currentTextColor))
                    assertTrue(ColorUtils.calculateContrast(style.foreground, style.color) >= 4.5)
                    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                    val background = view.findViewById<ImageView>(R.id.widget_background_image).drawable
                    background.setBounds(0, 0, 100, 100)
                    background.draw(Canvas(bitmap))
                    assertEquals((opacity * 255).toInt().toDouble(), Color.alpha(bitmap.getPixel(50, 50)).toDouble(), 1.0)
                    assertEquals(bitmap.getPixel(50, 35), bitmap.getPixel(50, 65))
                    bitmap.recycle()
                }
            }
        }
    }

    @Test fun roundedSurfacesKeepTheirRadiusAndHaveNoOutlineAfterResize() {
        instrumentation.runOnMainSync {
            val style = WidgetStyle(Color.rgb(40, 65, 90), 0.5f)
            val view = FocusWidgetProvider.createViews(context, listOf(task("Rounded task")), style = style)
                .apply(context, FrameLayout(context))
            for ((width, height) in listOf(700 to 900, 1200 to 600)) {
                view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, width, height)
                for ((id, radiusId) in listOf(R.id.widget_background_image to R.dimen.widget_corner_radius,
                    R.id.widget_card_image to R.dimen.widget_card_corner_radius)) {
                    val image = view.findViewById<ImageView>(id)
                    val shape = image.drawable as GradientDrawable
                    assertEquals(context.resources.getDimension(radiusId), shape.cornerRadius, 0.01f)
                    val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                    shape.setBounds(0, 0, image.width, image.height)
                    shape.draw(Canvas(bitmap))
                    val center = bitmap.getPixel(image.width / 2, image.height / 2)
                    assertEquals("No outline along straight edges", center, bitmap.getPixel(1, image.height / 2))
                    assertEquals("Rounded corners must be transparent", 0, Color.alpha(bitmap.getPixel(0, 0)))
                    assertEquals(127.0, Color.alpha(center).toDouble(), 1.0)
                    bitmap.recycle()
                }
            }
        }
    }
}
