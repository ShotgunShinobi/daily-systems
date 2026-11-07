package com.dailysystems.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.domain.Task
import com.dailysystems.app.ui.Format
import com.dailysystems.app.ui.theme.AppTheme
import java.time.LocalDate

/**
 * One to-do: check, title, sub-line (date — red "overdue" when late — system it runs, note
 * indicator), label chip that opens the label picker.
 */
@Composable
fun TaskRow(
    task: Task,
    label: PriorityLabel?,
    runsSystemName: String?,
    referenceDate: LocalDate,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onPickLabel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    Row(
        modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).padding(start = 4.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(task.done, labelColor(label), onToggle, if (task.done) "Mark not done" else "Mark done")
        Column(
            Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(vertical = 8.dp, horizontal = 2.dp),
        ) {
            Text(
                task.title.ifBlank { "Untitled to-do" },
                style = TextStyles.rowTitle,
                color = if (task.done) c.muted else c.ink,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
            )
            TaskSubLine(task, runsSystemName, referenceDate)
        }
        LabelChip(label, onPickLabel)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskSubLine(task: Task, runsSystemName: String?, referenceDate: LocalDate) {
    val c = AppTheme.colors
    val due = task.dueDate
    val hasNote = task.notes.isNotBlank()
    if (due == null && runsSystemName == null && !hasNote && !task.isExample && task.repeat == null) return
    FlowRow(
        Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (due != null) {
            val late = !task.done && due.isBefore(referenceDate)
            Text(
                (if (late) "overdue · " else "") + Format.shortDate(due) + (task.reminderTime?.let { " · " + Format.time(it) } ?: ""),
                style = AppTheme.type.monoSmall,
                color = if (late) c.danger else c.muted,
            )
        }
        task.repeat?.let { SubBit(AppIcons.Repeat, Format.repeatSummary(it).replaceFirstChar { c -> c.lowercase() }) }
        if (runsSystemName != null) SubBit(AppIcons.Plug, runsSystemName)
        if (hasNote) SubBit(AppIcons.Note, "note")
        if (task.isExample) ExampleTag()
    }
}

@Composable
fun SubBit(icon: ImageVector, text: String, color: Color = AppTheme.colors.muted) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(12.dp), tint = color)
        Spacer(Modifier.width(3.dp))
        Text(text, style = TextStyles.rowSub, color = color, maxLines = 1)
    }
}

@Composable
fun RowDivider() = HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.line)
