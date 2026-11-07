package com.dailysystems.app.domain

import java.time.LocalDate

/** Pure rules for completing / un-completing a recurring to-do. */
object Recurrence {
    /**
     * The next occurrence's due date when [task] is checked off on [doneOn]: the first occurrence
     * after both its due date and the day it was done (finishing an overdue daily task today
     * schedules tomorrow, not another past day).
     */
    fun nextDue(task: Task, rule: RepeatRule, doneOn: LocalDate): LocalDate {
        val due = task.dueDate ?: doneOn
        return rule.nextAfter(if (due.isAfter(doneOn)) due else doneOn)
    }
}
