package com.dailysystems.app.data

import com.dailysystems.app.domain.DailySystem
import com.dailysystems.app.domain.Note
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.domain.RepeatRule
import com.dailysystems.app.domain.RepeatUnit
import com.dailysystems.app.domain.SystemItem
import com.dailysystems.app.domain.Task
import java.time.DayOfWeek
import java.time.LocalDate

fun PriorityLabelEntity.toDomain() = PriorityLabel(id, name, colorArgb, rank)
fun PriorityLabel.toEntity() = PriorityLabelEntity(id, name, colorArgb, rank)

fun TaskEntity.toDomain() = Task(
    id = id, title = title, labelId = labelId, dueDate = dueDate, runSystemId = runSystemId,
    notes = notes, notesUpdatedAt = notesUpdatedAt, done = done, doneOn = doneOn, doneAt = doneAt,
    createdAt = createdAt, isExample = isExample, reminderTime = reminderTime,
    repeat = RepeatUnit.entries.firstOrNull { it.name == repeatUnit }?.let { unit ->
        RepeatRule(unit, repeatInterval, WeekdayMask.decode(repeatWeekdays), repeatAnchor ?: dueDate ?: LocalDate.now())
    },
    seriesId = seriesId,
)

fun Task.toEntity() = TaskEntity(
    id = id, title = title, labelId = labelId, dueDate = dueDate, runSystemId = runSystemId,
    notes = notes, notesUpdatedAt = notesUpdatedAt, done = done, doneOn = doneOn, doneAt = doneAt,
    createdAt = createdAt, isExample = isExample, reminderTime = reminderTime,
    repeatUnit = repeat?.unit?.name ?: "NONE",
    repeatInterval = repeat?.interval ?: 1,
    repeatWeekdays = repeat?.let { WeekdayMask.encode(it.weekdays) } ?: 0,
    repeatAnchor = repeat?.anchor,
    seriesId = seriesId,
)

fun NoteEntity.toDomain() = Note(id, title, body, labelId, createdAt, updatedAt, isExample)
fun Note.toEntity() = NoteEntity(id, title, body, labelId, createdAt, updatedAt, isExample)

/** bit 0 = Monday … bit 6 = Sunday */
object WeekdayMask {
    fun encode(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
    fun decode(mask: Int): Set<DayOfWeek> = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
}

object MonthDaysCsv {
    fun encode(days: Set<Int>): String = days.sorted().joinToString(",")
    fun decode(csv: String): Set<Int> =
        csv.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..31 }.toSet()
}

fun SystemEntity.toDomain(
    items: List<SystemItemEntity>,
    accepted: Map<String, Set<String>>,
    dates: List<SystemDateEntity>,
) = DailySystem(
    id = id, name = name, labelId = labelId, active = active, itemOrder = itemOrder,
    scheduleType = scheduleType,
    weekdays = WeekdayMask.decode(weekdaysMask),
    monthDays = MonthDaysCsv.decode(monthDays),
    dates = dates.map { it.date }.toSet(),
    items = items.sortedBy { it.position }.map {
        SystemItem(it.id, it.kind, it.title, it.labelId, it.position, it.slotCount, accepted[it.id].orEmpty())
    },
    createdAt = createdAt, isExample = isExample, reminderTime = reminderTime,
)

fun DailySystem.toEntity() = SystemEntity(
    id = id, name = name, labelId = labelId, active = active, itemOrder = itemOrder,
    scheduleType = scheduleType, weekdaysMask = WeekdayMask.encode(weekdays),
    monthDays = MonthDaysCsv.encode(monthDays), createdAt = createdAt, isExample = isExample,
    reminderTime = reminderTime,
)
