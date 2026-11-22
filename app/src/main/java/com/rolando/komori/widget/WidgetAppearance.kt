package com.rolando.komori.widget

import android.content.Context
import com.rolando.komori.KomoriApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

enum class WidgetTheme { Solid, Glass }

object WidgetAppearance {
    private const val KEY = "widget_theme"

    fun read(context: Context): WidgetTheme {
        val value = context.getSharedPreferences("komori_prefs", Context.MODE_PRIVATE).getString(KEY, null)
        return WidgetTheme.entries.firstOrNull { it.name == value } ?: WidgetTheme.Solid
    }

    suspend fun save(context: Context, theme: WidgetTheme) = withContext(Dispatchers.IO) {
        check(context.getSharedPreferences("komori_prefs", Context.MODE_PRIVATE)
            .edit().putString(KEY, theme.name).commit()) { "Could not save widget theme" }
        val repository = (context.applicationContext as KomoriApplication).repository
        FocusWidgetProvider.update(context, repository.allTasks.first())
    }
}
