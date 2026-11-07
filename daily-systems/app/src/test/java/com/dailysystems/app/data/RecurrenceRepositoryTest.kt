package com.dailysystems.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dailysystems.app.TEST_TODAY
import com.dailysystems.app.domain.DayPlanner
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.PlannedRow
import com.dailysystems.app.domain.RepeatRule
import com.dailysystems.app.domain.RepeatUnit
import com.dailysystems.app.testContainer
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RecurrenceRepositoryTest {
    private val c = testContainer()

    private suspend fun daily(): String {
        c.labels.seedDefaultsIfEmpty()
        val t = c.tasks.create("Water plants", labelId = "label-must", dueDate = TEST_TODAY, notes = "Balcony too")
        c.tasks.setReminderTime(t.id, LocalTime.of(8, 0))
        c.tasks.setRepeat(t.id, RepeatRule(RepeatUnit.DAY, 1, anchor = TEST_TODAY))
        return t.id
    }

    @Test
    fun checkingOffSpawnsTheNextOccurrenceAndKeepsTodaysInItsSlot() = runBlocking {
        val id = daily()
        val s = c.systems.createBlank("label-must")
        c.systems.addItem(s.id, ItemKind.SLOT, "label-must")

        c.tasks.toggleDone(id, TEST_TODAY)

        val tasks = c.tasks.tasks.first()
        assertEquals(2, tasks.size)
        val done = tasks.single { it.id == id }
        val next = tasks.single { it.id != id }
        assertTrue(done.done)
        assertNull(done.repeat)
        assertEquals(TEST_TODAY.plusDays(1), next.dueDate)
        assertEquals(RepeatUnit.DAY, next.repeat?.unit)
        assertEquals(done.seriesId, next.seriesId)
        assertEquals("Balcony too", next.notes)
        assertEquals(LocalTime.of(8, 0), next.reminderTime)

        // Property 1 still holds: today the checked occurrence stays in the slot.
        val plan = DayPlanner.planDay(TEST_TODAY, c.systems.systems.first(), tasks, c.labels.labels.first())
        assertEquals(listOf(id), (plan.systems.single().rows.single() as PlannedRow.Slot).tasks.map { it.id })
        // Tomorrow the new occurrence fills it.
        val tomorrow = DayPlanner.planDay(TEST_TODAY.plusDays(1), c.systems.systems.first(), tasks, c.labels.labels.first())
        assertEquals(listOf(next.id), (tomorrow.systems.single().rows.single() as PlannedRow.Slot).tasks.map { it.id })
    }

    @Test
    fun uncheckingFoldsTheNextOccurrenceBackIn() = runBlocking {
        val id = daily()
        c.tasks.toggleDone(id, TEST_TODAY)
        c.tasks.toggleDone(id, TEST_TODAY)
        val t = c.tasks.tasks.first().single()
        assertEquals(id, t.id)
        assertEquals(false, t.done)
        assertEquals(RepeatUnit.DAY, t.repeat?.unit)
        assertEquals("Balcony too", t.notes)
        assertEquals(TEST_TODAY, t.dueDate)
    }

    @Test
    fun uncheckingAnOlderOccurrenceLeavesTheSeriesAlone() = runBlocking {
        val id = daily()
        c.tasks.toggleDone(id, TEST_TODAY)
        val second = c.tasks.tasks.first().single { it.id != id }.id
        c.tasks.toggleDone(second, TEST_TODAY.plusDays(1))
        c.tasks.toggleDone(id, TEST_TODAY) // un-check the first one
        val tasks = c.tasks.tasks.first()
        assertEquals(3, tasks.size)
        assertEquals(1, tasks.count { it.repeat != null })
    }

    @Test
    fun overdueOccurrenceDoneTodaySchedulesFromToday() = runBlocking {
        c.labels.seedDefaultsIfEmpty()
        val t = c.tasks.create("Stretch", dueDate = TEST_TODAY.minusDays(4))
        c.tasks.setRepeat(t.id, RepeatRule(RepeatUnit.DAY, 1, anchor = TEST_TODAY.minusDays(4)))
        c.tasks.toggleDone(t.id, TEST_TODAY)
        assertEquals(TEST_TODAY.plusDays(1), c.tasks.tasks.first().single { !it.done }.dueDate)
    }

    @Test
    fun repeatNeedsADate() = runBlocking {
        val t = c.tasks.create("Undated")
        c.tasks.setRepeat(t.id, RepeatRule(RepeatUnit.WEEK, 1, anchor = TEST_TODAY))
        assertEquals(TEST_TODAY, c.tasks.get(t.id)!!.dueDate)
        c.tasks.setDueDate(t.id, TEST_TODAY.plusDays(2))
        assertEquals(TEST_TODAY.plusDays(2), c.tasks.get(t.id)!!.repeat?.anchor)
        c.tasks.setDueDate(t.id, null)
        assertNull(c.tasks.get(t.id)!!.repeat)
    }

    @Test
    fun dragAndDropMovesItemsAndLabels() = runBlocking {
        c.labels.seedDefaultsIfEmpty()
        c.labels.moveTo("label-someday", 0)
        assertEquals(listOf("Someday", "Must do", "Should do", "Could do"), c.labels.labels.first().map { it.name })
        assertEquals(listOf(0, 1, 2, 3), c.labels.labels.first().map { it.rank })

        val s = c.systems.createBlank(null)
        val a = c.systems.addItem(s.id, ItemKind.STEP, null)
        val b = c.systems.addItem(s.id, ItemKind.STEP, null)
        val d = c.systems.addItem(s.id, ItemKind.SLOT, null)
        c.systems.moveItemTo(s.id, a, 2)
        assertEquals(listOf(b, d, a), c.systems.systems.first().single().items.map { it.id })
        c.systems.moveItemTo(s.id, a, 9) // out of range: ignored
        assertEquals(listOf(0, 1, 2), c.systems.systems.first().single().items.map { it.position })
    }
}
