package com.rolando.komori.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String = "FlowerLotus", // Store icon name as String
    val completedDays: Set<Int> = emptySet(), // 0 = Mon, 6 = Sun
    val completedDates: Set<String> = emptySet(), // ISO-8601 strings "YYYY-MM-DD"
    val streak: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun isDayCompletedInCurrentWeek(dayIndex: Int, today: LocalDate = LocalDate.now()): Boolean {
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val targetDate = startOfWeek.plusDays(dayIndex.toLong()).toString()
        return completedDates.contains(targetDate) || completedDays.contains(dayIndex)
    }

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun calculateStreak(completedDates: Set<String>, today: LocalDate = LocalDate.now()): Int {
            var count = 0
            var checkDate = today
            if (completedDates.contains(checkDate.toString())) {
                while (completedDates.contains(checkDate.toString())) {
                    count++
                    checkDate = checkDate.minusDays(1)
                }
            } else {
                checkDate = today.minusDays(1)
                while (completedDates.contains(checkDate.toString())) {
                    count++
                    checkDate = checkDate.minusDays(1)
                }
            }
            return count
        }
    }
}
