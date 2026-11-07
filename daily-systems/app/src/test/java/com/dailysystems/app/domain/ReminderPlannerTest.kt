package com.dailysystems.app.domain

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderPlannerTest {
    private val tue = LocalDate.of(2026, 11, 3)
    private val labels = listOf(PriorityLabel("must", "Must do", 0, 0))
    private val seven = LocalTime.of(7, 0)

    private fun sys(id: String, type: ScheduleType = ScheduleType.DAILY, active: Boolean = true, time: LocalTime? = seven) =
        DailySystem(id, id, "must", active = active, scheduleType = type, reminderTime = time)

    @Test
    fun `system reminders fire on each running day`() {
        val r = ReminderPlanner.between(tue.atTime(6, 0), tue.plusDays(1).atTime(8, 0), listOf(sys("m")), emptyList(), labels)
        assertEquals(listOf(tue.atTime(7, 0), tue.plusDays(1).atTime(7, 0)), r.map { it.at })
        assertEquals("sys:m:$tue", r.first().key)
    }

    @Test
    fun `window is exclusive at start and inclusive at end`() {
        val s = listOf(sys("m"))
        assertEquals(0, ReminderPlanner.between(tue.atTime(7, 0), tue.atTime(9, 0), s, emptyList(), labels).size)
        assertEquals(1, ReminderPlanner.between(tue.atTime(6, 0), tue.atTime(7, 0), s, emptyList(), labels).size)
    }

    @Test
    fun `paused systems and systems without a time never remind`() {
        val r = ReminderPlanner.between(tue.atStartOfDay(), tue.plusDays(3).atStartOfDay(), listOf(sys("p", active = false), sys("n", time = null)), emptyList(), labels)
        assertEquals(emptyList<Reminder>(), r)
    }

    @Test
    fun `dates system reminds on a task target date`() {
        val prep = sys("prep", ScheduleType.DATES)
        val appt = Task("a", "Appt", dueDate = tue.plusDays(2), runSystemId = "prep")
        val r = ReminderPlanner.next(tue.atStartOfDay(), listOf(prep), listOf(appt), labels)
        assertEquals(tue.plusDays(2).atTime(7, 0), r?.at)
        assertEquals(Reminder.Kind.SYSTEM_START, r?.kind)
    }

    @Test
    fun `task reminders need a date and time and are skipped once done`() {
        val t = Task("t", "Bill", dueDate = tue, reminderTime = LocalTime.of(18, 30))
        val noTime = Task("x", "x", dueDate = tue)
        val done = t.copy(id = "d", done = true, doneOn = tue)
        val r = ReminderPlanner.between(tue.atStartOfDay(), tue.plusDays(1).atStartOfDay(), emptyList(), listOf(t, noTime, done), labels)
        assertEquals(listOf("task:t:$tue"), r.map { it.key })
        assertNull(ReminderPlanner.next(tue.atTime(19, 0), emptyList(), listOf(t), labels))
    }
}
