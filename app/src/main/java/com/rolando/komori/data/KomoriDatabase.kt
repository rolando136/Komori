package com.rolando.komori.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// CHANGE VERSION FROM 2 -> 3
@Database(entities = [Task::class, WeeklyEvent::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class KomoriDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun weeklyEventDao(): WeeklyEventDao

    companion object {
        @Volatile
        private var Instance: KomoriDatabase? = null

        fun getDatabase(context: Context): KomoriDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, KomoriDatabase::class.java, "komori_database")
                    // This line is crucial during dev: it allows Room to destroy the old DB
                    // and create a new empty one if the versions don't match.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}