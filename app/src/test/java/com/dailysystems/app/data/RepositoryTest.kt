package com.dailysystems.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dailysystems.app.TEST_TODAY
import com.dailysystems.app.domain.DayPlanner
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.PlannedRow
import com.dailysystems.app.domain.ScheduleType
import com.dailysystems.app.testContainer
import java.time.DayOfWeek
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
class RepositoryTest {
    private val c = testContainer()

    @Test
    fun seedsDefaultLabelsOnce() = runBlocking {
        c.labels.seedDefaultsIfEmpty()
        c.labels.seedDefaultsIfEmpty()
        val ls = c.labels.labels.first()
        assertEquals(listOf("Must do", "Should do", "Could do", "Someday"), ls.map { it.name })
        assertEquals(listOf(0, 1, 2, 3), ls.map { it.rank })
        assertEquals(0xFFD7263D.toInt(), ls[0].colorArgb)
    }

    @Test
    fun deletingALabelUnlabelsItemsAndReranks() = runBlocking {
        c.labels.seedDefaultsIfEmpty()
        val t = c.tasks.create("Task", labelId = "label-should")
        val s = c.systems.createBlank("label-should")
        val item = c.systems.addItem(s.id, ItemKind.SLOT, "label-should")
        c.systems.toggleAccepted(item, "label-should")
        val n = c.notes.create("Note", labelId = "label-should")

        c.labels.delete("label-should")

        val ls = c.labels.labels.first()
        assertEquals(listOf("Must do" to 0, "Could do" to 1, "Someday" to 2), ls.map { it.name to it.rank })
        assertNull(c.tasks.get(t.id)!!.labelId)
        val sys = c.systems.systems.first().single()
        assertNull(sys.labelId)
        assertNull(sys.items.single().labelId)
        assertTrue(sys.items.single().acceptedLabelIds.isEmpty())
        assertNull(c.notes.observe(n.id).first()!!.labelId)
    }

    @Test
    fun moveAndAddLabels() = runBlocking {
        c.labels.seedDefaultsIfEmpty()
        c.labels.move("label-someday", -1)
        assertEquals(listOf("Must do", "Should do", "Someday", "Could do"), c.labels.labels.first().map { it.name })
        c.labels.move("label-must", -1) // already first: no-op
        val added = c.labels.add()
        assertEquals(4, added.rank)
        assertEquals(LABEL_PALETTE[2], added.colorArgb) // first palette color not in use
    }

    @Test
    fun deletingASystemClearsTasksThatRunIt() = runBlocking {
        val s = c.systems.createBlank(null)
        val t = c.tasks.create("Appt", dueDate = TEST_TODAY, runSystemId = s.id)
        c.systems.delete(s.id)
        assertNull(c.tasks.get(t.id)!!.runSystemId)
    }

    @Test
    fun systemEditsRoundTrip() = runBlocking {
        c.labels.seedDefaultsIfEmpty()
        val s = c.systems.createBlank("label-must")
        c.systems.setScheduleType(s.id, ScheduleType.WEEKLY)
        c.systems.toggleWeekday(s.id, DayOfWeek.MONDAY)
        c.systems.toggleWeekday(s.id, DayOfWeek.SUNDAY)
        c.systems.toggleMonthDay(s.id, 0)
        c.systems.toggleMonthDay(s.id, 15)
        c.systems.addDate(s.id, TEST_TODAY)
        val a = c.systems.addItem(s.id, ItemKind.STEP, null)
        val b = c.systems.addItem(s.id, ItemKind.SLOT, null)
        c.systems.setSlotCount(b, 42) // clamped to 9
        c.systems.moveItem(s.id, b, -1)

        val sys = c.systems.systems.first().single()
        assertEquals(ScheduleType.WEEKLY, sys.scheduleType)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), sys.weekdays)
        assertEquals(setOf(0, 15), sys.monthDays)
        assertEquals(setOf(TEST_TODAY), sys.dates)
        assertEquals(listOf(b, a), sys.items.map { it.id })
        assertEquals(listOf(0, 1), sys.items.map { it.position })
        assertEquals(9, sys.items.first().slotCount)

        c.systems.removeItem(s.id, b)
        assertEquals(listOf(a to 0), c.systems.systems.first().single().items.map { it.id to it.position })
    }

    @Test
    fun blankThingsAreDiscarded() = runBlocking {
        val t = c.tasks.create()
        val keep = c.tasks.create(notes = "only notes")
        c.tasks.deleteIfBlank(t.id)
        c.tasks.deleteIfBlank(keep.id)
        assertNull(c.tasks.get(t.id))
        assertEquals("only notes", c.tasks.get(keep.id)!!.notes)

        val s = c.systems.createBlank(null)
        val withItem = c.systems.createBlank(null).also { c.systems.addItem(it.id, ItemKind.STEP, null) }
        c.systems.deleteIfBlank(s.id)
        c.systems.deleteIfBlank(withItem.id)
        assertEquals(listOf(withItem.id), c.systems.systems.first().map { it.id })

        val n = c.notes.create()
        c.notes.deleteIfBlank(n.id)
        assertTrue(c.notes.notes.first().isEmpty())
    }

    @Test
    fun stepCompletionTogglesPerDay() = runBlocking {
        val s = c.systems.createBlank(null)
        val step = c.systems.addItem(s.id, ItemKind.STEP, null)
        c.systems.toggleStep(s.id, step, TEST_TODAY)
        assertEquals(1, c.systems.checkedSteps(TEST_TODAY).first().size)
        assertTrue(c.systems.checkedSteps(TEST_TODAY.plusDays(1)).first().isEmpty())
        c.systems.toggleStep(s.id, step, TEST_TODAY)
        assertTrue(c.systems.checkedSteps(TEST_TODAY).first().isEmpty())
    }

    @Test
    fun starterExamplesPlanAsDescribed() = runBlocking {
        c.starterExamples.load(TEST_TODAY)
        val labels = c.labels.labels.first()
        val systems = c.systems.systems.first()
        val tasks = c.tasks.tasks.first()
        assertEquals(setOf("Morning launch", "Evening wind-down", "Big-day prep"), systems.map { it.name }.toSet())
        assertTrue((systems.map { it.isExample } + tasks.map { it.isExample }).all { it })
        assertEquals(5, tasks.size)
        assertEquals("How systems work", c.notes.notes.first().single().title)

        val plan = DayPlanner.planDay(TEST_TODAY, systems, tasks, labels)
        assertEquals(listOf("Morning launch", "Evening wind-down"), plan.systems.map { it.system.name })
        val morning = plan.systems.first().rows
        // By priority: "Review today's plan" (Must) and the Must slot before the Should step.
        assertEquals(listOf("Review today's plan", "Top priorities", "Water and a 10-minute walk"), morning.map { it.item.title })
        assertEquals(
            listOf("Pay electricity bill", "Reply to pending emails"),
            (morning[1] as PlannedRow.Slot).tasks.map { it.title },
        )

        // Big-day prep runs on the appointment's date via the target-date run.
        val appt = DayPlanner.planDay(TEST_TODAY.plusDays(4), systems, tasks, labels)
        assertTrue(appt.systems.any { it.system.name == "Big-day prep" && it.runningForTarget })
    }
}
