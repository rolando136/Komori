package com.rolando.komori.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Task::class, WeeklyEvent::class, Habit::class, Note::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class KomoriDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun weeklyEventDao(): WeeklyEventDao
    abstract fun habitDao(): HabitDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var Instance: KomoriDatabase? = null

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN bestStreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN targetDaysPerWeek INTEGER NOT NULL DEFAULT 7")
            }
        }

        fun getDatabase(context: Context): KomoriDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, KomoriDatabase::class.java, "komori_database")
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
