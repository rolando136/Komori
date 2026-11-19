package com.rolando.komori.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rolando.komori.KomoriApplication
import com.rolando.komori.data.Habit
import com.rolando.komori.data.Note
import com.rolando.komori.data.Subroutine
import com.rolando.komori.data.Task
import com.rolando.komori.data.TaskRepeat
import com.rolando.komori.data.TaskRepository
import com.rolando.komori.data.WeeklyEvent
import com.rolando.komori.ui.screens.DeleteScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // --- TASKS ---
    val allTasks: StateFlow<List<Task>?> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val weeklyEvents: Flow<List<WeeklyEvent>> = repository.getAllWeeklyEvents()

    fun insertWeeklyEvent(event: WeeklyEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertScheduleEvent(event)
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun toggleTask(task: Task, isChecked: Boolean): Boolean {
        if (isChecked && task.subtasks.isNotEmpty() && !task.subtasks.all { it.isCompleted }) {
            return false // Blocked: all subtasks must be accomplished first!
        }
        val todayStr = LocalDate.now().toString()
        val updated = if (isChecked) {
            task.copy(
                isCompleted = true,
                lastCompletedDate = if (task.repeat != TaskRepeat.NONE) todayStr else null
            )
        } else {
            task.copy(
                isCompleted = false,
                lastCompletedDate = null
            )
        }
        viewModelScope.launch { repository.updateTask(updated) }
        return true
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { repository.updateTask(task) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    // --- SCHEDULE / WEEKLY EVENTS ---
    val scheduleEvents: StateFlow<List<WeeklyEvent>> = repository.allScheduleEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addScheduleEvent(
        title: String,
        description: String,
        category: String,
        startDate: LocalDate,
        recurrence: String,
        start: LocalTime,
        end: LocalTime,
        color: Int
    ) {
        val newEvent = WeeklyEvent(
            title = title,
            description = description,
            category = category,
            startDate = startDate,
            recurrence = recurrence,
            startTime = start,
            endTime = end,
            colorArgb = color
        )
        viewModelScope.launch { repository.insertScheduleEvent(newEvent) }
    }

    fun updateScheduleEvent(event: WeeklyEvent) {
        viewModelScope.launch { repository.insertScheduleEvent(event) }
    }

    fun deleteScheduleEvent(event: WeeklyEvent) {
        viewModelScope.launch {
            repository.deleteScheduleEvent(event)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun deleteRecurringScheduleEvent(event: WeeklyEvent, scope: DeleteScope) {
        viewModelScope.launch(Dispatchers.IO) {
            when (scope) {
                DeleteScope.THIS_EVENT -> {
                    val dateToSkip = event.startDate.toString()
                    repository.addExclusionDate(event.id, dateToSkip)
                }
                DeleteScope.FOLLOWING_EVENTS -> {
                    val newEndDate = event.startDate.minusDays(1)
                    repository.updateRecurrenceEnd(event.id, newEndDate)
                }
                DeleteScope.ALL_EVENTS -> {
                    repository.deleteEventById(event.id)
                }
            }
        }
    }

    // --- HABITS (RITUALS) ---
    val allHabits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Seed default habits and welcome note on first run if empty
        viewModelScope.launch(Dispatchers.IO) {
            val existingHabits = repository.allHabits.first()
            if (existingHabits.isEmpty()) {
                val defaults = listOf(
                    Habit(name = "Morning Meditation", iconName = "FlowerLotus"),
                    Habit(name = "Read 20 Pages", iconName = "BookOpen"),
                    Habit(name = "Drink 2L Water", iconName = "Drop"),
                    Habit(name = "Workout / Gym", iconName = "Barbell"),
                    Habit(name = "Evening Journal", iconName = "PencilSimple")
                )
                repository.insertHabits(defaults)
            }

            val existingNotes = repository.allNotes.first()
            if (existingNotes.isEmpty()) {
                val welcomeNote = Note(
                    title = "Welcome to Komori",
                    content = "Komori (木漏れ日) is the sunlight filtering through trees.\n\nA quiet space to organize your focus, nurture daily rituals, keep your calendar, and collect your thoughts with calm clarity.",
                    isPinned = true
                )
                repository.insertNote(welcomeNote)
            }

            // Reset completed repeating tasks from prior days
            try {
                val existingTasks = repository.allTasks.first()
                val todayStr = LocalDate.now().toString()
                existingTasks.forEach { task ->
                    if (task.repeat != TaskRepeat.NONE && task.isCompleted && task.lastCompletedDate != null && task.lastCompletedDate != todayStr) {
                        val resetSubs = task.subtasks.map { it.copy(isCompleted = false) }
                        repository.updateTask(task.copy(isCompleted = false, subtasks = resetSubs))
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun addHabit(name: String, iconName: String, targetDaysPerWeek: Int = 7, subroutines: List<Subroutine> = emptyList()) {
        val newHabit = Habit(
            name = name.trim(),
            iconName = iconName,
            targetDaysPerWeek = targetDaysPerWeek,
            subroutines = subroutines
        )
        viewModelScope.launch {
            repository.insertHabit(newHabit)
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun toggleHabitDate(habit: Habit, date: LocalDate): Boolean {
        val today = LocalDate.now()
        val targetDate = date.toString()

        // Gating: If routine has subroutines, only mark done if all subroutines are accomplished!
        if (date == today && habit.subroutines.isNotEmpty()) {
            val effectiveSubs = habit.getEffectiveSubroutines(today)
            val allDone = effectiveSubs.all { it.isCompleted }
            if (!habit.isCompletedOn(today) && !allDone) {
                return false // Cannot mark done! Subroutines must be accomplished first.
            }
        }

        val newDates = if (habit.completedDates.contains(targetDate)) {
            habit.completedDates - targetDate
        } else {
            habit.completedDates + targetDate
        }

        val startOfCurrentWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfCurrentWeek = startOfCurrentWeek.plusDays(6)
        val newDays = if (!date.isBefore(startOfCurrentWeek) && !date.isAfter(endOfCurrentWeek)) {
            val dayIndex = date.dayOfWeek.value - 1
            if (newDates.contains(targetDate)) {
                habit.completedDays + dayIndex
            } else {
                habit.completedDays - dayIndex
            }
        } else {
            habit.completedDays
        }

        val newStreak = Habit.calculateStreak(newDates, today)
        val calculatedBest = Habit.calculateBestStreak(newDates)
        val newBestStreak = maxOf(habit.bestStreak, calculatedBest, newStreak)

        // If unchecking today, also reset subroutines to incomplete
        val updatedSubroutines = if (date == today && !newDates.contains(targetDate) && habit.subroutines.isNotEmpty()) {
            habit.subroutines.map { it.copy(isCompleted = false) }
        } else {
            habit.subroutines
        }

        val updated = habit.copy(
            subroutines = updatedSubroutines,
            subroutinesDate = if (date == today) today.toString() else habit.subroutinesDate,
            completedDays = newDays,
            completedDates = newDates,
            streak = newStreak,
            bestStreak = newBestStreak
        )
        viewModelScope.launch {
            repository.updateHabit(updated)
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun toggleSubroutine(habit: Habit, subroutineId: String) {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val currentSubs = habit.getEffectiveSubroutines(today)
        val updatedSubs = currentSubs.map { sub ->
            if (sub.id == subroutineId) sub.copy(isCompleted = !sub.isCompleted) else sub
        }
        val allAccomplished = updatedSubs.isNotEmpty() && updatedSubs.all { it.isCompleted }

        val newDates = if (allAccomplished) {
            habit.completedDates + todayStr
        } else {
            habit.completedDates - todayStr
        }

        val startOfCurrentWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfCurrentWeek = startOfCurrentWeek.plusDays(6)
        val dayIndex = today.dayOfWeek.value - 1
        val newDays = if (allAccomplished) {
            habit.completedDays + dayIndex
        } else {
            habit.completedDays - dayIndex
        }

        val newStreak = Habit.calculateStreak(newDates, today)
        val calculatedBest = Habit.calculateBestStreak(newDates)
        val newBestStreak = maxOf(habit.bestStreak, calculatedBest, newStreak)

        val updated = habit.copy(
            subroutines = updatedSubs,
            subroutinesDate = todayStr,
            completedDays = newDays,
            completedDates = newDates,
            streak = newStreak,
            bestStreak = newBestStreak
        )
        viewModelScope.launch {
            repository.updateHabit(updated)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun toggleHabitDay(habit: Habit, dayIndex: Int) {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val targetDate = startOfWeek.plusDays(dayIndex.toLong())
        toggleHabitDate(habit, targetDate)
    }

    // --- BACKUP & RESTORE ---
    suspend fun exportBackupJson(): String {
        return repository.exportBackupJson()
    }

    suspend fun importBackupJson(json: String, clearExisting: Boolean = false): Result<com.rolando.komori.data.ImportSummary> {
        return repository.importBackupJson(json, clearExisting)
    }

    // --- NOTES ---
    val allNotes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addNote(title: String, content: String, isPinned: Boolean = false, colorArgb: Int? = null) {
        val note = Note(
            title = title.trim(),
            content = content.trim(),
            timestamp = System.currentTimeMillis(),
            isPinned = isPinned,
            colorArgb = colorArgb
        )
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(timestamp = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun toggleNotePin(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as KomoriApplication)
                TaskViewModel(application.repository)
            }
        }
    }
}
