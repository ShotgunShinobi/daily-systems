package com.dailysystems.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailysystems.app.ui.LocalViewModelFactory
import com.dailysystems.app.ui.PickTarget
import com.dailysystems.app.ui.Sheet
import com.dailysystems.app.ui.SheetsViewModel
import com.dailysystems.app.ui.components.AppButton
import com.dailysystems.app.ui.components.AppCard
import com.dailysystems.app.ui.components.AppIcons
import com.dailysystems.app.ui.components.AppTextField
import com.dailysystems.app.ui.components.ButtonKind
import com.dailysystems.app.ui.components.DateField
import com.dailysystems.app.ui.components.EmptyCard
import com.dailysystems.app.ui.components.LabelChip
import com.dailysystems.app.ui.components.RowDivider
import com.dailysystems.app.ui.components.ScreenHeader
import com.dailysystems.app.ui.components.Segmented
import com.dailysystems.app.ui.components.TaskRow
import com.dailysystems.app.ui.components.labelColor
import com.dailysystems.app.ui.theme.AppTheme
import com.dailysystems.app.ui.theme.DailySystemsTheme
import com.dailysystems.app.ui.today.TodayPreviewData
import java.time.LocalDate

data class TasksActions(
    val add: (String) -> Boolean = { true },
    val cycleQuickLabel: () -> Unit = {},
    val setQuickDate: (LocalDate?) -> Unit = {},
    val setFilter: (TaskFilter) -> Unit = {},
    val toggle: (String) -> Unit = {},
    val open: (String) -> Unit = {},
    val pickLabel: (String) -> Unit = {},
    val newTask: () -> Unit = {},
    val openLabels: () -> Unit = {},
)

@Composable
fun TasksRoute(sheets: SheetsViewModel) {
    val vm: TasksViewModel = viewModel(factory = LocalViewModelFactory.current)
    val state by vm.state.collectAsStateWithLifecycle()
    TasksScreen(
        state,
        TasksActions(
            add = vm::add,
            cycleQuickLabel = vm::cycleQuickLabel,
            setQuickDate = vm::setQuickDate,
            setFilter = vm::setFilter,
            toggle = vm::toggle,
            open = { sheets.open(Sheet.TaskEditor(it)) },
            pickLabel = { sheets.open(Sheet.LabelPicker(PickTarget.ForTask(it))) },
            newTask = { sheets.newTask(dueDate = state.quickDate, labelId = state.quickLabel?.id) },
            openLabels = { sheets.open(Sheet.Labels) },
        ),
    )
}

@Composable
fun TasksScreen(state: TasksUiState, actions: TasksActions) {
    val c = AppTheme.colors
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val submit = {
        if (actions.add(text)) text = ""
        // Submitting keeps focus in the field for rapid entry.
        focus.requestFocus()
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().testTag("tasks-list"), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)) {
            item(key = "header") { ScreenHeader("To-dos", actions.openLabels) }
            item(key = "quick") {
                AppCard(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    AppTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = "Add a to-do",
                        bordered = false,
                        textStyle = AppTheme.type.cardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = androidx.compose.material3.MaterialTheme.typography.bodyLarge.fontFamily),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp).focusRequester(focus).testTag("quick-add"),
                    )
                    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        LabelChip(state.quickLabel, actions.cycleQuickLabel)
                        Spacer(Modifier.width(4.dp))
                        DateField(state.quickDate, { actions.setQuickDate(it) }, onClear = { actions.setQuickDate(null) }, placeholder = "Date")
                        Spacer(Modifier.weight(1f))
                        AppButton("Add", { submit() }, kind = ButtonKind.Primary, icon = AppIcons.Plus, small = true)
                    }
                }
            }
            item(key = "filter") {
                Segmented(
                    listOf(TaskFilter.OPEN to "Open", TaskFilter.DONE to "Done", TaskFilter.ALL to "All"),
                    state.filter, actions.setFilter, Modifier.padding(top = 14.dp, bottom = 4.dp),
                )
            }
            if (state.loaded && state.groups.isEmpty()) {
                item(key = "empty") {
                    EmptyCard(
                        null,
                        if (state.filter == TaskFilter.DONE) "Finished to-dos will collect here."
                        else "Nothing open. Add a to-do above, give it a priority, and any system slot that takes that priority will pick it up.",
                        Modifier.padding(top = 14.dp),
                    )
                }
            }
            state.groups.forEach { g ->
                item(key = "g-" + (g.label?.id ?: "none")) {
                    Row(Modifier.padding(start = 2.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(labelColor(g.label)))
                        Spacer(Modifier.width(8.dp))
                        Text(g.label?.name ?: "No label", style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = c.ink)
                        Spacer(Modifier.width(8.dp))
                        Text("${g.tasks.size}", style = AppTheme.type.monoSmall, color = c.muted)
                    }
                }
                item(key = "l-" + (g.label?.id ?: "none")) {
                    AppCard {
                        g.tasks.forEachIndexed { i, t ->
                            if (i > 0) RowDivider()
                            TaskRow(
                                task = t,
                                label = state.labelMap[t.labelId],
                                runsSystemName = t.runSystemId?.let { state.systemNames[it] },
                                referenceDate = state.today,
                                onToggle = { actions.toggle(t.id) },
                                onOpen = { actions.open(t.id) },
                                onPickLabel = { actions.pickLabel(t.id) },
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = actions.newTask,
            containerColor = c.accent, contentColor = c.onAccent, shape = RoundedCornerShape(18.dp),
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).semantics { contentDescription = "Add a to-do" },
        ) { Icon(AppIcons.Plus, null, Modifier.size(24.dp)) }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFEDF0F4, heightDp = 800)
@Composable
private fun TasksPreview() = DailySystemsTheme(darkTheme = false) {
    val d = TodayPreviewData
    val labelMap = d.labels.associateBy { it.id }
    val state = TasksUiState(
        loaded = true,
        groups = d.labels.map { l -> TaskGroup(l, d.tasks.filter { it.labelId == l.id }) }.filter { it.tasks.isNotEmpty() },
        labels = d.labels, labelMap = labelMap, today = d.today, quickLabel = d.labels[1],
    )
    Box(Modifier.background(Color(0xFFEDF0F4))) { TasksScreen(state, TasksActions()) }
}
