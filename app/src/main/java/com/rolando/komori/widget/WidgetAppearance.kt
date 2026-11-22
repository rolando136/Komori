package com.rolando.komori.widget

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.rolando.komori.KomoriApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class WidgetStyle(val color: Int = Color.rgb(237, 242, 233), val opacity: Float = 1f) {
    val dark: Boolean get() = ColorUtils.calculateContrast(Color.WHITE, color) > ColorUtils.calculateContrast(Color.BLACK, color)
    val foreground: Int get() {
        val soft = if (dark) Color.rgb(246, 248, 243) else Color.rgb(25, 30, 24)
        return if (ColorUtils.calculateContrast(soft, color) >= 4.5) soft else if (dark) Color.WHITE else Color.BLACK
    }
    val surface: Int get() = ColorUtils.blendARGB(color, if (dark) Color.WHITE else Color.BLACK, 0.06f)
}

object WidgetAppearance {
    private fun prefs(context: Context) = context.getSharedPreferences("komori_prefs", Context.MODE_PRIVATE)

    fun read(context: Context, widgetId: Int): WidgetStyle {
        val prefs = prefs(context)
        // Preserve the previous glass preference until this widget is customized.
        val legacyOpacity = if (prefs.getString("widget_theme", null) == "Glass") 0.8f else 1f
        return WidgetStyle(prefs.getInt("widget_${widgetId}_color", WidgetStyle().color),
            prefs.getFloat("widget_${widgetId}_opacity", legacyOpacity).coerceIn(0f, 1f))
    }

    suspend fun save(context: Context, widgetId: Int, style: WidgetStyle) = withContext(Dispatchers.IO) {
        require(widgetId > 0 && style.opacity.isFinite() && style.opacity in 0f..1f)
        check(prefs(context).edit()
            .putInt("widget_${widgetId}_color", ColorUtils.setAlphaComponent(style.color, 255))
            .putFloat("widget_${widgetId}_opacity", style.opacity).commit()) { "Could not save widget appearance" }
        val repository = (context.applicationContext as KomoriApplication).repository
        FocusWidgetProvider.update(context, repository.allTasks.first(), intArrayOf(widgetId))
    }

    fun delete(context: Context, ids: IntArray) {
        prefs(context).edit().apply {
            ids.forEach { remove("widget_${it}_color"); remove("widget_${it}_opacity") }
        }.apply()
    }

    fun restore(context: Context, oldIds: IntArray, newIds: IntArray) {
        val styles = oldIds.map { read(context, it) }
        prefs(context).edit().apply {
            oldIds.zip(newIds).forEachIndexed { index, (_, newId) ->
                putInt("widget_${newId}_color", styles[index].color)
                putFloat("widget_${newId}_opacity", styles[index].opacity)
            }
        }.apply()
    }
}
