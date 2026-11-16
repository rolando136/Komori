package com.rolando.komori.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Task::class, WeeklyEvent::class, Habit::class, Note::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class KomoriDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun weeklyEventDao(): WeeklyEventDao
    abstract fun habitDao(): HabitDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var Instance: KomoriDatabase? = null

        fun getDatabase(context: Context): KomoriDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, KomoriDatabase::class.java, "komori_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
