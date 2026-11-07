package com.dailysystems.app.ui.editors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailysystems.app.domain.DailySystem
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.ItemOrder
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.domain.ScheduleType
import com.dailysystems.app.domain.SystemItem
import com.dailysystems.app.ui.Format
import com.dailysystems.app.ui.Sheet
import com.dailysystems.app.ui.SheetsViewModel
import com.dailysystems.app.ui.components.AppButton
import com.dailysystems.app.ui.components.AppDatePickerDialog
import com.dailysystems.app.ui.components.AppIcons
import com.dailysystems.app.ui.components.AppTextField
import com.dailysystems.app.ui.components.DeleteButton
import com.dailysystems.app.ui.components.HistoryGrid
import com.dailysystems.app.ui.components.StatTile
import com.dailysystems.app.ui.components.percent
import com.dailysystems.app.domain.HabitStats
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.dailysystems.app.ui.components.DragHandle
import sh.calvin.reorderable.ReorderableColumn
import com.dailysystems.app.ui.components.Field
import com.dailysystems.app.ui.components.Hint
import com.dailysystems.app.ui.components.IconAction
import com.dailysystems.app.ui.components.LabelOptions
import com.dailysystems.app.ui.components.OptionChip
import com.dailysystems.app.ui.components.Segmented
import com.dailysystems.app.ui.components.DayToggle
import com.dailysystems.app.ui.components.Stepper
import com.dailysystems.app.ui.components.ReminderTimeField
import com.dailysystems.app.ui.components.TextStyles
import com.dailysystems.app.ui.components.dashedBorder
import com.dailysystems.app.ui.theme.AppTheme
import java.time.DayOfWeek
import kotlinx.coroutines.launch

@Composable
fun SystemEditorSheet(sheet: Sheet.SystemEditor, vm: SheetsViewModel, onDone: () -> Unit) {
    val flow = remember(sheet.id) { vm.observeSystem(sheet.id) }
    val system by flow.collectAsStateWithLifecycle(null)
    val s = system ?: return
    SheetScaffold(if (sheet.isNew) "New system" else "System", onDone) {
        SystemEditorBody(s, sheet.isNew, vm)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemEditorBody(s: DailySystem, isNew: Boolean, vm: SheetsViewModel) {
    val c = AppTheme.colors
    val labels by vm.labels.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var name by remember(s.id) { mutableStateOf(s.name) }
    var focusItemId by remember { mutableStateOf<String?>(null) }
    val nameFocus = remember { FocusRequester() }
    if (isNew) LaunchedEffect(Unit) { nameFocus.requestFocus() }

    Field("Name") {
        AppTextField(
            value = name,
            onValueChange = { name = it; vm.setSystemName(s.id, it) },
            placeholder = "e.g. Morning launch",
            textStyle = AppTheme.type.editorTitle,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.focusRequester(nameFocus).testTag("system-name"),
        )
    }
    Field("System priority") { LabelOptions(labels, s.labelId, { vm.setSystemLabel(s.id, it) }) }

    if (!isNew) HistorySection(s, vm)

    Field("Runs") {
        Segmented(
            listOf(
                ScheduleType.DAILY to "Daily", ScheduleType.WEEKLY to "Weekly",
                ScheduleType.MONTHLY to "Monthly", ScheduleType.DATES to "Specific dates",
            ),
            s.scheduleType, { vm.setScheduleType(s.id, it) },
        )
        Spacer(Modifier.height(10.dp))
        when (s.scheduleType) {
            ScheduleType.DAILY -> Hint("Runs every day.")
            ScheduleType.WEEKLY -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { d ->
                    DayToggle(Format.weekdayNarrow(d), d in s.weekdays, { vm.toggleWeekday(s.id, d) }, Format.weekdayShort(d))
                }
            }
            ScheduleType.MONTHLY -> MonthGrid(s.monthDays) { vm.toggleMonthDay(s.id, it) }
            ScheduleType.DATES -> {
                var picking by remember { mutableStateOf(false) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    s.dates.sorted().forEach { d ->
                        Row(
                            Modifier.clip(RoundedCornerShape(99.dp)).background(c.surface2).border(1.dp, c.line, RoundedCornerShape(99.dp)).padding(start = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(Format.shortDateYear(d), style = AppTheme.type.monoSmall, color = c.ink)
                            IconAction(AppIcons.Close, "Remove ${Format.shortDateYear(d)}", { vm.removeDate(s.id, d) }, Modifier.size(36.dp))
                        }
                    }
                    AppButton("Add date", { picking = true }, icon = AppIcons.Plus, small = true)
                }
                Hint("Add each target date this system should run on.", Modifier.padding(top = 6.dp))
                if (picking) AppDatePickerDialog(null, { picking = false }) { vm.addDate(s.id, it) }
            }
        }
    }

    Field("Remind me when it starts") {
        ReminderTimeField(s.reminderTime, { vm.setSystemReminder(s.id, it) }, { vm.setSystemReminder(s.id, null) })
        Hint(
            if (s.reminderTime == null) "Get a notification at a set time on every day this system runs."
            else "A notification with the day's steps and slot to-dos, on every day this system runs.",
            Modifier.padding(top = 6.dp),
        )
    }

    Field("Target dates from to-dos") {
        val linked = tasks.filter { it.runSystemId == s.id && it.dueDate != null && !it.done }.sortedBy { it.dueDate }
        if (linked.isEmpty()) {
            Hint("Any to-do can run this system on its target date. Open a to-do and choose this system under \"Run a system on that date\".")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                linked.forEach { t ->
                    Row(
                        Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(c.surface2)
                            .border(1.dp, c.line, RoundedCornerShape(99.dp))
                            .clickable(role = Role.Button) { vm.open(Sheet.TaskEditor(t.id)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(Format.shortDate(t.dueDate!!), style = AppTheme.type.monoSmall, color = c.muted)
                        Spacer(Modifier.width(6.dp))
                        Text(t.title.ifBlank { "Untitled to-do" }, style = TextStyles.rowSub.copy(fontSize = 13.sp), color = c.ink)
                    }
                }
            }
        }
    }

    Field("Items") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Hint("On the day, show items", Modifier.weight(1f))
            Segmented(listOf(ItemOrder.PRIORITY to "By priority", ItemOrder.LISTED to "As listed"), s.itemOrder, { vm.setOrder(s.id, it) })
        }
        Spacer(Modifier.height(10.dp))
        if (s.items.isEmpty()) {
            Hint("Add steps you always do, and slots that pull in your to-dos by priority.")
        }
        // Local order so a dropped item stays put until the database catches up.
        var order by remember(s.items) { mutableStateOf(s.items) }
        ReorderableColumn(
            list = order,
            onSettle = { from, to ->
                val moved = order[from]
                order = order.toMutableList().apply { add(to, removeAt(from)) }
                vm.moveItemTo(s.id, moved.id, to)
            },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { i, item, dragging ->
            key(item.id) {
                ReorderableItem {
                    ItemEditor(
                        s.id, item, labels, vm = vm, dragging = dragging,
                        requestFocus = focusItemId == item.id, onFocused = { focusItemId = null },
                        handle = {
                            DragHandle(
                                item.title.ifBlank { if (item.kind == ItemKind.SLOT) "slot" else "step" },
                                Modifier.draggableHandle(),
                                onMoveUp = if (i > 0) ({ vm.moveItem(s.id, item.id, -1) }) else null,
                                onMoveDown = if (i < order.lastIndex) ({ vm.moveItem(s.id, item.id, 1) }) else null,
                            )
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton("Step", { scope.launch { focusItemId = vm.addItem(s.id, ItemKind.STEP) } }, icon = AppIcons.Plus, small = true)
            AppButton("Slot", { scope.launch { focusItemId = vm.addItem(s.id, ItemKind.SLOT) } }, icon = AppIcons.Plus, small = true)
        }
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AppButton(if (s.active) "Pause system" else "Resume system", { vm.setActive(s.id, !s.active) })
        Spacer(Modifier.weight(1f))
        DeleteButton("Delete system", { vm.deleteSystem(s.id) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemEditor(
    systemId: String,
    item: SystemItem,
    labels: List<PriorityLabel>,
    vm: SheetsViewModel,
    dragging: Boolean,
    requestFocus: Boolean,
    onFocused: () -> Unit,
    handle: @Composable () -> Unit,
) {
    val c = AppTheme.colors
    val slot = item.kind == ItemKind.SLOT
    var title by remember(item.id) { mutableStateOf(item.title) }
    val focus = remember { FocusRequester() }
    if (requestFocus) LaunchedEffect(Unit) { focus.requestFocus(); onFocused() }
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface2)
            .then(
                when {
                    dragging -> Modifier.border(2.dp, c.accent, shape)
                    slot -> Modifier.dashedBorder(c.socket, 12.dp, 1.dp, dash = 4.dp)
                    else -> Modifier.border(1.dp, c.line, shape)
                },
            )
            .padding(10.dp)
            .testTag("item-editor"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            handle()
            AppTextField(
                value = title,
                onValueChange = { title = it; vm.setItemTitle(item.id, it) },
                placeholder = if (slot) "Slot name, e.g. Deep work" else "Step, e.g. Stretch 5 min",
                background = c.surface,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
            IconAction(AppIcons.Close, "Remove item", { vm.removeItem(systemId, item.id) }, Modifier.width(40.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Segmented(listOf(ItemKind.STEP to "Step", ItemKind.SLOT to "Slot"), item.kind, { vm.setItemKind(item.id, it) })
            Spacer(Modifier.width(10.dp))
            Hint(if (slot) "pulls in to-dos" else "done every time")
        }
        LabelOptions(labels, item.labelId, { vm.setItemLabel(item.id, it) })
        if (slot) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.surface).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("FILLS WITH TO-DOS LABELLED (NONE PICKED = ANY)", style = AppTheme.type.sectionLabel.copy(fontSize = 10.5.sp), color = c.muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    labels.forEach { l ->
                        OptionChip(l.name, Color(l.colorArgb), l.id in item.acceptedLabelIds, { vm.toggleAccepted(item.id, l.id) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("HOW MANY TO-DOS", style = AppTheme.type.sectionLabel.copy(fontSize = 10.5.sp), color = c.muted, modifier = Modifier.weight(1f))
                    Stepper(item.slotCount.coerceIn(1, 9), { vm.setSlotCount(item.id, it) })
                }
            }
        }
    }
}

/** 1–31 in rows of seven, then "Last" spanning the remainder of the last row. */
@Composable
private fun MonthGrid(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        (0 until 5).forEach { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                (1..7).map { r * 7 + it }.filter { it <= 31 }.forEach { d -> DayToggle("$d", d in selected, { onToggle(d) }, Format.ordinal(d)) }
                if (r == 4) DayToggle("Last", DailySystem.LAST_DAY in selected, { onToggle(DailySystem.LAST_DAY) }, "Last day of the month", weight = 4f)
            }
        }
    }
}

/** Streaks, a calendar of past runs, and how each step is going. */
@Composable
private fun HistorySection(s: DailySystem, vm: SheetsViewModel) {
    val c = AppTheme.colors
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val completions by vm.habitCompletions.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val h = remember(s, tasks, completions, today) { HabitStats.history(s, tasks, completions, today) }
    Field("History") {
        if (h == null) {
            Hint("Add a step to track this system. Streaks count the steps you check off; slots change every day.")
            return@Field
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("${h.currentStreak}", "in a row now", Modifier.weight(1f))
            StatTile("${h.bestStreak}", "best streak", Modifier.weight(1f))
            StatTile(percent(h.rate30), "last 30 days", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState(Int.MAX_VALUE))) { HistoryGrid(h, today) }
        val tracked = h.steps.filter { it.runs > 0 }
        if (tracked.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            tracked.forEach { st ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(st.item.title.ifBlank { "Untitled step" }, style = TextStyles.rowSub.copy(fontSize = 13.sp), color = c.ink, modifier = Modifier.weight(1f))
                    Text("${st.done}/${st.runs} · ${percent(st.rate)}", style = AppTheme.type.monoSmall, color = c.muted)
                }
            }
        }
        if (!s.active) Hint("Paused: no nudges while paused. The app doesn't record when a system was paused, so its run days still show here.", Modifier.padding(top = 6.dp))
    }
}
