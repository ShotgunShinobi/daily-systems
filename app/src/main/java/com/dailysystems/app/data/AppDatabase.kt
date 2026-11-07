package com.dailysystems.app.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PriorityLabelEntity::class,
        TaskEntity::class,
        SystemEntity::class,
        SystemDateEntity::class,
        SystemItemEntity::class,
        SlotAcceptedLabelEntity::class,
        StepCompletionEntity::class,
        NoteEntity::class,
    ],
    version = 2,
    exportSchema = true,
    // v2 adds reminders and recurring to-dos; all new columns are nullable or defaulted.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun labelDao(): LabelDao
    abstract fun taskDao(): TaskDao
    abstract fun systemDao(): SystemDao
    abstract fun stepCompletionDao(): StepCompletionDao
    abstract fun noteDao(): NoteDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "daily-systems.db").build()

        fun inMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }
}
