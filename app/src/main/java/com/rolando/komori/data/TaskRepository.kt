package com.rolando.komori.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TaskRepository(
    private val taskDao: TaskDao,
    private val weeklyEventDao: WeeklyEventDao,
    private val habitDao: HabitDao,
    private val noteDao: NoteDao
) {

    // --- TASKS ---
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteEventById(id: Int) {
        weeklyEventDao.deleteEventById(id)
    }

    suspend fun deleteFutureEvents(groupId: Long, date: LocalDate) {
        weeklyEventDao.deleteFutureEvents(groupId, date)
    }

    suspend fun updateRecurrenceEnd(id: Int, endDate: LocalDate) {
        weeklyEventDao.updateRecurrenceEnd(id, endDate)
    }

    suspend fun deleteAllEventsInSeries(groupId: Long) {
        weeklyEventDao.deleteAllEventsInSeries(groupId)
    }

    suspend fun addExclusionDate(id: Int, date: String) {
        weeklyEventDao.addExclusionDate(id, date)
    }

    // --- SCHEDULE EVENTS ---
    val allScheduleEvents: Flow<List<WeeklyEvent>> = weeklyEventDao.getAllEvents()

    suspend fun insertScheduleEvent(event: WeeklyEvent) {
        weeklyEventDao.insertEvent(event)
    }

    suspend fun deleteScheduleEvent(event: WeeklyEvent) {
        weeklyEventDao.deleteEvent(event)
    }

    fun getAllWeeklyEvents(): Flow<List<WeeklyEvent>> {
        return weeklyEventDao.getAllEvents()
    }

    // --- HABITS (RITUALS) ---
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()

    suspend fun insertHabit(habit: Habit) {
        habitDao.insertHabit(habit)
    }

    suspend fun insertHabits(habits: List<Habit>) {
        habitDao.insertHabits(habits)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    suspend fun deleteHabitById(id: String) {
        habitDao.deleteHabitById(id)
    }

    // --- NOTES ---
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: String) {
        noteDao.deleteNoteById(id)
    }
}
