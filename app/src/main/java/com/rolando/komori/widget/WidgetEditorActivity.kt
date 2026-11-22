package com.rolando.komori.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rolando.komori.data.ThemeManager
import com.rolando.komori.ui.ThemeViewModel
import com.rolando.komori.ui.theme.KomoriTheme

class WidgetEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, FocusWidgetProvider::class.java)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID || manager.getAppWidgetInfo(id)?.provider != provider) {
            Toast.makeText(this, "This widget is no longer available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val theme by themeViewModel.currentTheme.collectAsState()
            val themeManager = remember { ThemeManager(this) }
            val dark by themeManager.isDarkTheme.collectAsState(initial = isSystemInDarkTheme())
            val initial = remember(id) { WidgetAppearance.read(this, id) }
            KomoriTheme(themeOption = theme, darkTheme = dark) {
                WidgetAppearanceEditor(initial, onCancel = { finish() }, onSave = { style ->
                    check(manager.getAppWidgetInfo(id)?.provider == provider) { "This widget was removed" }
                    WidgetAppearance.save(this, id, style)
                    finish()
                })
            }
        }
    }
}
