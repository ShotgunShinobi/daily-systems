package com.dailysystems.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailysystems.app.AppContainer
import com.dailysystems.app.domain.DailySystem
import com.dailysystems.app.domain.DayPlanner
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.ItemOrder
import com.dailysystems.app.domain.Note
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.domain.RepeatRule
import com.dailysystems.app.domain.ScheduleType
import com.dailysystems.app.domain.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The one bottom sheet that can be open at a time. */
sealed interface Sheet {
    /** [contextDate]: the day the editor was opened from (Today), used when marking done. */
    data class TaskEditor(val id: String, val isNew: Boolean = false, val contextDate: LocalDate? = null) : Sheet
    data class SystemEditor(val id: String, val isNew: Boolean = false) : Sheet
    data class NoteEditor(val id: String, val isNew: Boolean = false) : Sheet
    data object Labels : Sheet
    data class LabelPicker(val target: PickTarget) : Sheet
}

sealed interface PickTarget {
    data class ForTask(val taskId: String) : PickTarget
    data class ForSystem(val systemId: String) : PickTarget
    data class ForItem(val systemId: String, val itemId: String) : PickTarget
    data class ForNote(val noteId: String) : PickTarget
}

/**
 * Owns the sheet stack (one deep) and every editor action. Editors save immediately through
 * repositories; all writes go through [AppContainer.write] so they stay ordered.
 */
class SheetsViewModel(private val c: AppContainer) : ViewModel() {

    private val _sheet = MutableStateFlow<Sheet?>(null)
    val sheet: StateFlow<Sheet?> = _sheet.asStateFlow()

    val labels: StateFlow<List<PriorityLabel>> = c.labels.labels.stateIn(viewModelScope, WhileUiSubscribed, emptyList())
    val systems: StateFlow<List<DailySystem>> = c.systems.systems.stateIn(viewModelScope, WhileUiSubscribed, emptyList())
    val tasks: StateFlow<List<Task>> = c.tasks.tasks.stateIn(viewModelScope, WhileUiSubscribed, emptyList())
    val notes: StateFlow<List<Note>> = c.notes.notes.stateIn(viewModelScope, WhileUiSubscribed, emptyList())
    val today: StateFlow<LocalDate> get() = c.today

    fun observeTask(id: String): Flow<Task?> = c.tasks.observe(id)
    fun observeNote(id: String): Flow<Note?> = c.notes.observe(id)
    fun observeSystem(id: String): Flow<DailySystem?> = c.systems.systems.map { list -> list.firstOrNull { it.id == id } }

    // --- open / close --------------------------------------------------------------------

    fun open(next: Sheet) {
        cleanUp(_sheet.value)
        _sheet.value = next
    }

    fun close() {
        cleanUp(_sheet.value)
        _sheet.value = null
    }

    /** Discard blank things on close (SPEC §4.4, §4.5). */
    private fun cleanUp(s: Sheet?) = when (s) {
        is Sheet.TaskEditor -> if (s.isNew) c.write { c.tasks.deleteIfBlank(s.id) } else Unit
        is Sheet.SystemEditor -> if (s.isNew) c.write { c.systems.deleteIfBlank(s.id) } else Unit
        is Sheet.NoteEditor -> c.write { c.notes.deleteIfBlank(s.id) }
        else -> Unit
    }

    private fun defaultTaskLabel(): String? = labels.value.let { (it.getOrNull(1) ?: it.firstOrNull())?.id }

    fun newTask(dueDate: LocalDate?, labelId: String? = defaultTaskLabel(), contextDate: LocalDate? = null) {
        viewModelScope.launch {
            val t = c.writeAwait { c.tasks.create(labelId = labelId, dueDate = dueDate) }
            open(Sheet.TaskEditor(t.id, isNew = true, contextDate = contextDate))
        }
    }

    /** Tapping an open slot: a to-do dated [date] with the slot's first accepted label. */
    fun fillSlot(date: LocalDate, acceptedLabelIds: Set<String>) {
        val byRank = labels.value.associateBy { it.id }
        val first = acceptedLabelIds.minByOrNull { DayPlanner.rank(it, byRank) }
        newTask(dueDate = date, labelId = first ?: defaultTaskLabel(), contextDate = date)
    }

    fun newSystem() {
        viewModelScope.launch {
            val s = c.writeAwait { c.systems.createBlank(labels.value.firstOrNull()?.id) }
            open(Sheet.SystemEditor(s.id, isNew = true))
        }
    }

    fun newNote() {
        viewModelScope.launch {
            val n = c.writeAwait { c.notes.create() }
            open(Sheet.NoteEditor(n.id, isNew = true))
        }
    }

    fun loadStarterExamples() = c.write { c.starterExamples.load(c.today.value) }

    // --- label picker ---------------------------------------------------------------------

    fun pick(target: PickTarget, labelId: String?) {
        c.write {
            when (target) {
                is PickTarget.ForTask -> c.tasks.setLabel(target.taskId, labelId)
                is PickTarget.ForSystem -> c.systems.setLabel(target.systemId, labelId)
                is PickTarget.ForItem -> c.systems.setItemLabel(target.itemId, labelId)
                is PickTarget.ForNote -> c.notes.setLabel(target.noteId, labelId)
            }
        }
        close()
    }


    // --- to-do editor ---------------------------------------------------------------------

    fun setTaskTitle(id: String, v: String) = c.write { c.tasks.setTitle(id, v) }
    fun setTaskLabel(id: String, v: String?) = c.write { c.tasks.setLabel(id, v) }
    fun setTaskDue(id: String, v: LocalDate?) = c.write { c.tasks.setDueDate(id, v) }
    fun setTaskRunSystem(id: String, v: String?) = c.write { c.tasks.setRunSystem(id, v) }
    fun setTaskNotes(id: String, v: String) = c.write { c.tasks.setNotes(id, v) }
    fun toggleTask(id: String, on: LocalDate) = c.write { c.tasks.toggleDone(id, on) }
    fun setTaskReminder(id: String, t: LocalTime?) = c.write { c.tasks.setReminderTime(id, t) }
    fun setTaskRepeat(id: String, r: RepeatRule?) = c.write { c.tasks.setRepeat(id, r) }

    fun deleteTask(id: String) {
        _sheet.value = null
        c.write { c.tasks.delete(id) }
    }

    // --- note editor ----------------------------------------------------------------------

    fun setNoteTitle(id: String, v: String) = c.write { c.notes.setTitle(id, v) }
    fun setNoteBody(id: String, v: String) = c.write { c.notes.setBody(id, v) }
    fun setNoteLabel(id: String, v: String?) = c.write { c.notes.setLabel(id, v) }

    fun deleteNote(id: String) {
        _sheet.value = null
        c.write { c.notes.delete(id) }
    }

    // --- system editor --------------------------------------------------------------------

    fun setSystemName(id: String, v: String) = c.write { c.systems.setName(id, v) }
    fun setSystemLabel(id: String, v: String?) = c.write { c.systems.setLabel(id, v) }
    fun setScheduleType(id: String, v: ScheduleType) = c.write { c.systems.setScheduleType(id, v) }
    fun toggleWeekday(id: String, d: DayOfWeek) = c.write { c.systems.toggleWeekday(id, d) }
    fun toggleMonthDay(id: String, d: Int) = c.write { c.systems.toggleMonthDay(id, d) }
    fun addDate(id: String, d: LocalDate) = c.write { c.systems.addDate(id, d) }
    fun removeDate(id: String, d: LocalDate) = c.write { c.systems.removeDate(id, d) }
    fun setOrder(id: String, v: ItemOrder) = c.write { c.systems.setOrder(id, v) }
    fun setActive(id: String, v: Boolean) = c.write { c.systems.setActive(id, v) }
    fun setSystemReminder(id: String, t: LocalTime?) = c.write { c.systems.setReminderTime(id, t) }
    fun moveItemTo(systemId: String, itemId: String, toIndex: Int) = c.write { c.systems.moveItemTo(systemId, itemId, toIndex) }

    /** Returns the new item's id so the editor can focus its title. */
    suspend fun addItem(systemId: String, kind: ItemKind): String =
        c.writeAwait { c.systems.addItem(systemId, kind, defaultTaskLabel()) }

    fun removeItem(systemId: String, itemId: String) = c.write { c.systems.removeItem(systemId, itemId) }
    fun moveItem(systemId: String, itemId: String, delta: Int) = c.write { c.systems.moveItem(systemId, itemId, delta) }
    fun setItemTitle(itemId: String, v: String) = c.write { c.systems.setItemTitle(itemId, v) }
    fun setItemKind(itemId: String, v: ItemKind) = c.write { c.systems.setItemKind(itemId, v) }
    fun setItemLabel(itemId: String, v: String?) = c.write { c.systems.setItemLabel(itemId, v) }
    fun toggleAccepted(itemId: String, labelId: String) = c.write { c.systems.toggleAccepted(itemId, labelId) }
    fun setSlotCount(itemId: String, n: Int) = c.write { c.systems.setSlotCount(itemId, n) }

    fun deleteSystem(id: String) {
        _sheet.value = null
        c.write { c.systems.delete(id) }
    }

    // --- labels manager -------------------------------------------------------------------

    fun renameLabel(id: String, name: String) = c.write { c.labels.rename(id, name) }
    fun setLabelColor(id: String, color: Int) = c.write { c.labels.setColor(id, color) }
    fun moveLabel(id: String, delta: Int) = c.write { c.labels.move(id, delta) }
    fun moveLabelTo(id: String, toIndex: Int) = c.write { c.labels.moveTo(id, toIndex) }
    fun deleteLabel(id: String) = c.write { c.labels.delete(id) }
    suspend fun addLabel(): String = c.writeAwait { c.labels.add().id }
}
