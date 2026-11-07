package com.dailysystems.app.ui.editors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailysystems.app.ui.Sheet
import com.dailysystems.app.ui.SheetsViewModel
import com.dailysystems.app.ui.labels.LabelPickerSheet
import com.dailysystems.app.ui.labels.LabelsManagerSheet
import com.dailysystems.app.ui.review.ReviewSheet
import com.dailysystems.app.ui.theme.AppTheme
import kotlinx.coroutines.launch

/** Hosts the single open bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHost(vm: SheetsViewModel) {
    val sheet by vm.sheet.collectAsStateWithLifecycle()
    // Keep shared lists hot while any sheet is open.
    vm.labels.collectAsStateWithLifecycle()
    val s = sheet ?: return
    val c = AppTheme.colors
    key(s) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        val done: () -> Unit = {
            scope.launch { state.hide() }.invokeOnCompletion { if (vm.sheet.value == s) vm.close() }
        }
        ModalBottomSheet(
            onDismissRequest = { if (vm.sheet.value == s) vm.close() },
            sheetState = state,
            containerColor = c.surface,
            contentColor = c.ink,
            scrimColor = c.scrim,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Box(Modifier.padding(top = 8.dp, bottom = 6.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(4.dp)).background(c.line))
            },
        ) {
            when (s) {
                is Sheet.TaskEditor -> TaskEditorSheet(s, vm, done)
                is Sheet.SystemEditor -> SystemEditorSheet(s, vm, done)
                is Sheet.NoteEditor -> NoteEditorSheet(s, vm, done)
                Sheet.Labels -> LabelsManagerSheet(vm, done)
                is Sheet.LabelPicker -> LabelPickerSheet(s, vm, done)
                is Sheet.Review -> ReviewSheet(s, vm, done)
            }
        }
    }
}
