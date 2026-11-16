package com.rolando.komori.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.rolando.komori.ui.theme.SageGreen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "weekly_events")
data class WeeklyEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val category: String = "Custom",

    // CHANGED: 'day' is removed, replaced by 'startDate'
    val startDate: LocalDate,

    // NEW: "Once", "Daily", "Weekly", "Monthly", "Yearly"
    val recurrence: String = "Once",
    val recurrenceEnd: LocalDate? = null,
    val groupId: Long? = null,

    val excludedDates: String = "",

    val startTime: LocalTime,
    val endTime: LocalTime,
    val colorArgb: Int
) {
    val color: Color
        get() = Color(colorArgb)

    // Helper for backward compatibility in code that asks for DayOfWeek
    val day: DayOfWeek
        @RequiresApi(Build.VERSION_CODES.O)
        get() = startDate.dayOfWeek
}