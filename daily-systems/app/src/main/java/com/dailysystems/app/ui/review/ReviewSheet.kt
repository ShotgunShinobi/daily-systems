package com.dailysystems.app.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailysystems.app.domain.HabitStats
import com.dailysystems.app.ui.Format
import com.dailysystems.app.ui.Sheet
import com.dailysystems.app.ui.SheetsViewModel
import com.dailysystems.app.ui.components.AppIcons
import com.dailysystems.app.ui.components.Field
import com.dailysystems.app.ui.components.Hint
import com.dailysystems.app.ui.components.IconAction
import com.dailysystems.app.ui.components.RowDivider
import com.dailysystems.app.ui.components.StatTile
import com.dailysystems.app.ui.components.TextStyles
import com.dailysystems.app.ui.components.percent
import com.dailysystems.app.ui.editors.SheetScaffold
import com.dailysystems.app.ui.theme.AppTheme
import kotlin.math.roundToInt

/** Weekly look-back: how many runs were kept, streaks, most-missed steps, and the nudge settings. */
@Composable
fun ReviewSheet(sheet: Sheet.Review, vm: SheetsViewModel, onDone: () -> Unit) {
    val c = AppTheme.colors
    val systems by vm.systems.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val completions by vm.habitCompletions.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val thisWeek = HabitStats.weekStart(today)
    val week = sheet.weekStart ?: thisWeek
    val r = remember(week, systems, tasks, completions, today) { HabitStats.weekReview(week, systems, tasks, completions, today) }

    SheetScaffold("Weekly review", onDone) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(AppIcons.Left, "Previous week", { vm.open(Sheet.Review(week.minusWeeks(1))) })
            Text(
                if (week == thisWeek) "This week" else "Week of ${Format.shortDate(week)}",
                style = AppTheme.type.cardTitle, color = c.ink,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp).testTag("review-title"),
            )
            IconAction(AppIcons.Right, "Next week", { vm.open(Sheet.Review(week.plusWeeks(1))) }, enabled = week.isBefore(thisWeek))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val delta = if (r.keptRate != null && r.previousKeptRate != null) ((r.keptRate - r.previousKeptRate) * 100).roundToInt() else null
            StatTile(
                percent(r.keptRate),
                "runs kept" + (delta?.let { " · ${if (it >= 0) "+" else ""}$it vs last" } ?: ""),
                Modifier.weight(1f),
            )
            StatTile("${r.stepsChecked}", "steps checked", Modifier.weight(1f))
            StatTile("${r.todosDone}", "to-dos done", Modifier.weight(1f))
        }

        Field("Systems") {
            if (r.systems.isEmpty()) {
                Hint("No system with steps ran this week yet. Streaks count the steps you check off.")
            }
            Column {
                r.systems.forEachIndexed { i, s ->
                    if (i > 0) RowDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${s.system.name}: kept ${s.kept} of ${s.runDays} runs, " +
                                    "${s.complete} complete, streak ${s.currentStreak}, best ${s.bestStreak}"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(s.system.name.ifBlank { "Untitled system" }, style = TextStyles.rowTitle, color = c.ink)
                            Text(
                                "${s.complete} with every step · best streak ${s.bestStreak}",
                                style = TextStyles.rowSub, color = c.muted,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${s.kept}/${s.runDays}", style = AppTheme.type.mono, color = c.ink)
                            Text("${s.currentStreak} in a row", style = AppTheme.type.monoSmall, color = c.muted)
                        }
                    }
                }
            }
        }

        if (r.mostMissed.isNotEmpty()) {
            Field("Most missed steps") {
                r.mostMissed.forEach { m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.stepTitle, style = TextStyles.rowTitle, color = c.ink)
                            Text(m.systemName, style = TextStyles.rowSub, color = c.muted)
                        }
                        Text("missed ${m.missed}/${m.runs}", style = AppTheme.type.monoSmall, color = c.danger)
                    }
                }
                Hint("A step you keep skipping might be too big. Try a smaller version of it.", Modifier.padding(top = 6.dp))
            }
        }

        Field("Never miss twice") {
            SettingSwitch(
                "Show a nudge on Today", "When a system's last run was missed, its card says so until you check a step.",
                settings.nudgesInApp, vm::setNudgesInApp, "switch-nudge-app",
            )
            Spacer(Modifier.height(6.dp))
            SettingSwitch(
                "Add it to start reminders", "Uses the system's existing reminder; no extra notifications.",
                settings.nudgesInReminders, vm::setNudgesInReminders, "switch-nudge-reminder",
            )
        }
    }
}

@Composable
private fun SettingSwitch(title: String, hint: String, on: Boolean, onChange: (Boolean) -> Unit, tag: String) {
    val c = AppTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = TextStyles.rowTitle, color = c.ink)
            Text(hint, style = TextStyles.rowSub, color = c.muted)
        }
        Switch(
            checked = on, onCheckedChange = onChange, modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent, uncheckedTrackColor = c.surface2, uncheckedBorderColor = c.line),
        )
    }
}
