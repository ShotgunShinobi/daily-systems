package com.dailysystems.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dailysystems.app.MainActivity
import com.dailysystems.app.R
import com.dailysystems.app.domain.DailySystem
import com.dailysystems.app.domain.DayPlanner
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.PlannedRow
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.domain.Reminder
import com.dailysystems.app.domain.Task
import com.dailysystems.app.ui.Format

object Notifications {
    const val CHANNEL_ID = "reminders"
    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_NOTIFICATION_ID = "notificationId"
    const val ACTION_MARK_DONE = "com.dailysystems.app.MARK_DONE"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "When a system starts and when a to-do is due"
                },
            )
        }
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun idFor(r: Reminder): Int = r.key.hashCode()

    private fun openApp(context: Context, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context, requestCode,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Builds and posts the notification for [r]; silently skips if notifications are off. */
    /** [nudge]: optional "never miss twice" line for a system start reminder. */
    fun post(
        context: Context, r: Reminder, systems: List<DailySystem>, tasks: List<Task>, labels: List<PriorityLabel>,
        nudge: String? = null,
    ) {
        if (!canPost(context)) return
        ensureChannel(context)
        val id = idFor(r)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(0xFF2B4ACB.toInt())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp(context, id))

        when (r.kind) {
            Reminder.Kind.SYSTEM_START -> {
                val plan = DayPlanner.planDay(r.date, systems, tasks, labels)
                val ps = plan.systems.firstOrNull { it.system.id == r.refId } ?: return
                val lines = ps.rows.flatMap { row ->
                    when (row) {
                        is PlannedRow.Step -> listOf("○ " + row.item.title.ifBlank { "Untitled step" })
                        is PlannedRow.Slot -> row.tasks.map { "→ " + it.title.ifBlank { "Untitled to-do" } } +
                            List(row.openPositions) { "→ open slot: " + row.item.title.ifBlank { "Slot" } }
                    }
                }
                val steps = ps.system.items.count { it.kind == ItemKind.STEP }
                val slotTasks = ps.rows.filterIsInstance<PlannedRow.Slot>().sumOf { it.tasks.size }
                builder
                    .setContentTitle("${ps.system.name.ifBlank { "Untitled system" }} starts now")
                    .setContentText(nudge ?: (Format.plural(steps, "step") + " · " + Format.plural(slotTasks, "to-do")))
                    .setStyle(NotificationCompat.BigTextStyle().bigText((listOfNotNull(nudge) + lines).joinToString("\n")))
            }
            Reminder.Kind.TASK_DUE -> {
                val t = tasks.firstOrNull { it.id == r.refId } ?: return
                val label = labels.firstOrNull { it.id == t.labelId }
                builder
                    .setContentTitle(t.title.ifBlank { "Untitled to-do" })
                    .setContentText(listOfNotNull("Due today", label?.name).joinToString(" · "))
                    .addAction(
                        0, "Mark done",
                        PendingIntent.getBroadcast(
                            context, id,
                            Intent(context, ReminderActionReceiver::class.java)
                                .setAction(ACTION_MARK_DONE)
                                .putExtra(EXTRA_TASK_ID, t.id)
                                .putExtra(EXTRA_NOTIFICATION_ID, id),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                        ),
                    )
                if (t.notes.isNotBlank()) builder.setStyle(NotificationCompat.BigTextStyle().bigText(t.notes))
            }
        }
        @Suppress("MissingPermission") // checked by canPost()
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}
