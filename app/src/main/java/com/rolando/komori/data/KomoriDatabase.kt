package com.rolando.komori.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class KomoriDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var Instance: KomoriDatabase? = null

        fun getDatabase(context: Context): KomoriDatabase {
            // If instance exists, return it. If not, create it.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, KomoriDatabase::class.java, "komori_database")
                    .fallbackToDestructiveMigration() // Wipes data if you change the table structure
                    .build()
                    .also { Instance = it }
            }
        }
    }
}