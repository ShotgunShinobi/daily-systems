package com.dailysystems.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** User-defined priority label. Rank 0 is the highest priority. */
data class PriorityLabel(
    val id: String,
    val name: String,
    val colorArgb: Int,
    val rank: Int,
)

data class Task(
    val id: String,
    val title: String,
    val labelId: String? = null,
    val dueDate: LocalDate? = null,
    val runSystemId: String? = null,
    val notes: String = "",
    val notesUpdatedAt: Long? = null,
    val done: Boolean = false,
    val doneOn: LocalDate? = null,
    val doneAt: Long? = null,
    val createdAt: Long = 0,
    val isExample: Boolean = false,
    /** Notify at this time on [dueDate]; ignored without a due date. */
    val reminderTime: LocalTime? = null,
    /** Set on the open occurrence of a recurring to-do. */
    val repeat: RepeatRule? = null,
    /** Shared by every occurrence of a recurring to-do. */
    val seriesId: String? = null,
)

enum class ItemOrder { PRIORITY, LISTED }

enum class ScheduleType { DAILY, WEEKLY, MONTHLY, DATES }

enum class ItemKind { STEP, SLOT }

data class SystemItem(
    val id: String,
    val kind: ItemKind,
    val title: String,
    val labelId: String? = null,
    val position: Int = 0,
    val slotCount: Int = 1,
    /** Empty = the slot accepts to-dos with any label (or none). */
    val acceptedLabelIds: Set<String> = emptySet(),
)

/**
 * A reusable routine. All schedule fields are kept even when another [scheduleType] is active,
 * so switching type back and forth in the editor doesn't lose picks.
 */
data class DailySystem(
    val id: String,
    val name: String,
    val labelId: String? = null,
    val active: Boolean = true,
    val itemOrder: ItemOrder = ItemOrder.PRIORITY,
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val weekdays: Set<DayOfWeek> = emptySet(),
    /** 1..31, plus [LAST_DAY] meaning "last day of the month". */
    val monthDays: Set<Int> = emptySet(),
    val dates: Set<LocalDate> = emptySet(),
    val items: List<SystemItem> = emptyList(),
    val createdAt: Long = 0,
    val isExample: Boolean = false,
    /** "Starts at" reminder on every day the system runs. */
    val reminderTime: LocalTime? = null,
) {
    companion object {
        const val LAST_DAY = 0
    }
}

data class Note(
    val id: String,
    val title: String,
    val body: String,
    val labelId: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isExample: Boolean = false,
)

/** Identifies one step checked off on a day. */
data class StepKey(val systemId: String, val itemId: String)
