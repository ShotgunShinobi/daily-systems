package com.dailysystems.app.ui.systems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailysystems.app.AppContainer
import com.dailysystems.app.domain.DailySystem
import com.dailysystems.app.domain.DayPlanner
import com.dailysystems.app.domain.HabitStats
import com.dailysystems.app.domain.SystemHistory
import com.dailysystems.app.domain.WeekReview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.domain.SystemItem
import com.dailysystems.app.ui.Format
import com.dailysystems.app.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SystemSummary(
    val system: DailySystem,
    val schedule: String,
    val steps: Int,
    val slots: Int,
    /** Items in display order, for the row of pips. */
    val pips: List<SystemItem>,
)

data class SystemsUiState(
    val loaded: Boolean = false,
    val systems: List<SystemSummary> = emptyList(),
    val labels: Map<String, PriorityLabel> = emptyMap(),
    val histories: Map<String, SystemHistory> = emptyMap(),
    val week: WeekReview? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SystemsViewModel(c: AppContainer) : ViewModel() {
    private val history = c.today.flatMapLatest { c.habitCompletions(it) }

    val state: StateFlow<SystemsUiState> = combine(c.labels.labels, c.systems.systems, c.tasks.tasks, c.today, history) { labels, systems, tasks, today, done ->
        val labelMap = labels.associateBy { it.id }
        SystemsUiState(
            loaded = true,
            labels = labelMap,
            systems = systems.sortedWith(DayPlanner.systemComparator(labelMap)).map { s ->
                SystemSummary(
                    system = s,
                    schedule = Format.scheduleSummary(s, tasks, today),
                    steps = s.items.count { it.kind == ItemKind.STEP },
                    slots = s.items.count { it.kind == ItemKind.SLOT },
                    pips = DayPlanner.orderedItems(s, labelMap),
                )
            },
            histories = systems.mapNotNull { HabitStats.history(it, tasks, done, today) }.associateBy { it.system.id },
            week = HabitStats.weekReview(HabitStats.weekStart(today), systems, tasks, done, today),
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, SystemsUiState())
}
