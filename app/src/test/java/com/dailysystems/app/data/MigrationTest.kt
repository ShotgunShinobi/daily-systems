package com.dailysystems.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** v1 (first release) → v2 (reminders, recurring to-dos) keeps existing data. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun v1DataSurvives() {
        helper.createDatabase("migration-test", 1).apply {
            execSQL("INSERT INTO priority_label (id, name, colorArgb, rank) VALUES ('l1', 'Must do', -2677187, 0)")
            execSQL("INSERT INTO system (id, name, labelId, active, itemOrder, scheduleType, weekdaysMask, monthDays, createdAt, isExample) VALUES ('s1', 'Morning', 'l1', 1, 'PRIORITY', 'DAILY', 0, '', 1, 0)")
            execSQL("INSERT INTO task (id, title, labelId, dueDate, runSystemId, notes, notesUpdatedAt, done, doneOn, doneAt, createdAt, isExample) VALUES ('t1', 'Pay bill', 'l1', '2026-11-03', 's1', 'n', NULL, 0, NULL, NULL, 1, 0)")
            close()
        }
        helper.runMigrationsAndValidate("migration-test", 2, true)

        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, "migration-test")
            .allowMainThreadQueries().build()
        runBlocking {
            val t = db.taskDao().observeAll().first().single().toDomain()
            assertEquals("Pay bill", t.title)
            assertEquals("s1", t.runSystemId)
            assertNull(t.repeat)
            assertNull(t.reminderTime)
            assertEquals("Morning", db.systemDao().observeSystems().first().single().name)
        }
        db.close()
    }
}
