package com.dailysystems.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailysystems.app.AppContainer
import com.dailysystems.app.domain.DailySystem
import com.dailysystems.app.domain.DayPlan
import com.dailysystems.app.domain.DayPlanner
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.domain.Progress
import com.dailysystems.app.domain.StepKey
import com.dailysystems.app.domain.Task
import com.dailysystems.app.ui.WhileUiSubscribed
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class WeekDay(val date: LocalDate, val hasSystems: Boolean)

data class TodayUiState(
    val loaded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val date: LocalDate = LocalDate.now(),
    val week: List<WeekDay> = emptyList(),
    val plan: DayPlan = DayPlan(LocalDate.now(), emptyList(), emptyList(), emptyList()),
    val progress: Progress = Progress(0, 0),
    val checkedSteps: Set<StepKey> = emptySet(),
    val labels: Map<String, PriorityLabel> = emptyMap(),
    val systemNames: Map<String, String> = emptyMap(),
    val allTasks: List<Task> = emptyList(),
    /** No systems and no tasks at all: show the onboarding empty state. */
    val isEmpty: Boolean = false,
)

private data class Inputs(
    val labels: List<PriorityLabel>,
    val systems: List<DailySystem>,
    val tasks: List<Task>,
    val today: LocalDate,
    val date: LocalDate,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val c: AppContainer) : ViewModel() {

    /** null = follow "today", including across midnight. */
    private val pinned = MutableStateFlow<LocalDate?>(null)

    private val inputs = combine(c.labels.labels, c.systems.systems, c.tasks.tasks, c.today, pinned) { l, s, t, today, p ->
        Inputs(l, s, t, today, p ?: today)
    }

    val state: StateFlow<TodayUiState> = inputs
        .flatMapLatest { i -> c.systems.checkedSteps(i.date).map { checked -> build(i, checked) } }
        .stateIn(viewModelScope, WhileUiSubscribed, TodayUiState())

    private fun build(i: Inputs, checked: Set<StepKey>): TodayUiState {
        val labelMap = i.labels.associateBy { it.id }
        val plan = DayPlanner.planDay(i.date, i.systems, i.tasks, i.labels)
        val monday = i.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val week = (0L until 7L).map { k ->
            val d = monday.plusDays(k)
            WeekDay(d, DayPlanner.activeSystems(i.systems, i.tasks, labelMap, d).isNotEmpty())
        }
        return TodayUiState(
            loaded = true,
            today = i.today,
            date = i.date,
            week = week,
            plan = plan,
            progress = plan.progress(checked),
            checkedSteps = checked,
            labels = labelMap,
            systemNames = i.systems.associate { it.id to it.name.ifBlank { "Untitled system" } },
            allTasks = i.tasks,
            isEmpty = i.systems.isEmpty() && i.tasks.isEmpty(),
        )
    }

    fun selectDay(d: LocalDate) {
        pinned.value = if (d == c.today.value) null else d
    }

    fun shiftWeek(weeks: Long) = selectDay(state.value.date.plusWeeks(weeks))

    fun goToday() {
        pinned.value = null
    }

    /** Checking a to-do on Today counts it as done on the day being viewed. */
    fun toggleTask(id: String) {
        val d = state.value.date
        c.write { c.tasks.toggleDone(id, d) }
    }

    fun toggleStep(systemId: String, itemId: String) {
        val d = state.value.date
        c.write { c.systems.toggleStep(systemId, itemId, d) }
    }
}
