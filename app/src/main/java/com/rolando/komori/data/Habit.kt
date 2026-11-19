package com.rolando.komori.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class Subroutine(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String = "FlowerLotus", // Store icon name as String
    val completedDays: Set<Int> = emptySet(), // 0 = Mon, 6 = Sun for current week
    val completedDates: Set<String> = emptySet(), // ISO-8601 strings "YYYY-MM-DD"
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val targetDaysPerWeek: Int = 7, // Daily = 7, or 1..7
    val createdAt: Long = System.currentTimeMillis(),
    val subroutines: List<Subroutine> = emptyList(),
    val subroutinesDate: String? = null
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun getEffectiveSubroutines(today: LocalDate = LocalDate.now()): List<Subroutine> {
        if (subroutines.isEmpty()) return emptyList()
        val isDoneToday = isCompletedOn(today)
        val isForToday = subroutinesDate == today.toString()
        return when {
            isDoneToday -> subroutines.map { it.copy(isCompleted = true) }
            isForToday -> subroutines
            else -> subroutines.map { it.copy(isCompleted = false) }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun isDayCompletedInCurrentWeek(dayIndex: Int, today: LocalDate = LocalDate.now()): Boolean {
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val targetDate = startOfWeek.plusDays(dayIndex.toLong()).toString()
        return completedDates.contains(targetDate) || completedDays.contains(dayIndex)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun isCompletedOn(date: LocalDate): Boolean {
        return completedDates.contains(date.toString())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCompletionsInWeek(startOfWeek: LocalDate): Int {
        var count = 0
        for (i in 0..6) {
            val d = startOfWeek.plusDays(i.toLong())
            if (completedDates.contains(d.toString())) {
                count++
            }
        }
        return count
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun calculateCompletionRate(daysCount: Int = 30, today: LocalDate = LocalDate.now()): Float {
        if (daysCount <= 0) return 0f
        var completed = 0
        for (i in 0 until daysCount) {
            val d = today.minusDays(i.toLong())
            if (completedDates.contains(d.toString())) {
                completed++
            }
        }
        return (completed.toFloat() / daysCount).coerceIn(0f, 1f)
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

        @RequiresApi(Build.VERSION_CODES.O)
        fun calculateBestStreak(completedDates: Set<String>): Int {
            if (completedDates.isEmpty()) return 0
            val parsedDates = completedDates.mapNotNull {
                try {
                    LocalDate.parse(it)
                } catch (e: Exception) {
                    null
                }
            }.sorted()

            if (parsedDates.isEmpty()) return 0

            var maxStreak = 1
            var currentStreak = 1
            for (i in 1 until parsedDates.size) {
                val prev = parsedDates[i - 1]
                val curr = parsedDates[i]
                if (curr == prev.plusDays(1)) {
                    currentStreak++
                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak
                    }
                } else if (curr != prev) {
                    currentStreak = 1
                }
            }
            return maxStreak
        }
    }
}
