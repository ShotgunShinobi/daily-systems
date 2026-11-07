package com.dailysystems.app.ui.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailysystems.app.data.LABEL_PALETTE
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.ui.SheetsViewModel
import com.dailysystems.app.ui.components.AppButton
import com.dailysystems.app.ui.components.AppIcons
import com.dailysystems.app.ui.components.AppTextField
import com.dailysystems.app.ui.components.Hint
import com.dailysystems.app.ui.components.DragHandle
import sh.calvin.reorderable.ReorderableColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.dailysystems.app.ui.components.IconAction
import com.dailysystems.app.ui.editors.SheetScaffold
import com.dailysystems.app.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Ordered list (position = priority): swatch, inline rename, move, two-tap delete, add. */
@Composable
fun LabelsManagerSheet(vm: SheetsViewModel, onDone: () -> Unit) {
    val labels by vm.labels.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var paletteFor by remember { mutableStateOf<String?>(null) }
    var armed by remember { mutableStateOf<String?>(null) }
    var focusId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(armed) {
        if (armed != null) { delay(3500); armed = null }
    }

    SheetScaffold("Priority labels", onDone) {
        Hint("Higher in the list means higher priority; drag the grip to reorder. Slots fill from the top label down, and systems with higher labels pick first.")
        var order by remember(labels) { mutableStateOf(labels) }
        ReorderableColumn(
            list = order,
            onSettle = { from, to ->
                val moved = order[from]
                order = order.toMutableList().apply { add(to, removeAt(from)) }
                vm.moveLabelTo(moved.id, to)
            },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { i, l, dragging ->
            key(l.id) {
                ReorderableItem {
                    LabelRow(
                        label = l, position = i,
                        paletteOpen = paletteFor == l.id,
                        armed = armed == l.id,
                        dragging = dragging,
                        requestFocus = focusId == l.id,
                        onFocused = { focusId = null },
                        onSwatch = { paletteFor = if (paletteFor == l.id) null else l.id },
                        onColor = { vm.setLabelColor(l.id, it) },
                        onRename = { vm.renameLabel(l.id, it) },
                        onDelete = {
                            if (armed == l.id) { vm.deleteLabel(l.id); armed = null } else armed = l.id
                        },
                        handle = {
                            DragHandle(
                                l.name, Modifier.draggableHandle(),
                                onMoveUp = if (i > 0) ({ vm.moveLabel(l.id, -1) }) else null,
                                onMoveDown = if (i < order.lastIndex) ({ vm.moveLabel(l.id, 1) }) else null,
                            )
                        },
                    )
                }
            }
        }
        AppButton("Add label", { scope.launch { focusId = vm.addLabel() } }, icon = AppIcons.Plus, small = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelRow(
    label: PriorityLabel,
    position: Int,
    paletteOpen: Boolean,
    armed: Boolean,
    dragging: Boolean,
    requestFocus: Boolean,
    onFocused: () -> Unit,
    onSwatch: () -> Unit,
    onColor: (Int) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    handle: @Composable () -> Unit,
) {
    val c = AppTheme.colors
    var name by remember(label.id) { mutableStateOf(label.name) }
    val focus = remember { FocusRequester() }
    if (requestFocus) LaunchedEffect(Unit) { focus.requestFocus(); onFocused() }
    Column(
        Modifier
            .testTag("label-row")
            .clip(RoundedCornerShape(12.dp))
            .background(if (dragging) c.surface2 else c.surface)
            .then(if (dragging) Modifier.border(2.dp, c.accent, RoundedCornerShape(12.dp)) else Modifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            handle()
            Text("${position + 1}", style = AppTheme.type.monoSmall, color = c.muted, textAlign = TextAlign.End, modifier = Modifier.width(16.dp))
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onSwatch)
                    .semantics { contentDescription = "Change color of ${label.name}" },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(30.dp).clip(CircleShape).border(1.dp, c.line, CircleShape).padding(3.dp).clip(CircleShape).background(Color(label.colorArgb)))
            }
            AppTextField(
                value = name,
                onValueChange = { name = it; onRename(it) },
                placeholder = "Label name",
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
            IconAction(
                AppIcons.Close, if (armed) "Confirm delete ${label.name}" else "Delete ${label.name}", onDelete,
                Modifier.width(40.dp), tint = if (armed) c.danger else c.muted,
            )
        }
        if (paletteOpen) {
            FlowRow(Modifier.padding(start = 56.dp, top = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                LABEL_PALETTE.forEach { col ->
                    val on = col == label.colorArgb
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.RadioButton) { onColor(col) }
                            .semantics { selected = on; contentDescription = "Color #%06X".format(col and 0xFFFFFF) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(30.dp).clip(CircleShape)
                                .then(if (on) Modifier.border(2.dp, Color(col), CircleShape).padding(4.dp).clip(CircleShape) else Modifier)
                                .background(Color(col)),
                        )
                    }
                }
            }
        }
        if (armed) {
            Hint("Tap × again to delete. Items with this label become unlabelled.", Modifier.padding(start = 100.dp, top = 2.dp, bottom = 4.dp), color = c.danger)
        }
    }
    Spacer(Modifier.width(0.dp))
}
