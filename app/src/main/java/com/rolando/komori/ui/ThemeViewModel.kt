package com.rolando.komori.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rolando.komori.ui.theme.AppThemeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("komori_prefs", Context.MODE_PRIVATE)

    // 1. STATE
    private val _currentTheme = MutableStateFlow(AppThemeOption.Komori)
    val currentTheme: StateFlow<AppThemeOption> = _currentTheme.asStateFlow()

    init {
        loadTheme()
    }

    // 2. SAVE
    fun setTheme(theme: AppThemeOption) {
        _currentTheme.value = theme
        viewModelScope.launch {
            prefs.edit().putString("app_theme", theme.name).apply()
        }
    }

    // 3. LOAD
    private fun loadTheme() {
        val themeName = prefs.getString("app_theme", AppThemeOption.Komori.name)
        _currentTheme.value = try {
            AppThemeOption.valueOf(themeName ?: AppThemeOption.Komori.name)
        } catch (e: Exception) {
            AppThemeOption.Komori
        }
    }
}