package com.dailysystems.app.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class Reminder(
    /** Stable per occurrence, e.g. "task:<id>:2026-11-03"; used to avoid duplicate notifications. */
    val key: String,
    val at: LocalDateTime,
    val kind: Kind,
    val refId: String,
    val date: LocalDate,
) {
    enum class Kind { SYSTEM_START, TASK_DUE }
}

/** Which reminders fall in a time window. Pure Kotlin; the Android scheduler just executes the result. */
object ReminderPlanner {

    /** Reminders with `start < at <= end`, earliest first. */
    fun between(
        start: LocalDateTime,
        end: LocalDateTime,
        systems: Collection<DailySystem>,
        tasks: Collection<Task>,
        labels: Collection<PriorityLabel>,
    ): List<Reminder> {
        if (!end.isAfter(start)) return emptyList()
        val labelMap = labels.associateBy { it.id }
        val out = ArrayList<Reminder>()
        var day = start.toLocalDate()
        while (!day.isAfter(end.toLocalDate())) {
            // Same rule as Today: scheduled or target-date run, and never while paused.
            DayPlanner.activeSystems(systems, tasks, labelMap, day).forEach { s ->
                val t = s.reminderTime ?: return@forEach
                out += Reminder("sys:${s.id}:$day", day.atTime(t), Reminder.Kind.SYSTEM_START, s.id, day)
            }
            tasks.forEach { t ->
                val time = t.reminderTime
                if (!t.done && time != null && t.dueDate == day) {
                    out += Reminder("task:${t.id}:$day", day.atTime(time), Reminder.Kind.TASK_DUE, t.id, day)
                }
            }
            day = day.plusDays(1)
        }
        return out.filter { it.at.isAfter(start) && !it.at.isAfter(end) }.sortedWith(compareBy({ it.at }, { it.key }))
    }

    fun next(
        after: LocalDateTime,
        systems: Collection<DailySystem>,
        tasks: Collection<Task>,
        labels: Collection<PriorityLabel>,
        horizonDays: Long = 8,
    ): Reminder? = between(after, after.plusDays(horizonDays), systems, tasks, labels).firstOrNull()
}
