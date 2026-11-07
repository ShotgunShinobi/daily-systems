package com.dailysystems.app.ui.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailysystems.app.ui.PickTarget
import com.dailysystems.app.ui.Sheet
import com.dailysystems.app.ui.SheetsViewModel
import com.dailysystems.app.ui.components.AppButton
import com.dailysystems.app.ui.components.AppIcons
import com.dailysystems.app.ui.components.ButtonKind
import com.dailysystems.app.ui.components.TextStyles
import com.dailysystems.app.ui.editors.SheetScaffold
import com.dailysystems.app.ui.theme.AppTheme

/** Opened by tapping any label chip on a list row. */
@Composable
fun LabelPickerSheet(sheet: Sheet.LabelPicker, vm: SheetsViewModel, onDone: () -> Unit) {
    val labels by vm.labels.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val systems by vm.systems.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val target = sheet.target
    val (title, current) = when (target) {
        is PickTarget.ForTask -> "To-do priority" to tasks.firstOrNull { it.id == target.taskId }?.labelId
        is PickTarget.ForSystem -> "System priority" to systems.firstOrNull { it.id == target.systemId }?.labelId
        is PickTarget.ForItem -> "Item priority" to
            systems.firstOrNull { it.id == target.systemId }?.items?.firstOrNull { it.id == target.itemId }?.labelId
        is PickTarget.ForNote -> "Note priority" to notes.firstOrNull { it.id == target.noteId }?.labelId
    }
    val known = labels.any { it.id == current }
    SheetScaffold(title, onDone) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEach { l -> PickRow(l.name, Color(l.colorArgb), current == l.id) { vm.pick(target, l.id) } }
            PickRow("No label", AppTheme.colors.socket, !known) { vm.pick(target, null) }
        }
        AppButton("Edit labels", { vm.open(Sheet.Labels) }, kind = ButtonKind.Ghost, icon = AppIcons.Tag)
    }
}

@Composable
private fun PickRow(name: String, color: Color, on: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(if (on) color.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (on) color else c.line, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = on }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(name, style = TextStyles.button, color = c.ink)
    }
}
