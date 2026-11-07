package com.dailysystems.app.domain

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Everything the Today screen shows for one day. */
data class DayPlan(
    val date: LocalDate,
    val systems: List<PlannedSystem>,
    /** Visible dated to-dos not placed in any slot. */
    val alsoDue: List<Task>,
    /** All undated open to-dos not placed in any slot (UI shows the first [NEXT_UP_LIMIT]). */
    val nextUp: List<Task>,
) {
    val nextUpTop: List<Task> get() = nextUp.take(NEXT_UP_LIMIT)

    /** Progress = (checked steps + done slot tasks + done "also due") / (all of those). */
    fun progress(checkedSteps: Set<StepKey>): Progress {
        var done = 0
        var total = 0
        systems.forEach { ps ->
            ps.rows.forEach { row ->
                when (row) {
                    is PlannedRow.Step -> {
                        total++
                        if (StepKey(ps.system.id, row.item.id) in checkedSteps) done++
                    }
                    is PlannedRow.Slot -> row.tasks.forEach { total++; if (it.done) done++ }
                }
            }
        }
        alsoDue.forEach { total++; if (it.done) done++ }
        return Progress(done, total)
    }

    companion object {
        const val NEXT_UP_LIMIT = 5
    }
}

data class Progress(val done: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
}

data class PlannedSystem(
    val system: DailySystem,
    /** True when the system runs only because a to-do targets it on this day. */
    val runningForTarget: Boolean,
    val rows: List<PlannedRow>,
)

sealed interface PlannedRow {
    val item: SystemItem

    data class Step(override val item: SystemItem) : PlannedRow

    data class Slot(override val item: SystemItem, val tasks: List<Task>) : PlannedRow {
        val capacity: Int get() = item.slotCount.coerceAtLeast(1)
        val openPositions: Int get() = (capacity - tasks.size).coerceAtLeast(0)
    }
}

/**
 * The planning algorithm (SPEC §3). Pure Kotlin, no Android dependencies: every function takes
 * plain data and returns plain data.
 */
object DayPlanner {

    const val UNRANKED = Int.MAX_VALUE

    fun rank(labelId: String?, labels: Map<String, PriorityLabel>): Int =
        labelId?.let { labels[it]?.rank } ?: UNRANKED

    fun scheduledOn(system: DailySystem, date: LocalDate): Boolean = when (system.scheduleType) {
        ScheduleType.DAILY -> true
        ScheduleType.WEEKLY -> date.dayOfWeek in system.weekdays
        ScheduleType.MONTHLY -> date.dayOfMonth in system.monthDays ||
            (DailySystem.LAST_DAY in system.monthDays && isLastDayOfMonth(date))
        ScheduleType.DATES -> date in system.dates
    }

    fun isLastDayOfMonth(date: LocalDate): Boolean =
        date == date.with(TemporalAdjusters.lastDayOfMonth())

    fun targetRun(system: DailySystem, tasks: Collection<Task>, date: LocalDate): Boolean =
        tasks.any { t ->
            t.runSystemId == system.id && t.dueDate == date && (!t.done || t.doneOn == date)
        }

    fun systemComparator(labels: Map<String, PriorityLabel>): Comparator<DailySystem> =
        compareBy<DailySystem> { rank(it.labelId, labels) }
            .thenBy { it.createdAt }
            .thenBy { it.id }

    /** Systems running on [date], highest priority first. Paused systems never run. */
    fun activeSystems(
        systems: Collection<DailySystem>,
        tasks: Collection<Task>,
        labels: Map<String, PriorityLabel>,
        date: LocalDate,
    ): List<DailySystem> = systems
        .filter { it.active && (scheduledOn(it, date) || targetRun(it, tasks, date)) }
        .sortedWith(systemComparator(labels))

    fun orderedItems(system: DailySystem, labels: Map<String, PriorityLabel>): List<SystemItem> {
        val listed = system.items.sortedBy { it.position }
        return when (system.itemOrder) {
            ItemOrder.LISTED -> listed
            // sortedWith is stable, so ties keep their listed position.
            ItemOrder.PRIORITY -> listed.sortedWith(compareBy { rank(it.labelId, labels) })
        }
    }

    fun visibleOn(task: Task, date: LocalDate): Boolean =
        if (task.done) task.doneOn == date
        else task.dueDate == null || !task.dueDate.isAfter(date)

    /** rank ASC, dated before undated, dueDate ASC, createdAt ASC (id as a final tie-break). */
    fun taskComparator(labels: Map<String, PriorityLabel>): Comparator<Task> =
        compareBy<Task> { rank(it.labelId, labels) }
            .thenBy { if (it.dueDate != null) 0 else 1 }
            .thenBy { it.dueDate }
            .thenBy { it.createdAt }
            .thenBy { it.id }

    fun planDay(
        date: LocalDate,
        systems: Collection<DailySystem>,
        tasks: Collection<Task>,
        labels: Collection<PriorityLabel>,
    ): DayPlan {
        val labelMap = labels.associateBy { it.id }
        val candidates = tasks.filter { visibleOn(it, date) }.sortedWith(taskComparator(labelMap))
        val used = HashSet<String>()

        val planned = activeSystems(systems, tasks, labelMap, date).map { system ->
            val rows = orderedItems(system, labelMap).map { item ->
                when (item.kind) {
                    ItemKind.STEP -> PlannedRow.Step(item)
                    ItemKind.SLOT -> {
                        val want = item.slotCount.coerceAtLeast(1)
                        val take = ArrayList<Task>(want)
                        for (t in candidates) {
                            if (take.size >= want) break
                            if (t.id in used) continue
                            if (item.acceptedLabelIds.isNotEmpty() && t.labelId !in item.acceptedLabelIds) continue
                            take += t
                            used += t.id
                        }
                        PlannedRow.Slot(item, take)
                    }
                }
            }
            PlannedSystem(system, runningForTarget = !scheduledOn(system, date), rows = rows)
        }

        val rest = candidates.filter { it.id !in used }
        return DayPlan(
            date = date,
            systems = planned,
            alsoDue = rest.filter { it.dueDate != null },
            nextUp = rest.filter { it.dueDate == null && !it.done },
        )
    }
}
