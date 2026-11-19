package com.rolando.komori.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    private val gson = Gson()

    // --- Priority Converter (Enum <-> String) ---
    @TypeConverter
    fun fromPriority(priority: TaskPriority): String {
        return priority.name // Saves "HIGH", "MEDIUM", "LOW" to DB
    }

    @TypeConverter
    fun toPriority(priority: String): TaskPriority {
        return try {
            TaskPriority.valueOf(priority)
        } catch (e: Exception) {
            TaskPriority.MEDIUM // Default fallback
        }
    }

    // --- Task Repeat Converter (Enum <-> String) ---
    @TypeConverter
    fun fromTaskRepeat(repeat: TaskRepeat?): String {
        return repeat?.name ?: TaskRepeat.NONE.name
    }

    @TypeConverter
    fun toTaskRepeat(value: String?): TaskRepeat {
        return try {
            TaskRepeat.valueOf(value ?: TaskRepeat.NONE.name)
        } catch (e: Exception) {
            TaskRepeat.NONE
        }
    }

    // --- Subtask List Converter (List <-> JSON String) ---
    @TypeConverter
    fun fromSubtaskList(subtasks: List<Subtask>): String {
        return gson.toJson(subtasks)
    }

    @TypeConverter
    fun toSubtaskList(subtasksString: String): List<Subtask> {
        val listType = object : TypeToken<List<Subtask>>() {}.type
        return gson.fromJson(subtasksString, listType) ?: emptyList()
    }

    // --- Subroutine List Converter (List <-> JSON String) ---
    @TypeConverter
    fun fromSubroutineList(subroutines: List<Subroutine>?): String {
        return gson.toJson(subroutines ?: emptyList<Subroutine>())
    }

    @TypeConverter
    fun toSubroutineList(subroutinesString: String?): List<Subroutine> {
        if (subroutinesString.isNullOrBlank()) return emptyList()
        val listType = object : TypeToken<List<Subroutine>>() {}.type
        return gson.fromJson(subroutinesString, listType) ?: emptyList()
    }

    // --- YOUR EXISTING CONVERTERS (Unchanged) ---

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? { // Changed to nullable to be safe
        return date?.toEpochDay()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): Long? = time?.toNanoOfDay()

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalTime(value: Long?): LocalTime? = value?.let { LocalTime.ofNanoOfDay(it) }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromDayOfWeek(day: DayOfWeek): String = day.name

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toDayOfWeek(value: String): DayOfWeek = DayOfWeek.valueOf(value)

    // --- Habit & Note Converters ---
    @TypeConverter
    fun fromIntSet(set: Set<Int>?): String {
        return set?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toIntSet(data: String?): Set<Int> {
        if (data.isNullOrBlank()) return emptySet()
        return data.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    @TypeConverter
    fun fromStringSet(set: Set<String>?): String {
        return set?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toStringSet(data: String?): Set<String> {
        if (data.isNullOrBlank()) return emptySet()
        return data.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}