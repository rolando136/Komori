package com.rolando.komori.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    // --- BACKUP & RESTORE ---
    suspend fun exportBackupJson(): String {
        val tasks = taskDao.getAllTasks().first()
        val events = weeklyEventDao.getAllEvents().first()
        val habits = habitDao.getAllHabits().first()
        val notes = noteDao.getAllNotes().first()

        val backup = KomoriBackup(
            tasks = tasks,
            weeklyEvents = events,
            habits = habits,
            notes = notes
        )
        return BackupManager.toJson(backup)
    }

    suspend fun importBackupJson(json: String, clearExisting: Boolean): Result<ImportSummary> {
        val backup = BackupManager.fromJson(json)
            ?: return Result.failure(IllegalArgumentException("Invalid Komori backup file format."))

        if (clearExisting) {
            taskDao.clearAllTasks()
            weeklyEventDao.clearAllEvents()
            habitDao.clearAllHabits()
            noteDao.clearAllNotes()
        }

        if (backup.tasks.isNotEmpty()) taskDao.insertTasks(backup.tasks)
        if (backup.weeklyEvents.isNotEmpty()) weeklyEventDao.insertEvents(backup.weeklyEvents)
        if (backup.habits.isNotEmpty()) habitDao.insertHabits(backup.habits)
        if (backup.notes.isNotEmpty()) noteDao.insertNotes(backup.notes)

        return Result.success(
            ImportSummary(
                tasksCount = backup.tasks.size,
                eventsCount = backup.weeklyEvents.size,
                habitsCount = backup.habits.size,
                notesCount = backup.notes.size
            )
        )
    }
}
