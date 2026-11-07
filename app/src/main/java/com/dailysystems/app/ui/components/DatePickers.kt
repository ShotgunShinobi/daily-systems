package com.dailysystems.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dailysystems.app.ui.Format
import com.dailysystems.app.ui.theme.AppTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(initial: LocalDate?, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val c = AppTheme.colors
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    val colors = DatePickerDefaults.colors(containerColor = c.surface)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK", color = c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.muted) } },
    ) { DatePicker(state, colors = colors) }
}

/** A date "input": mono date in an inset pill; tap to pick, × to clear. */
@Composable
fun DateField(
    date: LocalDate?,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "No date",
    onClear: (() -> Unit)? = null,
    description: String = "Target date",
) {
    val c = AppTheme.colors
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .defaultMinSize(minHeight = 44.dp)
                .clip(shape)
                .background(c.surface2)
                .border(1.dp, c.line, shape)
                .clickable(role = Role.Button) { open = true }
                .semantics(mergeDescendants = true) {
                    contentDescription = "$description: " + (date?.let(Format::shortDateYear) ?: placeholder)
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Calendar, null, Modifier.size(16.dp), tint = c.muted)
            Spacer(Modifier.width(6.dp))
            Text(date?.let(Format::shortDateYear) ?: placeholder, style = AppTheme.type.mono, color = if (date == null) c.muted else c.ink)
        }
        if (date != null && onClear != null) IconAction(AppIcons.Close, "Clear date", onClear)
    }
    if (open) AppDatePickerDialog(date, { open = false }, onPick)
}
