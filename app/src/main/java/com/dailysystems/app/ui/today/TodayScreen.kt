package com.dailysystems.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailysystems.app.domain.DailySystem
import com.dailysystems.app.domain.DayPlanner
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.ItemOrder
import com.dailysystems.app.domain.PlannedRow
import com.dailysystems.app.domain.PlannedSystem
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.domain.ScheduleType
import com.dailysystems.app.domain.StepKey
import com.dailysystems.app.domain.SystemItem
import com.dailysystems.app.domain.Task
import com.dailysystems.app.ui.Format
import com.dailysystems.app.ui.LocalViewModelFactory
import com.dailysystems.app.ui.PickTarget
import com.dailysystems.app.ui.Sheet
import com.dailysystems.app.ui.SheetsViewModel
import com.dailysystems.app.ui.components.AppButton
import com.dailysystems.app.ui.components.AppCard
import com.dailysystems.app.ui.components.AppIcons
import com.dailysystems.app.ui.components.ButtonKind
import com.dailysystems.app.ui.components.CheckCircle
import com.dailysystems.app.ui.components.EmptyCard
import com.dailysystems.app.ui.components.ExampleTag
import com.dailysystems.app.ui.components.IconAction
import com.dailysystems.app.ui.components.LabelChip
import com.dailysystems.app.ui.components.ProgressBar
import com.dailysystems.app.ui.components.RowDivider
import com.dailysystems.app.ui.components.ScreenHeader
import com.dailysystems.app.ui.components.SectionHeader
import com.dailysystems.app.ui.components.TaskRow
import com.dailysystems.app.ui.components.TextStyles
import com.dailysystems.app.ui.components.dashedBorder
import com.dailysystems.app.ui.components.labelColor
import com.dailysystems.app.ui.theme.AppTheme
import com.dailysystems.app.ui.theme.DailySystemsTheme
import java.time.LocalDate

/** Everything the Today screen can ask for; keeps [TodayScreen] previewable with fake data. */
data class TodayActions(
    val selectDay: (LocalDate) -> Unit = {},
    val shiftWeek: (Long) -> Unit = {},
    val goToday: () -> Unit = {},
    val toggleTask: (String) -> Unit = {},
    val toggleStep: (systemId: String, itemId: String) -> Unit = { _, _ -> },
    val openTask: (String) -> Unit = {},
    val pickLabel: (PickTarget) -> Unit = {},
    val editSystem: (String) -> Unit = {},
    val fillSlot: (Set<String>) -> Unit = {},
    val newTask: () -> Unit = {},
    val newSystem: () -> Unit = {},
    val loadStarter: () -> Unit = {},
    val openLabels: () -> Unit = {},
    val openSystemsTab: () -> Unit = {},
)

@Composable
fun TodayRoute(sheets: SheetsViewModel, openSystemsTab: () -> Unit) {
    val vm: TodayViewModel = viewModel(factory = LocalViewModelFactory.current)
    val state by vm.state.collectAsStateWithLifecycle()
    TodayScreen(
        state,
        TodayActions(
            selectDay = vm::selectDay,
            shiftWeek = vm::shiftWeek,
            goToday = vm::goToday,
            toggleTask = vm::toggleTask,
            toggleStep = vm::toggleStep,
            openTask = { sheets.open(Sheet.TaskEditor(it, contextDate = state.date)) },
            pickLabel = { sheets.open(Sheet.LabelPicker(it)) },
            editSystem = { sheets.open(Sheet.SystemEditor(it)) },
            fillSlot = { sheets.fillSlot(state.date, it) },
            newTask = { sheets.newTask(dueDate = state.date, contextDate = state.date) },
            newSystem = sheets::newSystem,
            loadStarter = sheets::loadStarterExamples,
            openLabels = { sheets.open(Sheet.Labels) },
            openSystemsTab = openSystemsTab,
        ),
    )
}

@Composable
fun TodayScreen(state: TodayUiState, actions: TodayActions) {
    val c = AppTheme.colors
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().testTag("today-list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = Format.relativeDay(state.date, state.today),
                    eyebrow = Format.longDate(state.date),
                    onLabels = actions.openLabels,
                    actions = {
                        if (state.date != state.today) AppButton("Today", actions.goToday, small = true)
                    },
                )
            }
            item(key = "week") { WeekStrip(state, actions) }
            item(key = "progress") {
                Row(Modifier.padding(top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressBar(state.progress.fraction, Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    Text("${state.progress.done}/${state.progress.total} done", style = AppTheme.type.mono, color = c.muted)
                }
            }

            if (state.loaded && state.isEmpty) {
                item(key = "empty") {
                    EmptyCard(
                        title = "Build your first system",
                        body = "A system is a routine you run on chosen days: fixed steps plus open slots. Your to-dos plug " +
                            "into the slots by priority, so the plan refills itself as you add and finish things.",
                        modifier = Modifier.padding(top = 14.dp),
                    ) {
                        AppButton("New system", actions.newSystem, kind = ButtonKind.Primary, icon = AppIcons.Plus)
                        AppButton("Load starter examples", actions.loadStarter)
                    }
                }
                return@LazyColumn
            }

            item(key = "sys-h") { SectionHeader("Systems running", state.plan.systems.size) }
            if (state.plan.systems.isEmpty()) {
                item(key = "no-sys") {
                    EmptyCard(null, "No system is scheduled for this day.") {
                        AppButton("Open systems", actions.openSystemsTab)
                    }
                }
            }
            items(state.plan.systems, key = { "sys-" + it.system.id }) { ps ->
                SystemCard(ps, state, actions, Modifier.padding(bottom = 12.dp))
            }

            if (state.plan.alsoDue.isNotEmpty()) {
                item(key = "due-h") { SectionHeader("Also due", state.plan.alsoDue.size) }
                item(key = "due") { TaskList(state.plan.alsoDue, state, actions) }
            }
            if (state.plan.nextUp.isNotEmpty()) {
                item(key = "next-h") { SectionHeader("Next up, no date", state.plan.nextUp.size) }
                item(key = "next") { TaskList(state.plan.nextUpTop, state, actions) }
            }
        }

        FloatingActionButton(
            onClick = actions.newTask,
            containerColor = c.accent,
            contentColor = c.onAccent,
            shape = RoundedCornerShape(18.dp),
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).semantics { contentDescription = "Add a to-do" },
        ) { Icon(AppIcons.Plus, null, Modifier.size(24.dp)) }
    }
}

@Composable
private fun WeekStrip(state: TodayUiState, actions: TodayActions) {
    val c = AppTheme.colors
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconAction(AppIcons.Left, "Previous week", { actions.shiftWeek(-1) }, Modifier.width(36.dp))
        state.week.forEach { day ->
            val sel = day.date == state.date
            val isToday = day.date == state.today
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(shape)
                    .background(if (sel) c.ink else Color.Transparent)
                    .border(1.dp, if (isToday && !sel) c.line else Color.Transparent, shape)
                    .clickable(role = Role.Tab) { actions.selectDay(day.date) }
                    .semantics {
                        selected = sel
                        contentDescription = Format.weekdayLong(day.date) + ", " + Format.shortDate(day.date) +
                            if (day.hasSystems) ", systems running" else ""
                    }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    Format.weekdayNarrow(day.date.dayOfWeek).uppercase(),
                    style = AppTheme.type.sectionLabel.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Medium),
                    color = if (sel) c.background else c.muted,
                )
                Text("${day.date.dayOfMonth}", style = AppTheme.type.mono.copy(fontSize = 15.sp), color = if (sel) c.background else c.ink)
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.size(4.dp).clip(CircleShape)
                        .background(if (!day.hasSystems) Color.Transparent else if (sel) c.background else c.accent),
                )
            }
        }
        IconAction(AppIcons.Right, "Next week", { actions.shiftWeek(1) }, Modifier.width(36.dp))
    }
}

@Composable
private fun TaskList(tasks: List<Task>, state: TodayUiState, actions: TodayActions) {
    AppCard {
        tasks.forEachIndexed { i, t ->
            if (i > 0) RowDivider()
            TaskRow(
                task = t,
                label = state.labels[t.labelId],
                runsSystemName = t.runSystemId?.let { state.systemNames[it] },
                referenceDate = state.date,
                onToggle = { actions.toggleTask(t.id) },
                onOpen = { actions.openTask(t.id) },
                onPickLabel = { actions.pickLabel(PickTarget.ForTask(t.id)) },
            )
        }
    }
}

@Composable
private fun SystemCard(ps: PlannedSystem, state: TodayUiState, actions: TodayActions, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val s = ps.system
    AppCard(modifier.fillMaxWidth().testTag("system-card")) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.name.ifBlank { "Untitled system" }, style = AppTheme.type.cardTitle, color = c.ink)
                Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (ps.runningForTarget) "Running for a target date" else Format.scheduleSummary(s, state.allTasks, state.today),
                        style = AppTheme.type.monoSmall, color = c.muted, modifier = Modifier.weight(1f, fill = false),
                    )
                    if (s.isExample) { Spacer(Modifier.width(6.dp)); ExampleTag() }
                }
            }
            LabelChip(state.labels[s.labelId], { actions.pickLabel(PickTarget.ForSystem(s.id)) })
            IconAction(AppIcons.Edit, "Edit system", { actions.editSystem(s.id) })
        }
        if (ps.rows.isEmpty()) {
            Text(
                "No items yet. Edit the system to add steps or slots.",
                style = TextStyles.hint, color = c.muted, modifier = Modifier.padding(14.dp),
            )
        }
        ps.rows.forEachIndexed { i, row ->
            if (i > 0) RowDivider()
            when (row) {
                is PlannedRow.Step -> StepRow(s.id, row.item, StepKey(s.id, row.item.id) in state.checkedSteps, state, actions)
                is PlannedRow.Slot -> SlotBlock(row, state, actions)
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun StepRow(systemId: String, item: SystemItem, checked: Boolean, state: TodayUiState, actions: TodayActions) {
    val c = AppTheme.colors
    val label = state.labels[item.labelId]
    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).padding(start = 4.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        CheckCircle(checked, labelColor(label), { actions.toggleStep(systemId, item.id) }, if (checked) "Mark step not done" else "Mark step done")
        Text(
            item.title.ifBlank { "Untitled step" },
            style = TextStyles.rowTitle,
            color = if (checked) c.muted else c.ink,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
        )
        LabelChip(label, { actions.pickLabel(PickTarget.ForItem(systemId, item.id)) })
    }
}

/** A slot drawn as a socket: jack, vertical rule, plugged-in to-dos, dashed open positions. */
@Composable
private fun SlotBlock(row: PlannedRow.Slot, state: TodayUiState, actions: TodayActions) {
    val c = AppTheme.colors
    val item = row.item
    val takes = item.acceptedLabelIds
        .mapNotNull { state.labels[it] }
        .sortedBy { it.rank }
        .joinToString(", ") { it.name }
        .ifEmpty { "any priority" }
    Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Jack()
            Spacer(Modifier.width(8.dp))
            Text(item.title.ifBlank { "Slot" }, style = TextStyles.rowTitle.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp), color = c.ink, modifier = Modifier.weight(1f))
            Text("${row.tasks.size}/${row.capacity}", style = AppTheme.type.monoSmall, color = c.muted)
        }
        Text("takes $takes", style = TextStyles.rowSub, color = c.muted, modifier = Modifier.padding(start = 22.dp, top = 2.dp))
        Row(Modifier.height(IntrinsicSize.Min).padding(start = 6.dp, top = 8.dp, bottom = 4.dp)) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(c.line))
            Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                row.tasks.forEach { t ->
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(c.surface2)) {
                        TaskRow(
                            task = t,
                            label = state.labels[t.labelId],
                            runsSystemName = t.runSystemId?.let { state.systemNames[it] },
                            referenceDate = state.date,
                            onToggle = { actions.toggleTask(t.id) },
                            onOpen = { actions.openTask(t.id) },
                            onPickLabel = { actions.pickLabel(PickTarget.ForTask(t.id)) },
                        )
                    }
                }
                repeat(row.openPositions) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .dashedBorder(c.socket, 10.dp, 1.5.dp, dash = 5.dp)
                            .clickable(role = Role.Button) { actions.fillSlot(item.acceptedLabelIds) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text("Open slot · add a to-do ($takes)", style = TextStyles.rowSub.copy(fontSize = 13.sp), color = c.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun Jack() {
    val ink = AppTheme.colors.ink
    Box(Modifier.size(14.dp).border(2.dp, ink, CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.size(4.dp).clip(CircleShape).background(ink))
    }
}

// --- previews -------------------------------------------------------------------------------

internal object TodayPreviewData {
    val labels = listOf(
        PriorityLabel("must", "Must do", 0xFFD7263D.toInt(), 0),
        PriorityLabel("should", "Should do", 0xFFF08A24.toInt(), 1),
        PriorityLabel("could", "Could do", 0xFF2F7FD8.toInt(), 2),
        PriorityLabel("someday", "Someday", 0xFF7A8794.toInt(), 3),
    )
    val today: LocalDate = LocalDate.of(2026, 11, 3)
    val systems = listOf(
        DailySystem(
            "m", "Morning launch", "must", itemOrder = ItemOrder.PRIORITY, scheduleType = ScheduleType.DAILY,
            items = listOf(
                SystemItem("s1", ItemKind.STEP, "Water and a 10-minute walk", "should", 0),
                SystemItem("s2", ItemKind.STEP, "Review today's plan", "must", 1),
                SystemItem("s3", ItemKind.SLOT, "Top priorities", "must", 2, 2, setOf("must", "should")),
            ),
        ),
    )
    val tasks = listOf(
        Task("t1", "Pay electricity bill", "must", dueDate = today, createdAt = 1),
        Task("t2", "Reply to pending emails", "should", createdAt = 2, notes = "Mostly the invoices"),
        Task("t3", "Book dentist appointment", "could", createdAt = 3),
        Task("t4", "Renew passport", "could", dueDate = today.minusDays(2), createdAt = 4),
    )

    fun state(): TodayUiState {
        val plan = DayPlanner.planDay(today, systems, tasks, labels)
        val checked = setOf(StepKey("m", "s2"))
        return TodayUiState(
            loaded = true, today = today, date = today,
            week = (0L until 7L).map { WeekDay(today.minusDays(1).plusDays(it), true) },
            plan = plan, progress = plan.progress(checked), checkedSteps = checked,
            labels = labels.associateBy { it.id }, systemNames = mapOf("m" to "Morning launch"), allTasks = tasks,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDF0F4, heightDp = 900)
@Composable
private fun TodayPreview() = DailySystemsTheme(darkTheme = false) {
    Box(Modifier.background(AppTheme.colors.background)) { TodayScreen(TodayPreviewData.state(), TodayActions()) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E131A, heightDp = 900)
@Composable
private fun TodayDarkPreview() = DailySystemsTheme(darkTheme = true) {
    Box(Modifier.background(AppTheme.colors.background)) { TodayScreen(TodayPreviewData.state(), TodayActions()) }
}
