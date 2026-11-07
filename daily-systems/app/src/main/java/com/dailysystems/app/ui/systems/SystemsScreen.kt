package com.dailysystems.app.ui.systems

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.ui.Format
import com.dailysystems.app.ui.LocalViewModelFactory
import com.dailysystems.app.ui.PickTarget
import com.dailysystems.app.ui.Sheet
import com.dailysystems.app.ui.SheetsViewModel
import com.dailysystems.app.ui.components.AppButton
import com.dailysystems.app.ui.components.AppCard
import com.dailysystems.app.ui.components.AppIcons
import com.dailysystems.app.ui.components.ButtonKind
import com.dailysystems.app.ui.components.EmptyCard
import com.dailysystems.app.ui.components.ExampleTag
import com.dailysystems.app.ui.components.RecentRunsStrip
import com.dailysystems.app.ui.components.StreakBadge
import com.dailysystems.app.ui.components.percent
import androidx.compose.material3.Icon
import com.dailysystems.app.ui.components.Hint
import com.dailysystems.app.ui.components.LabelChip
import com.dailysystems.app.ui.components.ScreenHeader
import com.dailysystems.app.ui.components.TextStyles
import com.dailysystems.app.ui.components.labelColor
import com.dailysystems.app.ui.theme.AppTheme
import com.dailysystems.app.ui.theme.DailySystemsTheme
import com.dailysystems.app.ui.today.TodayPreviewData

data class SystemsActions(
    val newSystem: () -> Unit = {},
    val edit: (String) -> Unit = {},
    val pickLabel: (String) -> Unit = {},
    val loadStarter: () -> Unit = {},
    val openLabels: () -> Unit = {},
    val openReview: () -> Unit = {},
)

@Composable
fun SystemsRoute(sheets: SheetsViewModel) {
    val vm: SystemsViewModel = viewModel(factory = LocalViewModelFactory.current)
    val state by vm.state.collectAsStateWithLifecycle()
    SystemsScreen(
        state,
        SystemsActions(
            newSystem = sheets::newSystem,
            edit = { sheets.open(Sheet.SystemEditor(it)) },
            pickLabel = { sheets.open(Sheet.LabelPicker(PickTarget.ForSystem(it))) },
            loadStarter = sheets::loadStarterExamples,
            openLabels = { sheets.open(Sheet.Labels) },
            openReview = { sheets.open(Sheet.Review()) },
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemsScreen(state: SystemsUiState, actions: SystemsActions) {
    val c = AppTheme.colors
    LazyColumn(Modifier.fillMaxSize().testTag("systems-list"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
        item(key = "header") {
            ScreenHeader("Systems", actions.openLabels) {
                AppButton("New", actions.newSystem, kind = ButtonKind.Primary, icon = AppIcons.Plus, small = true)
            }
        }
        if (state.loaded && state.systems.isEmpty()) {
            item(key = "empty") {
                EmptyCard(
                    "No systems yet",
                    "Make a routine once and reuse it: daily, on chosen weekdays, on days of the month, or on specific target dates.",
                    Modifier.padding(top = 4.dp),
                ) {
                    AppButton("New system", actions.newSystem, kind = ButtonKind.Primary, icon = AppIcons.Plus)
                    AppButton("Load starter examples", actions.loadStarter)
                }
            }
            return@LazyColumn
        }
        item(key = "hint") { Hint("Higher-priority systems claim matching to-dos first.", Modifier.padding(bottom = 12.dp)) }
        state.week?.let { w ->
            item(key = "week") {
                AppCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = actions.openReview)
                            .testTag("week-card").padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("THIS WEEK", style = AppTheme.type.sectionLabel, color = c.muted)
                            val runs = w.systems.sumOf { it.runDays }
                            val kept = w.systems.sumOf { it.kept }
                            Text(
                                if (runs == 0) "No runs to count yet" else "$kept of $runs runs kept · ${percent(w.keptRate)}",
                                style = TextStyles.rowTitle, color = c.ink, modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Text("Review", style = TextStyles.button, color = c.accent)
                        Icon(AppIcons.Right, null, Modifier.size(18.dp), tint = c.accent)
                    }
                }
            }
        }
        items(state.systems, key = { it.system.id }) { sum ->
            val s = sum.system
            AppCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { actions.edit(s.id) }
                        .alpha(if (s.active) 1f else 0.6f)
                        .padding(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name.ifBlank { "Untitled system" }, style = AppTheme.type.cardTitle, color = c.ink, modifier = Modifier.weight(1f, fill = false))
                            if (s.isExample) { Spacer(Modifier.width(6.dp)); ExampleTag() }
                        }
                        Text(sum.schedule, style = AppTheme.type.monoSmall, color = c.muted, modifier = Modifier.padding(top = 3.dp))
                        Text(
                            Format.plural(sum.steps, "step") + " · " + Format.plural(sum.slots, "slot"),
                            style = TextStyles.rowSub, color = c.muted, modifier = Modifier.padding(top = 2.dp),
                        )
                        state.histories[s.id]?.let { h ->
                            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("recent runs", style = AppTheme.type.monoSmall.copy(fontSize = 10.5.sp), color = c.muted)
                                Spacer(Modifier.width(6.dp))
                                RecentRunsStrip(h)
                                Spacer(Modifier.width(8.dp))
                                StreakBadge(h.currentStreak)
                            }
                        }
                        if (sum.pips.isNotEmpty()) {
                            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                sum.pips.forEach { item ->
                                    val col = labelColor(state.labels[item.labelId])
                                    // filled = step, ring = slot
                                    Box(
                                        Modifier.size(9.dp).clip(CircleShape).then(
                                            if (item.kind == ItemKind.STEP) Modifier.background(col)
                                            else Modifier.border(2.dp, col, CircleShape),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    LabelChip(state.labels[s.labelId], { actions.pickLabel(s.id) })
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDF0F4)
@Composable
private fun SystemsPreview() = DailySystemsTheme(darkTheme = false) {
    val d = TodayPreviewData
    val labels = d.labels.associateBy { it.id }
    val state = SystemsUiState(
        loaded = true, labels = labels,
        systems = d.systems.map { SystemSummary(it, "Every day", 2, 1, it.items) },
    )
    Box(Modifier.background(Color(0xFFEDF0F4))) { SystemsScreen(state, SystemsActions()) }
}
