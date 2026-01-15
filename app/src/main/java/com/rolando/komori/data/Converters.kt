package com.rolando.komori.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    // --- NEW: LocalDate Converters ---
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long {
        return date.toEpochDay()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalDate(value: Long): LocalDate {
        return LocalDate.ofEpochDay(value)
    }

    // --- EXISTING CONVERTERS (Keep these) ---
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromLocalTime(time: LocalTime): Long = time.toNanoOfDay()

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalTime(value: Long): LocalTime = LocalTime.ofNanoOfDay(value)

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromDayOfWeek(day: DayOfWeek): String = day.name

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toDayOfWeek(value: String): DayOfWeek = DayOfWeek.valueOf(value)
}