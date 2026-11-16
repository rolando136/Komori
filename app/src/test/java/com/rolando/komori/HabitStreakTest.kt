package com.rolando.komori

import com.rolando.komori.data.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class HabitStreakTest {

    private val fixedToday = LocalDate.of(2026, 3, 30) // Monday

    @Test
    fun testEmptyCompletedDatesHasZeroStreak() {
        val streak = Habit.calculateStreak(emptySet(), fixedToday)
        assertEquals(0, streak)
    }

    @Test
    fun testCompletedTodayGivesStreak() {
        val completed = setOf("2026-03-30")
        val streak = Habit.calculateStreak(completed, fixedToday)
        assertEquals(1, streak)
    }

    @Test
    fun testCompletedTodayAndYesterdayGivesStreakOfTwo() {
        val completed = setOf("2026-03-30", "2026-03-29")
        val streak = Habit.calculateStreak(completed, fixedToday)
        assertEquals(2, streak)
    }

    @Test
    fun testCompletedYesterdayMaintainsStreakEvenIfNotCompletedToday() {
        val completed = setOf("2026-03-29", "2026-03-28")
        val streak = Habit.calculateStreak(completed, fixedToday)
        assertEquals(2, streak)
    }

    @Test
    fun testBrokenStreakResetsToZero() {
        // Completed 2 days ago, but missed yesterday and today
        val completed = setOf("2026-03-28", "2026-03-27")
        val streak = Habit.calculateStreak(completed, fixedToday)
        assertEquals(0, streak)
    }

    @Test
    fun testHabitDayCheckInCurrentWeek() {
        val habit = Habit(
            name = "Morning Meditation",
            completedDates = setOf("2026-03-30"), // Monday
            completedDays = setOf(2) // Wednesday
        )
        // 0 = Monday
        assertTrue(habit.isDayCompletedInCurrentWeek(0, fixedToday))
        // 2 = Wednesday
        assertTrue(habit.isDayCompletedInCurrentWeek(2, fixedToday))
        // 1 = Tuesday (not completed)
        assertFalse(habit.isDayCompletedInCurrentWeek(1, fixedToday))
    }
}
