package com.dailysystems.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitStatsTest {
    private val utc = ZoneOffset.UTC
    private val today: LocalDate = LocalDate.of(2026, 11, 4) // Wednesday

    private fun millis(d: LocalDate) = d.atStartOfDay(utc).toInstant().toEpochMilli()

    private fun system(
        id: String = "s",
        steps: Int = 2,
        created: LocalDate = today.minusDays(20),
        type: ScheduleType = ScheduleType.DAILY,
        weekdays: Set<DayOfWeek> = emptySet(),
        active: Boolean = true,
        slot: Boolean = false,
    ) = DailySystem(
        id = id, name = id, active = active, scheduleType = type, weekdays = weekdays, createdAt = millis(created),
        items = (1..steps).map { SystemItem("$id-$it", ItemKind.STEP, "Step $it", position = it) } +
            if (slot) listOf(SystemItem("$id-slot", ItemKind.SLOT, "Slot", position = 99)) else emptyList(),
    )

    /** daysAgo → how many of the first steps were checked that day. */
    private fun done(s: DailySystem, vararg byDaysAgo: Pair<Long, Int>): Map<LocalDate, Set<StepKey>> =
        byDaysAgo.associate { (ago, n) -> today.minusDays(ago) to (1..n).map { StepKey(s.id, "${s.id}-$it") }.toSet() }

    private fun history(s: DailySystem, c: Map<LocalDate, Set<StepKey>>, tasks: List<Task> = emptyList()) =
        HabitStats.history(s, tasks, c, today, utc)

    @Test
    fun `partial and complete days both keep the streak, a miss breaks it`() {
        val s = system()
        val h = history(s, done(s, 1L to 1, 2L to 2, 3L to 2, 5L to 2, 6L to 2, 7L to 2, 8L to 1))!!
        assertEquals(DayResult.PARTIAL, h.days[today.minusDays(1)])
        assertEquals(DayResult.COMPLETE, h.days[today.minusDays(2)])
        assertEquals(DayResult.MISSED, h.days[today.minusDays(4)])
        assertEquals(3, h.currentStreak)
        assertEquals(4, h.bestStreak)
    }

    @Test
    fun `today does not break a streak until it is over`() {
        val s = system()
        val h = history(s, done(s, 1L to 2, 2L to 2))!!
        assertEquals(DayResult.PENDING, h.days[today])
        assertEquals(2, h.currentStreak)
        val checkedToday = history(s, done(s, 0L to 1, 1L to 2, 2L to 2))!!
        assertEquals(3, checkedToday.currentStreak)
    }

    @Test
    fun `only run days count, so a weekly system keeps its streak across days off`() {
        // Mon, Wed. Today is Wed 4 Nov; previous runs Mon 2, Wed 28 Oct, Mon 26 Oct.
        val s = system(type = ScheduleType.WEEKLY, weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        val h = history(s, done(s, 2L to 2, 7L to 1, 9L to 2))!!
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), h.days.keys.map { it.dayOfWeek }.toSet())
        assertEquals(3, h.currentStreak)
        assertEquals(0, h.missesBeforeToday)
    }

    @Test
    fun `days before the system existed are not misses`() {
        val s = system(created = today.minusDays(2))
        val h = history(s, done(s, 1L to 2, 2L to 2))!!
        assertEquals(listOf(today.minusDays(2), today.minusDays(1), today), h.days.keys.toList())
        assertEquals(1f, h.rate30!!, 0.001f)
    }

    @Test
    fun `target-date runs count as run days`() {
        val s = system(type = ScheduleType.DATES)
        val appt = Task("t", "Appt", dueDate = today.minusDays(3), runSystemId = "s")
        val h = history(s, done(s, 3L to 2), listOf(appt))!!
        assertEquals(listOf(today.minusDays(3)), h.days.keys.toList())
        assertEquals(1, h.currentStreak)
    }

    @Test
    fun `systems without steps have no history`() {
        assertNull(history(system(steps = 0, slot = true), emptyMap()))
    }

    @Test
    fun `step stats start from a step's first check-off`() {
        val s = system()
        // Step 2 first checked 3 days ago; step 1 every day for the last 5.
        val c = (1L..5L).associate { ago ->
            today.minusDays(ago) to (setOf(StepKey("s", "s-1")) + if (ago <= 3) setOf(StepKey("s", "s-2")) else emptySet())
        }
        val h = history(s, c)!!
        val (one, two) = h.steps
        assertEquals(5, one.done)
        assertEquals(3, two.runs)
        assertEquals(3, two.done)
        assertEquals(3, two.currentStreak)
    }

    @Test
    fun `never miss twice nudge`() {
        val s = system()
        assertNull("last run kept", HabitStats.nudge(history(s, done(s, 1L to 1)), today))
        val once = HabitStats.nudge(history(s, done(s, 2L to 2)), today)
        assertTrue(once!!.startsWith("Missed last time"))
        val twice = HabitStats.nudge(history(s, done(s, 3L to 2)), today)
        assertTrue(twice!!.startsWith("Missed the last 2 runs"))
        assertNull("gone once something is checked today", HabitStats.nudge(history(s, done(s, 0L to 1, 2L to 2)), today))
        val paused = system(active = false)
        assertNull("paused", HabitStats.nudge(history(paused, done(paused, 2L to 2)), today))
        val notToday = system(type = ScheduleType.WEEKLY, weekdays = setOf(DayOfWeek.MONDAY))
        assertNull("doesn't run today", HabitStats.nudge(history(notToday, emptyMap()), today))
        val brandNew = system(created = today)
        assertNull("no past runs yet", HabitStats.nudge(history(brandNew, emptyMap()), today))
    }

    @Test
    fun `weekly review`() {
        val a = system("a")
        val b = system("b", steps = 1)
        val monday = HabitStats.weekStart(today) // Mon 2 Nov; Mon..Wed so far, today pending for both
        val c = done(a, 1L to 2, 2L to 1).toMutableMap()
        // b: kept Mon only; missed Tue.
        c[monday] = c.getValue(monday) + StepKey("b", "b-1")
        // Last week: a kept every day, b none.
        (3L..9L).forEach { c[today.minusDays(it)] = setOf(StepKey("a", "a-1")) }
        val tasks = listOf(
            Task("x", "x", done = true, doneOn = today.minusDays(1)),
            Task("y", "y", done = true, doneOn = today.minusDays(10)),
        )
        val r = HabitStats.weekReview(monday, listOf(a, b), tasks, c, today, utc)
        assertEquals(listOf(2, 2), r.systems.map { it.runDays })
        assertEquals(listOf(2, 1), r.systems.map { it.kept })
        assertEquals(3f / 4, r.keptRate!!, 0.001f)
        assertEquals(7f / 14, r.previousKeptRate!!, 0.001f)
        assertEquals(4, r.stepsChecked)
        assertEquals(1, r.todosDone)
        assertNotNull(r.mostMissed.firstOrNull { it.systemName == "b" && it.missed == 1 })
        assertEquals("Step 2", r.mostMissed.first { it.systemName == "a" }.stepTitle)
    }
}
