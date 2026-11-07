package com.dailysystems.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailysystems.app.AppContainer
import com.dailysystems.app.domain.DayPlanner
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.domain.Task
import com.dailysystems.app.ui.WhileUiSubscribed
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class TaskFilter { OPEN, DONE, ALL }

data class TaskGroup(val label: PriorityLabel?, val tasks: List<Task>)

data class TasksUiState(
    val loaded: Boolean = false,
    val filter: TaskFilter = TaskFilter.OPEN,
    val groups: List<TaskGroup> = emptyList(),
    val labels: List<PriorityLabel> = emptyList(),
    val labelMap: Map<String, PriorityLabel> = emptyMap(),
    val systemNames: Map<String, String> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
    val quickLabel: PriorityLabel? = null,
    val quickDate: LocalDate? = null,
)

private data class QuickAdd(val labelId: String?, val date: LocalDate?)

class TasksViewModel(private val c: AppContainer) : ViewModel() {
    private val filter = MutableStateFlow(TaskFilter.OPEN)
    private val quick = MutableStateFlow(QuickAdd(null, null))

    val state: StateFlow<TasksUiState> = combine(
        c.labels.labels, c.tasks.tasks, c.systems.systems, combine(filter, quick, c.today, ::Triple),
    ) { labels, tasks, systems, (f, q, today) ->
        val labelMap = labels.associateBy { it.id }
        var list = tasks.filter {
            when (f) {
                TaskFilter.OPEN -> !it.done
                TaskFilter.DONE -> it.done
                TaskFilter.ALL -> true
            }
        }.sortedWith(DayPlanner.taskComparator(labelMap))
        if (f == TaskFilter.DONE) list = list.sortedByDescending { it.doneAt ?: 0L }
        // Grouped by label in rank order, then "No label" (includes unknown label ids).
        val groups = labels.map { l -> TaskGroup(l, list.filter { it.labelId == l.id }) } +
            TaskGroup(null, list.filter { it.labelId == null || it.labelId !in labelMap })
        TasksUiState(
            loaded = true,
            filter = f,
            groups = groups.filter { it.tasks.isNotEmpty() },
            labels = labels,
            labelMap = labelMap,
            systemNames = systems.associate { it.id to it.name.ifBlank { "Untitled system" } },
            today = today,
            quickLabel = labelMap[q.labelId] ?: labels.getOrNull(1) ?: labels.firstOrNull(),
            quickDate = q.date,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, TasksUiState())

    fun setFilter(f: TaskFilter) {
        filter.value = f
    }

    /** Tap-to-cycle label chip in quick add. */
    fun cycleQuickLabel() {
        val ls = state.value.labels
        if (ls.isEmpty()) return
        val i = ls.indexOfFirst { it.id == state.value.quickLabel?.id }
        quick.value = quick.value.copy(labelId = ls[(i + 1) % ls.size].id)
    }

    fun setQuickDate(d: LocalDate?) {
        quick.value = quick.value.copy(date = d)
    }

    /** Returns false (and adds nothing) for a blank title. */
    fun add(title: String): Boolean {
        val t = title.trim()
        if (t.isEmpty()) return false
        val s = state.value
        c.write { c.tasks.create(title = t, labelId = s.quickLabel?.id, dueDate = s.quickDate) }
        return true
    }

    fun toggle(id: String) {
        val today = c.today.value
        c.write { c.tasks.toggleDone(id, today) }
    }
}
