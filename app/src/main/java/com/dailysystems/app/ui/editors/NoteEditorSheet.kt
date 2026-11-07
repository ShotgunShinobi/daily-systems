package com.dailysystems.app.ui.editors

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailysystems.app.ui.Format
import com.dailysystems.app.ui.Sheet
import com.dailysystems.app.ui.SheetsViewModel
import com.dailysystems.app.ui.components.AppTextField
import com.dailysystems.app.ui.components.DeleteButton
import com.dailysystems.app.ui.components.Field
import com.dailysystems.app.ui.components.Hint
import com.dailysystems.app.ui.components.LabelOptions
import com.dailysystems.app.ui.theme.AppTheme

@Composable
fun NoteEditorSheet(sheet: Sheet.NoteEditor, vm: SheetsViewModel, onDone: () -> Unit) {
    val flow = remember(sheet.id) { vm.observeNote(sheet.id) }
    val note by flow.collectAsStateWithLifecycle(null)
    val n = note ?: return
    val labels by vm.labels.collectAsStateWithLifecycle()
    var title by remember(n.id) { mutableStateOf(n.title) }
    var body by remember(n.id) { mutableStateOf(n.body) }
    val focus = remember { FocusRequester() }
    if (sheet.isNew) LaunchedEffect(Unit) { focus.requestFocus() }

    SheetScaffold(if (sheet.isNew) "New note" else "Note", onDone) {
        AppTextField(
            value = title,
            onValueChange = { title = it; vm.setNoteTitle(n.id, it) },
            placeholder = "Title",
            bordered = false,
            textStyle = AppTheme.type.editorTitle,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.focusRequester(focus).testTag("note-title"),
        )
        Field("Priority") { LabelOptions(labels, n.labelId, { vm.setNoteLabel(n.id, it) }) }
        Field("Note") {
            AppTextField(
                value = body,
                onValueChange = { body = it; vm.setNoteBody(n.id, it) },
                placeholder = "Start writing",
                singleLine = false,
                minHeight = 260.dp,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.testTag("note-body"),
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (n.updatedAt > 0) Hint("Edited " + Format.timestamp(n.updatedAt))
            Spacer(Modifier.weight(1f))
            DeleteButton("Delete note", { vm.deleteNote(n.id) })
        }
    }
}
