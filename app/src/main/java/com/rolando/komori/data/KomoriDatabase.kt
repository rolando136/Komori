package com.rolando.komori.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Task::class, WeeklyEvent::class, Habit::class, Note::class], version = 8, exportSchema = false)
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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeat TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN lastCompletedDate TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN subroutines TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE habits ADD COLUMN subroutinesDate TEXT")
            }
        }

        fun getDatabase(context: Context): KomoriDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, KomoriDatabase::class.java, "komori_database")
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
