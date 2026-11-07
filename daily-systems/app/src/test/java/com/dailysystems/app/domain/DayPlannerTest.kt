package com.dailysystems.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayPlannerTest {

    private val must = PriorityLabel("must", "Must do", 0, 0)
    private val should = PriorityLabel("should", "Should do", 0, 1)
    private val could = PriorityLabel("could", "Could do", 0, 2)
    private val someday = PriorityLabel("someday", "Someday", 0, 3)
    private val labels = listOf(must, should, could, someday)
    private val labelMap = labels.associateBy { it.id }

    private val today: LocalDate = LocalDate.of(2026, 3, 10) // a Tuesday
    private val tomorrow: LocalDate = today.plusDays(1)

    private var seq = 0L
    private fun task(
        title: String,
        label: PriorityLabel? = null,
        due: LocalDate? = null,
        runs: String? = null,
        done: Boolean = false,
        doneOn: LocalDate? = null,
    ) = Task(
        id = title, title = title, labelId = label?.id, dueDate = due, runSystemId = runs,
        done = done, doneOn = doneOn, createdAt = ++seq,
    )

    private fun slot(id: String, count: Int = 1, accepts: Set<PriorityLabel> = emptySet(), label: PriorityLabel? = null, pos: Int = 0) =
        SystemItem(id, ItemKind.SLOT, id, label?.id, pos, count, accepts.map { it.id }.toSet())

    private fun step(id: String, label: PriorityLabel? = null, pos: Int = 0) =
        SystemItem(id, ItemKind.STEP, id, label?.id, pos)

    private fun system(
        id: String,
        label: PriorityLabel? = null,
        items: List<SystemItem> = emptyList(),
        type: ScheduleType = ScheduleType.DAILY,
        active: Boolean = true,
        order: ItemOrder = ItemOrder.PRIORITY,
        weekdays: Set<DayOfWeek> = emptySet(),
        monthDays: Set<Int> = emptySet(),
        dates: Set<LocalDate> = emptySet(),
        createdAt: Long = ++seq,
    ) = DailySystem(
        id = id, name = id, labelId = label?.id, active = active, itemOrder = order, scheduleType = type,
        weekdays = weekdays, monthDays = monthDays, dates = dates, items = items, createdAt = createdAt,
    )

    private fun plan(date: LocalDate, systems: List<DailySystem>, tasks: List<Task>) =
        DayPlanner.planDay(date, systems, tasks, labels)

    private fun DayPlan.slotTitles(systemId: String, slotId: String): List<String> =
        systems.first { it.system.id == systemId }.rows
            .filterIsInstance<PlannedRow.Slot>().first { it.item.id == slotId }.tasks.map { it.title }

    // --- Property 1 -------------------------------------------------------------------------

    @Test
    fun `task finished today stays in the slot it filled, shown checked`() {
        val sys = system("morning", must, listOf(slot("top", count = 2)))
        val a = task("A", must)
        val b = task("B", should)
        val c = task("C", could)

        assertEquals(listOf("A", "B"), plan(today, listOf(sys), listOf(a, b, c)).slotTitles("morning", "top"))

        val aDone = a.copy(done = true, doneOn = today)
        val after = plan(today, listOf(sys), listOf(aDone, b, c))
        assertEquals(listOf("A", "B"), after.slotTitles("morning", "top"))
        val filled = after.systems.single().rows.filterIsInstance<PlannedRow.Slot>().single().tasks
        assertTrue(filled.first { it.id == "A" }.done)
        assertEquals(listOf("C"), after.nextUp.map { it.title })
    }

    // --- Property 2 -------------------------------------------------------------------------

    @Test
    fun `tomorrow done tasks drop out and the next highest priority task fills the slot`() {
        val sys = system("morning", must, listOf(slot("top", count = 2)))
        val a = task("A", must, done = true, doneOn = today)
        val b = task("B", should)
        val c = task("C", could)

        val p = plan(tomorrow, listOf(sys), listOf(a, b, c))
        assertEquals(listOf("B", "C"), p.slotTitles("morning", "top"))
        assertTrue(p.alsoDue.isEmpty() && p.nextUp.isEmpty())
    }

    // --- Property 3 -------------------------------------------------------------------------

    @Test
    fun `higher priority systems claim matching tasks first and no task is in two slots`() {
        // Created first but lower priority: must not win the Must-do task.
        val low = system("low", could, listOf(slot("lowSlot", count = 2)), createdAt = 1)
        val high = system("high", must, listOf(slot("highSlot", count = 1, accepts = setOf(must))), createdAt = 2)
        val a = task("A", must)
        val b = task("B", must)
        val c = task("C", should)

        val p = plan(today, listOf(low, high), listOf(a, b, c))
        assertEquals(listOf("high", "low"), p.systems.map { it.system.id })
        assertEquals(listOf("A"), p.slotTitles("high", "highSlot"))
        assertEquals(listOf("B", "C"), p.slotTitles("low", "lowSlot"))

        val all = p.systems.flatMap { s -> s.rows.filterIsInstance<PlannedRow.Slot>().flatMap { it.tasks } }
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test
    fun `systems with equal priority are ordered by creation time`() {
        val first = system("first", should, createdAt = 10)
        val second = system("second", should, createdAt = 20)
        val unlabelled = system("unlabelled", null, createdAt = 0)
        val p = plan(today, listOf(unlabelled, second, first), emptyList())
        assertEquals(listOf("first", "second", "unlabelled"), p.systems.map { it.system.id })
    }

    @Test
    fun `slot only accepts its labels and empty set accepts anything`() {
        val sys = system(
            "s", must,
            listOf(
                slot("onlySomeday", count = 3, accepts = setOf(someday), pos = 0),
                slot("any", count = 3, pos = 1),
            ),
            order = ItemOrder.LISTED,
        )
        val a = task("A", must)
        val b = task("B", someday)
        val c = task("C", null)
        val p = plan(today, listOf(sys), listOf(a, b, c))
        assertEquals(listOf("B"), p.slotTitles("s", "onlySomeday"))
        assertEquals(listOf("A", "C"), p.slotTitles("s", "any"))
    }

    @Test
    fun `slot count below one still takes one task`() {
        val sys = system("s", must, listOf(slot("x", count = 0)))
        val p = plan(today, listOf(sys), listOf(task("A", must), task("B", must)))
        assertEquals(listOf("A"), p.slotTitles("s", "x"))
    }

    // --- Property 4 -------------------------------------------------------------------------

    @Test
    fun `overdue open tasks are still candidates`() {
        val sys = system("s", must, listOf(slot("x", count = 1)))
        val overdue = task("Overdue", should, due = today.minusDays(3))
        val p = plan(today, listOf(sys), listOf(overdue))
        assertEquals(listOf("Overdue"), p.slotTitles("s", "x"))
    }

    @Test
    fun `overdue tasks not placed in a slot show in also due`() {
        val overdue = task("Overdue", should, due = today.minusDays(3))
        val p = plan(today, emptyList(), listOf(overdue))
        assertEquals(listOf("Overdue"), p.alsoDue.map { it.title })
    }

    // --- Property 5 -------------------------------------------------------------------------

    @Test
    fun `future dated tasks do not appear before their date`() {
        val sys = system("s", must, listOf(slot("x", count = 3)))
        val future = task("Future", must, due = today.plusDays(2))
        val now = task("Now", could)

        val p = plan(today, listOf(sys), listOf(future, now))
        assertEquals(listOf("Now"), p.slotTitles("s", "x"))
        assertTrue(p.alsoDue.none { it.id == "Future" })

        val onDay = plan(today.plusDays(2), listOf(sys), listOf(future, now))
        assertEquals(listOf("Future", "Now"), onDay.slotTitles("s", "x"))
    }

    // --- Property 6 -------------------------------------------------------------------------

    @Test
    fun `dates schedule with no dates still runs on a task target date`() {
        val prep = system("prep", must, listOf(step("lay out")), type = ScheduleType.DATES)
        val appt = task("Appointment", must, due = today.plusDays(4), runs = "prep")

        assertTrue(plan(today, listOf(prep), listOf(appt)).systems.isEmpty())
        val p = plan(today.plusDays(4), listOf(prep), listOf(appt))
        assertEquals(listOf("prep"), p.systems.map { it.system.id })
        assertTrue(p.systems.single().runningForTarget)
    }

    @Test
    fun `target run stays on the day the task was finished but not after`() {
        val prep = system("prep", must, type = ScheduleType.DATES)
        val doneThatDay = task("Appt", must, due = today, runs = "prep", done = true, doneOn = today)
        assertEquals(1, plan(today, listOf(prep), listOf(doneThatDay)).systems.size)

        val doneEarlier = doneThatDay.copy(doneOn = today.minusDays(1))
        assertTrue(plan(today, listOf(prep), listOf(doneEarlier)).systems.isEmpty())
    }

    @Test
    fun `dates schedule runs on its listed dates`() {
        val sys = system("d", must, type = ScheduleType.DATES, dates = setOf(today))
        assertTrue(DayPlanner.scheduledOn(sys, today))
        assertFalse(DayPlanner.scheduledOn(sys, tomorrow))
        assertFalse(plan(today, listOf(sys), emptyList()).systems.single().runningForTarget)
    }

    // --- Property 7 -------------------------------------------------------------------------

    @Test
    fun `last day monthly works for 28, 29, 30 and 31 day months`() {
        val sys = system("m", type = ScheduleType.MONTHLY, monthDays = setOf(DailySystem.LAST_DAY))
        listOf(
            LocalDate.of(2026, 2, 28), // 28 days
            LocalDate.of(2028, 2, 29), // leap year, 29 days
            LocalDate.of(2026, 4, 30), // 30 days
            LocalDate.of(2026, 1, 31), // 31 days
            LocalDate.of(2026, 12, 31), // year end
        ).forEach { d ->
            assertTrue("$d should be last day", DayPlanner.scheduledOn(sys, d))
            assertFalse("${d.minusDays(1)} is not last day", DayPlanner.scheduledOn(sys, d.minusDays(1)))
        }
        assertFalse(DayPlanner.scheduledOn(sys, LocalDate.of(2028, 2, 28)))
    }

    @Test
    fun `monthly day numbers and last day combine`() {
        val sys = system("m", type = ScheduleType.MONTHLY, monthDays = setOf(1, 15, DailySystem.LAST_DAY))
        assertTrue(DayPlanner.scheduledOn(sys, LocalDate.of(2026, 6, 1)))
        assertTrue(DayPlanner.scheduledOn(sys, LocalDate.of(2026, 6, 15)))
        assertTrue(DayPlanner.scheduledOn(sys, LocalDate.of(2026, 6, 30)))
        assertFalse(DayPlanner.scheduledOn(sys, LocalDate.of(2026, 6, 16)))
        // A 31st pick simply doesn't fire in shorter months.
        val the31st = system("x", type = ScheduleType.MONTHLY, monthDays = setOf(31))
        assertFalse(DayPlanner.scheduledOn(the31st, LocalDate.of(2026, 6, 30)))
    }

    // --- Property 8 -------------------------------------------------------------------------

    @Test
    fun `paused systems never run, including target date runs`() {
        val paused = system("p", must, listOf(slot("x")), active = false)
        val pausedDates = system("pd", must, type = ScheduleType.DATES, active = false)
        val t = task("T", must, due = today, runs = "pd")

        val p = plan(today, listOf(paused, pausedDates), listOf(t))
        assertTrue(p.systems.isEmpty())
        assertEquals(listOf("T"), p.alsoDue.map { it.title })
    }

    // --- Ordering, weekly, sections, progress -------------------------------------------------

    @Test
    fun `weekly schedule matches chosen weekdays`() {
        val sys = system("w", type = ScheduleType.WEEKLY, weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        assertTrue(DayPlanner.scheduledOn(sys, LocalDate.of(2026, 3, 9))) // Monday
        assertFalse(DayPlanner.scheduledOn(sys, LocalDate.of(2026, 3, 10))) // Tuesday
        assertTrue(DayPlanner.scheduledOn(sys, LocalDate.of(2026, 3, 13))) // Friday
    }

    @Test
    fun `items ordered by priority keep listed order for ties, listed ignores labels`() {
        val items = listOf(
            step("walk", should, pos = 0),
            step("review", must, pos = 1),
            slot("top", label = must, pos = 2),
            step("none", null, pos = 3),
        )
        val byPriority = system("s", items = items, order = ItemOrder.PRIORITY)
        assertEquals(listOf("review", "top", "walk", "none"), DayPlanner.orderedItems(byPriority, labelMap).map { it.id })
        val listed = byPriority.copy(itemOrder = ItemOrder.LISTED)
        assertEquals(listOf("walk", "review", "top", "none"), DayPlanner.orderedItems(listed, labelMap).map { it.id })
    }

    @Test
    fun `task order is rank, then dated first, then due date, then created`() {
        val undated = task("undated", should)
        val later = task("later", should, due = today.minusDays(1))
        val earlier = task("earlier", should, due = today.minusDays(5))
        val unlabelled = task("unlabelled", null, due = today.minusDays(9))
        val top = task("top", must)
        val sorted = listOf(undated, later, earlier, unlabelled, top).sortedWith(DayPlanner.taskComparator(labelMap))
        assertEquals(listOf("top", "earlier", "later", "undated", "unlabelled"), sorted.map { it.id })
    }

    @Test
    fun `rest splits into also due and next up, next up excludes done and is capped at five on screen`() {
        val dated = task("dated", should, due = today)
        val doneDated = task("doneDated", should, due = today, done = true, doneOn = today)
        val undated = (1..7).map { task("u$it", could) }
        val p = plan(today, emptyList(), listOf(dated, doneDated) + undated)
        assertEquals(setOf("dated", "doneDated"), p.alsoDue.map { it.id }.toSet())
        assertEquals(7, p.nextUp.size)
        assertEquals(listOf("u1", "u2", "u3", "u4", "u5"), p.nextUpTop.map { it.id })
    }

    @Test
    fun `progress counts steps, slot tasks and also due`() {
        val sys = system("s", must, listOf(step("a", pos = 0), step("b", pos = 1), slot("x", count = 2, pos = 2)), order = ItemOrder.LISTED)
        val t1 = task("T1", must, done = true, doneOn = today)
        val t2 = task("T2", must)
        val due = task("Due", should, due = today, done = true, doneOn = today)
        // Only 2 slot positions, so "Due" ends up... in the slot? No: T1 and T2 are must (rank 0) and fill it first.
        val p = plan(today, listOf(sys), listOf(t1, t2, due))
        assertEquals(listOf("T1", "T2"), p.slotTitles("s", "x"))
        val progress = p.progress(setOf(StepKey("s", "a")))
        // steps: 1/2, slot: 1/2, also due: 1/1
        assertEquals(Progress(3, 5), progress)
    }

    @Test
    fun `unknown label ids rank as unlabelled`() {
        val ghost = task("ghost", null).copy(labelId = "deleted-label")
        val real = task("real", someday)
        val sorted = listOf(ghost, real).sortedWith(DayPlanner.taskComparator(labelMap))
        assertEquals(listOf("real", "ghost"), sorted.map { it.id })
    }
}
