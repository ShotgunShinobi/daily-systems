package com.dailysystems.app.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dailysystems.app.reminders.Notifications
import com.dailysystems.app.ui.Format
import com.dailysystems.app.ui.theme.AppTheme
import java.time.LocalTime

/**
 * Reminder time picker. Picking a time asks for notification permission (Android 13+) the first
 * time; if notifications are off, a hint explains how to turn them on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimeField(
    time: LocalTime?,
    onPick: (LocalTime) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "No reminder",
    enabled: Boolean = true,
) {
    val c = AppTheme.colors
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var allowed by remember { mutableStateOf(Notifications.canPost(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = Notifications.canPost(context) }
    val shape = RoundedCornerShape(10.dp)

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(shape)
                    .background(c.surface2)
                    .border(1.dp, c.line, shape)
                    .clickable(enabled = enabled, role = Role.Button) { open = true }
                    .semantics(mergeDescendants = true) { contentDescription = "Reminder: " + (time?.let(Format::time) ?: placeholder) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Bell, null, Modifier.size(16.dp), tint = c.muted)
                Spacer(Modifier.width(6.dp))
                Text(time?.let(Format::time) ?: placeholder, style = AppTheme.type.mono, color = if (time == null || !enabled) c.muted else c.ink)
            }
            if (time != null && enabled) IconAction(AppIcons.Close, "Remove reminder", onClear)
        }
        if (time != null && enabled && !allowed) {
            Hint("Notifications are off for Daily Systems, so this reminder can't show.", Modifier.padding(top = 6.dp), color = c.danger)
            AppButton("Turn on notifications", {
                if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }, kind = ButtonKind.Ghost, small = true)
        }
    }

    if (open) {
        val initial = time ?: LocalTime.of(9, 0)
        val state = rememberTimePickerState(initial.hour, initial.minute, DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = c.surface,
            confirmButton = {
                TextButton(onClick = {
                    open = false
                    onPick(LocalTime.of(state.hour, state.minute))
                    if (Build.VERSION.SDK_INT >= 33 && !Notifications.canPost(context)) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text("OK", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel", color = c.muted) } },
            text = { TimePicker(state, colors = TimePickerDefaults.colors(clockDialColor = c.surface2, selectorColor = c.accent)) },
        )
    }
}
