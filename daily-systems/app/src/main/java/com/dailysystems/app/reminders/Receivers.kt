package com.dailysystems.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.dailysystems.app.DailySystemsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Runs [block] off the main thread while keeping the broadcast alive. */
private fun BroadcastReceiver.async(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try { block() } finally { pending.finish() }
    }
}

private val Context.app get() = applicationContext as DailySystemsApp

/** The single reminder alarm fired. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = async { context.app.reminders.onAlarm() }
}

/** Alarms don't survive reboots or clock changes; set the next one again. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Exported for system broadcasts; ignore anything else.
        if (intent.action !in HANDLED) return
        async { context.app.reminders.scheduleNow() }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}

/** "Mark done" on a to-do notification. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = async {
        if (intent.action != Notifications.ACTION_MARK_DONE) return@async
        val id = intent.getStringExtra(Notifications.EXTRA_TASK_ID) ?: return@async
        val c = context.app.container
        c.writeAwait {
            val t = c.tasks.get(id)
            if (t != null && !t.done) c.tasks.toggleDone(id, LocalDate.now())
        }
        NotificationManagerCompat.from(context).cancel(intent.getIntExtra(Notifications.EXTRA_NOTIFICATION_ID, 0))
    }
}
