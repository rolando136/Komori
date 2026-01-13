package com.rolando.komori.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a single DataStore instance
val Context.dataStore by preferencesDataStore(name = "settings")

class ThemeManager(private val context: Context) {

    // Key for saving the setting
    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    // Read the setting (Default is false/Light)
    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    // Save the setting
    suspend fun saveTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDark
        }
    }
}