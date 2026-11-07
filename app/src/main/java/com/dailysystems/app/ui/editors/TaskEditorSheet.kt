package com.dailysystems.app.ui.editors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailysystems.app.domain.DayPlanner
import com.dailysystems.app.domain.Task
import com.dailysystems.app.ui.Sheet
import com.dailysystems.app.ui.SheetsViewModel
import com.dailysystems.app.ui.components.AppButton
import com.dailysystems.app.ui.components.AppIcons
import com.dailysystems.app.ui.components.AppTextField
import com.dailysystems.app.ui.components.DateField
import com.dailysystems.app.ui.components.DeleteButton
import com.dailysystems.app.ui.components.Field
import com.dailysystems.app.ui.components.Hint
import com.dailysystems.app.ui.components.LabelOptions
import com.dailysystems.app.ui.components.TextStyles
import com.dailysystems.app.ui.components.ReminderTimeField
import com.dailysystems.app.ui.components.Segmented
import com.dailysystems.app.ui.components.Stepper
import com.dailysystems.app.ui.components.DayToggle
import com.dailysystems.app.domain.RepeatRule
import com.dailysystems.app.domain.RepeatUnit
import com.dailysystems.app.ui.Format
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import java.time.DayOfWeek
import java.time.LocalDate
import com.dailysystems.app.ui.theme.AppTheme

@Composable
fun TaskEditorSheet(sheet: Sheet.TaskEditor, vm: SheetsViewModel, onDone: () -> Unit) {
    val flow = remember(sheet.id) { vm.observeTask(sheet.id) }
    val task by flow.collectAsStateWithLifecycle(null)
    val t = task ?: return
    SheetScaffold(if (sheet.isNew) "New to-do" else "To-do", onDone) {
        TaskEditorBody(t, sheet, vm)
    }
}

@Composable
private fun TaskEditorBody(t: Task, sheet: Sheet.TaskEditor, vm: SheetsViewModel) {
    val labels by vm.labels.collectAsStateWithLifecycle()
    val systems by vm.systems.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()

    // Local text state seeded once; every keystroke saves through the repository.
    var title by remember(t.id) { mutableStateOf(t.title) }
    var notes by remember(t.id) { mutableStateOf(t.notes) }
    val titleFocus = remember { FocusRequester() }
    if (sheet.isNew) LaunchedEffect(Unit) { titleFocus.requestFocus() }

    Field("Title") {
        AppTextField(
            value = title,
            onValueChange = { title = it; vm.setTaskTitle(t.id, it) },
            placeholder = "What needs doing?",
            textStyle = AppTheme.type.editorTitle,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.focusRequester(titleFocus).testTag("task-title"),
        )
    }
    Field("Priority") { LabelOptions(labels, t.labelId, { vm.setTaskLabel(t.id, it) }) }
    Field("Target date") {
        DateField(t.dueDate, { vm.setTaskDue(t.id, it) }, onClear = { vm.setTaskDue(t.id, null) })
    }
    Field("Repeat") {
        RepeatEditor(t.repeat, t.dueDate ?: sheet.contextDate ?: today) { vm.setTaskRepeat(t.id, it) }
    }
    Field("Remind me") {
        ReminderTimeField(
            t.reminderTime, { vm.setTaskReminder(t.id, it) }, { vm.setTaskReminder(t.id, null) },
            enabled = t.dueDate != null,
        )
        if (t.dueDate == null) Hint("Give it a target date to get a reminder on that day.", Modifier.padding(top = 6.dp))
    }
    Field("Run a system on that date") {
        val labelMap = labels.associateBy { it.id }
        val options = systems.sortedWith(DayPlanner.systemComparator(labelMap))
        SystemDropdown(
            selectedName = options.firstOrNull { it.id == t.runSystemId }?.name?.ifBlank { "Untitled system" },
            options = options.map { it.id to it.name.ifBlank { "Untitled system" } },
            onSelect = { vm.setTaskRunSystem(t.id, it) },
        )
        if (t.runSystemId != null && t.dueDate == null) {
            Hint("Pick a target date so the system knows when to run.", Modifier.padding(top = 6.dp))
        }
    }
    Field("Notes") {
        AppTextField(
            value = notes,
            onValueChange = { notes = it; vm.setTaskNotes(t.id, it) },
            placeholder = "Links, context, sub-steps, anything you want to remember",
            singleLine = false,
            minHeight = 140.dp,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.testTag("task-notes"),
        )
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AppButton(
            if (t.done) "Mark not done" else "Mark done",
            { vm.toggleTask(t.id, sheet.contextDate ?: today) },
            icon = if (t.done) null else AppIcons.Check,
        )
        Spacer(Modifier.weight(1f))
        DeleteButton("Delete", { vm.deleteTask(t.id) })
    }
}

@Composable
private fun SystemDropdown(selectedName: String?, options: List<Pair<String, String>>, onSelect: (String?) -> Unit) {
    val c = AppTheme.colors
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clip(shape)
                .background(c.surface2)
                .border(1.dp, c.line, shape)
                .clickable(role = Role.DropdownList) { open = true }
                .semantics(mergeDescendants = true) { contentDescription = "Run a system on that date: ${selectedName ?: "None"}" }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedName != null) Icon(AppIcons.Plug, null, Modifier.size(16.dp).padding(end = 0.dp), tint = c.muted)
            Text(
                selectedName ?: "None",
                style = TextStyles.input,
                color = if (selectedName == null) c.muted else c.ink,
                modifier = Modifier.weight(1f).padding(start = if (selectedName != null) 8.dp else 0.dp),
            )
            Icon(AppIcons.Down, null, Modifier.size(18.dp), tint = c.muted)
        }
        DropdownMenu(open, { open = false }, containerColor = c.surface) {
            DropdownMenuItem(text = { Text("None", color = c.muted) }, onClick = { onSelect(null); open = false })
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name, color = c.ink) }, onClick = { onSelect(id); open = false })
            }
        }
    }
}

/** Never / Daily / Weekly / Monthly / Yearly, an "every N" stepper, and weekdays for weekly. */
@Composable
private fun RepeatEditor(rule: RepeatRule?, anchor: LocalDate, onChange: (RepeatRule?) -> Unit) {
    val c = AppTheme.colors
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        Segmented(
            listOf<Pair<RepeatUnit?, String>>(
                null to "Never", RepeatUnit.DAY to "Daily", RepeatUnit.WEEK to "Weekly",
                RepeatUnit.MONTH to "Monthly", RepeatUnit.YEAR to "Yearly",
            ),
            rule?.unit,
            { unit ->
                onChange(
                    unit?.let {
                        RepeatRule(
                            unit = it,
                            interval = rule?.interval ?: 1,
                            weekdays = if (it == RepeatUnit.WEEK) rule?.weekdays.orEmpty().ifEmpty { setOf(anchor.dayOfWeek) } else emptySet(),
                            anchor = rule?.anchor ?: anchor,
                        )
                    },
                )
            },
        )
    }
    if (rule == null) return
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val unitWord = when (rule.unit) {
            RepeatUnit.DAY -> "day"; RepeatUnit.WEEK -> "week"; RepeatUnit.MONTH -> "month"; RepeatUnit.YEAR -> "year"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Every", style = TextStyles.body, color = c.muted)
            Spacer(Modifier.padding(start = 8.dp))
            Stepper(rule.interval, { onChange(rule.copy(interval = it)) }, min = 1, max = 30, unit = unitWord + "s")
            Spacer(Modifier.padding(start = 8.dp))
            Text(if (rule.interval == 1) unitWord else unitWord + "s", style = TextStyles.body, color = c.muted)
        }
        if (rule.unit == RepeatUnit.WEEK) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { d ->
                    val on = d in rule.weekdays
                    DayToggle(Format.weekdayNarrow(d), on, {
                        val next = if (on) rule.weekdays - d else rule.weekdays + d
                        if (next.isNotEmpty()) onChange(rule.copy(weekdays = next))
                    }, Format.weekdayShort(d))
                }
            }
        }
        Hint(Format.repeatSummary(rule) + ". When you check it off, the next one is added automatically.")
    }
}
