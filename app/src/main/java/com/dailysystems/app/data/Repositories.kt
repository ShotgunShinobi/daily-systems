package com.dailysystems.app.data

import androidx.room.withTransaction
import com.dailysystems.app.domain.DailySystem
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.ItemOrder
import com.dailysystems.app.domain.Note
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.domain.Recurrence
import com.dailysystems.app.domain.RepeatRule
import com.dailysystems.app.domain.ScheduleType
import com.dailysystems.app.domain.StepKey
import com.dailysystems.app.domain.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal fun newId(): String = UUID.randomUUID().toString()

/** Seeded on first launch (SPEC §1). */
val DEFAULT_LABELS = listOf(
    PriorityLabel("label-must", "Must do", 0xFFD7263D.toInt(), 0),
    PriorityLabel("label-should", "Should do", 0xFFF08A24.toInt(), 1),
    PriorityLabel("label-could", "Could do", 0xFF2F7FD8.toInt(), 2),
    PriorityLabel("label-someday", "Someday", 0xFF7A8794.toInt(), 3),
)

/** Picker palette (SPEC §1). */
val LABEL_PALETTE: List<Int> = listOf(
    0xFFD7263D, 0xFFF08A24, 0xFFE3B505, 0xFF3AA35C, 0xFF1F9E9A,
    0xFF2F7FD8, 0xFF5B5BD6, 0xFFA64AC9, 0xFFD6457F, 0xFF7A8794,
).map { it.toInt() }

class LabelRepository(private val db: AppDatabase) {
    private val dao = db.labelDao()

    val labels: Flow<List<PriorityLabel>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun seedDefaultsIfEmpty() {
        db.withTransaction { if (dao.count() == 0) dao.upsertAll(DEFAULT_LABELS.map { it.toEntity() }) }
    }

    suspend fun all(): List<PriorityLabel> = dao.getAll().map { it.toDomain() }

    suspend fun rename(id: String, name: String) = dao.rename(id, name)
    suspend fun setColor(id: String, color: Int) = dao.setColor(id, color)

    suspend fun move(id: String, delta: Int) = db.withTransaction {
        val i = dao.getAll().indexOfFirst { it.id == id }
        if (i >= 0) moveToUnsafe(id, i + delta)
    }

    /** Drag-and-drop: the label at [toIndex] gets that rank; the rest renumber 0..n-1. */
    suspend fun moveTo(id: String, toIndex: Int) = db.withTransaction { moveToUnsafe(id, toIndex) }

    private suspend fun moveToUnsafe(id: String, toIndex: Int) {
        val list = dao.getAll().toMutableList()
        val i = list.indexOfFirst { it.id == id }
        if (i < 0 || toIndex !in list.indices || toIndex == i) return
        list.add(toIndex, list.removeAt(i))
        rerank(list)
    }

    /** Items using the label become unlabelled (FK SET NULL); remaining labels are re-ranked 0..n-1. */
    suspend fun delete(id: String) = db.withTransaction {
        dao.delete(id)
        rerank(dao.getAll())
    }

    suspend fun add(): PriorityLabel = db.withTransaction {
        val list = dao.getAll()
        val used = list.map { it.colorArgb }.toSet()
        val label = PriorityLabel(
            id = newId(),
            name = "New label",
            colorArgb = LABEL_PALETTE.firstOrNull { it !in used } ?: LABEL_PALETTE.first(),
            rank = list.size,
        )
        dao.upsert(label.toEntity())
        label
    }

    private suspend fun rerank(list: List<PriorityLabelEntity>) {
        list.forEachIndexed { k, l -> if (l.rank != k) dao.setRank(l.id, k) }
    }
}

class TaskRepository(private val db: AppDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao = db.taskDao()

    val tasks: Flow<List<Task>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observe(id: String): Flow<Task?> = dao.observe(id).map { it?.toDomain() }

    suspend fun get(id: String): Task? = dao.get(id)?.toDomain()

    suspend fun create(
        title: String = "",
        labelId: String? = null,
        dueDate: LocalDate? = null,
        runSystemId: String? = null,
        notes: String = "",
        isExample: Boolean = false,
        createdAt: Long = clock(),
    ): Task {
        val t = Task(
            id = newId(), title = title, labelId = labelId, dueDate = dueDate, runSystemId = runSystemId,
            notes = notes, notesUpdatedAt = if (notes.isNotBlank()) createdAt else null,
            createdAt = createdAt, isExample = isExample,
        )
        dao.insert(t.toEntity())
        return t
    }

    suspend fun update(id: String, change: (Task) -> Task) = db.withTransaction {
        val cur = dao.get(id)?.toDomain() ?: return@withTransaction
        val next = change(cur)
        if (next != cur) dao.update(next.toEntity())
    }

    suspend fun setTitle(id: String, title: String) = update(id) { it.copy(title = title) }
    suspend fun setLabel(id: String, labelId: String?) = update(id) { it.copy(labelId = labelId) }
    /** Clearing the date also stops a repeat (a series needs a date); moving it re-anchors the series. */
    suspend fun setDueDate(id: String, date: LocalDate?) = update(id) {
        it.copy(dueDate = date, repeat = if (date == null) null else it.repeat?.copy(anchor = date))
    }

    suspend fun setReminderTime(id: String, time: LocalTime?) = update(id) { it.copy(reminderTime = time) }

    /** Setting a repeat on an undated to-do dates it to the rule's anchor. */
    suspend fun setRepeat(id: String, rule: RepeatRule?) = update(id) {
        if (rule == null) it.copy(repeat = null)
        else {
            val due = it.dueDate ?: rule.anchor
            it.copy(repeat = rule.copy(anchor = due), dueDate = due, seriesId = it.seriesId ?: it.id)
        }
    }
    suspend fun setRunSystem(id: String, systemId: String?) = update(id) { it.copy(runSystemId = systemId) }
    suspend fun setNotes(id: String, notes: String) = update(id) { it.copy(notes = notes, notesUpdatedAt = clock()) }

    /**
     * Toggle done; when marking done, [on] is the day it counts as finished.
     *
     * Recurring to-dos: the checked occurrence stays (done, in its slot for the day) and the series
     * moves to a new open occurrence on the next due date, carrying the rule and notes. Un-checking
     * right away folds that new occurrence back in, so a mis-tap leaves no trace.
     */
    suspend fun toggleDone(id: String, on: LocalDate) = db.withTransaction {
        val t = dao.get(id)?.toDomain() ?: return@withTransaction
        val now = clock()
        if (!t.done) {
            val rule = t.repeat
            if (rule == null) {
                dao.update(t.copy(done = true, doneOn = on, doneAt = now).toEntity())
                return@withTransaction
            }
            val series = t.seriesId ?: t.id
            dao.insert(
                t.copy(
                    id = newId(), dueDate = Recurrence.nextDue(t, rule, on), createdAt = now,
                    done = false, doneOn = null, doneAt = null, seriesId = series,
                ).toEntity(),
            )
            dao.update(t.copy(done = true, doneOn = on, doneAt = now, repeat = null, seriesId = series, notes = "", notesUpdatedAt = null).toEntity())
        } else {
            var restored = t.copy(done = false, doneOn = null, doneAt = null)
            val series = t.seriesId
            val doneOn = t.doneOn
            if (series != null && t.repeat == null && doneOn != null) {
                val head = dao.openInSeries(series).singleOrNull()?.toDomain()
                val rule = head?.repeat
                if (head != null && rule != null && head.dueDate == Recurrence.nextDue(t, rule, doneOn)) {
                    dao.delete(head.id)
                    restored = restored.copy(
                        title = head.title, labelId = head.labelId, runSystemId = head.runSystemId,
                        notes = head.notes, notesUpdatedAt = head.notesUpdatedAt,
                        reminderTime = head.reminderTime, repeat = rule,
                    )
                }
            }
            dao.update(restored.toEntity())
        }
    }

    suspend fun delete(id: String) = dao.delete(id)

    /** A brand-new to-do closed with an empty title and empty notes is discarded. */
    suspend fun deleteIfBlank(id: String) = db.withTransaction {
        val t = dao.get(id) ?: return@withTransaction
        if (t.title.isBlank() && t.notes.isBlank()) dao.delete(id)
    }
}

class SystemRepository(private val db: AppDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao = db.systemDao()
    private val steps = db.stepCompletionDao()

    val systems: Flow<List<DailySystem>> = combine(
        dao.observeSystems(), dao.observeItems(), dao.observeAccepted(), dao.observeDates(),
    ) { systems, items, accepted, dates ->
        val itemsBy = items.groupBy { it.systemId }
        val acceptedBy = accepted.groupBy({ it.itemId }, { it.labelId }).mapValues { it.value.toSet() }
        val datesBy = dates.groupBy { it.systemId }
        systems.map { it.toDomain(itemsBy[it.id].orEmpty(), acceptedBy, datesBy[it.id].orEmpty()) }
    }

    suspend fun create(system: DailySystem): DailySystem {
        db.withTransaction {
            dao.insert(system.toEntity())
            system.dates.forEach { dao.insertDate(SystemDateEntity(system.id, it)) }
            dao.insertItems(system.items.mapIndexed { i, it ->
                SystemItemEntity(it.id, system.id, it.kind, it.title, it.labelId, i, it.slotCount)
            })
            dao.insertAccepted(system.items.flatMap { it.acceptedLabelIds.map { l -> SlotAcceptedLabelEntity(it.id, l) } })
        }
        return system
    }

    suspend fun createBlank(labelId: String?): DailySystem =
        create(DailySystem(id = newId(), name = "", labelId = labelId, createdAt = clock()))

    suspend fun setName(id: String, name: String) = dao.setName(id, name)
    suspend fun setLabel(id: String, labelId: String?) = dao.setLabel(id, labelId)
    suspend fun setActive(id: String, active: Boolean) = dao.setActive(id, active)
    suspend fun setOrder(id: String, order: ItemOrder) = dao.setOrder(id, order)
    suspend fun setScheduleType(id: String, type: ScheduleType) = dao.setScheduleType(id, type)
    suspend fun setReminderTime(id: String, time: LocalTime?) = dao.setReminderTime(id, time)

    suspend fun toggleWeekday(id: String, day: DayOfWeek) = db.withTransaction {
        val s = dao.get(id) ?: return@withTransaction
        val days = WeekdayMask.decode(s.weekdaysMask).toMutableSet()
        if (!days.add(day)) days.remove(day)
        dao.setWeekdays(id, WeekdayMask.encode(days))
    }

    suspend fun toggleMonthDay(id: String, day: Int) = db.withTransaction {
        val s = dao.get(id) ?: return@withTransaction
        val days = MonthDaysCsv.decode(s.monthDays).toMutableSet()
        if (!days.add(day)) days.remove(day)
        dao.setMonthDays(id, MonthDaysCsv.encode(days))
    }

    suspend fun addDate(id: String, date: LocalDate) = dao.insertDate(SystemDateEntity(id, date))
    suspend fun removeDate(id: String, date: LocalDate) = dao.deleteDate(SystemDateEntity(id, date))

    suspend fun addItem(systemId: String, kind: ItemKind, labelId: String?): String = db.withTransaction {
        val id = newId()
        dao.insertItem(SystemItemEntity(id, systemId, kind, "", labelId, dao.itemsOf(systemId).size, 1))
        id
    }

    suspend fun removeItem(systemId: String, itemId: String) = dao.deleteItemAndCompact(systemId, itemId)
    suspend fun moveItem(systemId: String, itemId: String, delta: Int) = dao.moveItem(systemId, itemId, delta)
    suspend fun moveItemTo(systemId: String, itemId: String, toIndex: Int) = dao.moveItemTo(systemId, itemId, toIndex)
    suspend fun setItemTitle(itemId: String, title: String) = dao.setItemTitle(itemId, title)
    suspend fun setItemLabel(itemId: String, labelId: String?) = dao.setItemLabel(itemId, labelId)
    suspend fun setItemKind(itemId: String, kind: ItemKind) = dao.setItemKind(itemId, kind)
    suspend fun setSlotCount(itemId: String, count: Int) = dao.setSlotCount(itemId, count.coerceIn(1, 9))

    suspend fun toggleAccepted(itemId: String, labelId: String) = db.withTransaction {
        if (dao.isAccepted(itemId, labelId) > 0) dao.deleteAccepted(itemId, labelId)
        else dao.insertAccepted(listOf(SlotAcceptedLabelEntity(itemId, labelId)))
    }

    /** Deleting a system clears tasks that pointed at it via runSystemId. */
    suspend fun delete(id: String) = db.withTransaction {
        db.taskDao().clearRunSystem(id)
        dao.delete(id)
    }

    /** A new system closed with no name and no items is discarded. */
    suspend fun deleteIfBlank(id: String) = db.withTransaction {
        val s = dao.get(id) ?: return@withTransaction
        if (s.name.isBlank() && dao.itemsOf(id).isEmpty()) delete(id)
    }

    fun checkedSteps(date: LocalDate): Flow<Set<StepKey>> =
        steps.observeOn(date).map { rows -> rows.map { StepKey(it.systemId, it.itemId) }.toSet() }

    suspend fun toggleStep(systemId: String, itemId: String, date: LocalDate) = db.withTransaction {
        if (steps.count(systemId, itemId, date) > 0) steps.delete(systemId, itemId, date)
        else steps.insert(StepCompletionEntity(systemId, itemId, date))
    }
}

class NoteRepository(private val db: AppDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao = db.noteDao()

    val notes: Flow<List<Note>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observe(id: String): Flow<Note?> = dao.observe(id).map { it?.toDomain() }

    suspend fun create(title: String = "", body: String = "", labelId: String? = null, isExample: Boolean = false): Note {
        val now = clock()
        val n = Note(newId(), title, body, labelId, now, now, isExample)
        dao.insert(n.toEntity())
        return n
    }

    suspend fun update(id: String, change: (Note) -> Note) = db.withTransaction {
        val cur = dao.get(id)?.toDomain() ?: return@withTransaction
        val next = change(cur)
        if (next != cur) dao.update(next.copy(updatedAt = clock()).toEntity())
    }

    suspend fun setTitle(id: String, title: String) = update(id) { it.copy(title = title) }
    suspend fun setBody(id: String, body: String) = update(id) { it.copy(body = body) }
    suspend fun setLabel(id: String, labelId: String?) = update(id) { it.copy(labelId = labelId) }

    suspend fun delete(id: String) = dao.delete(id)

    /** Empty notes (no title, no body) are deleted when the editor closes. */
    suspend fun deleteIfBlank(id: String) = db.withTransaction {
        val n = dao.get(id) ?: return@withTransaction
        if (n.title.isBlank() && n.body.isBlank()) dao.delete(id)
    }
}
