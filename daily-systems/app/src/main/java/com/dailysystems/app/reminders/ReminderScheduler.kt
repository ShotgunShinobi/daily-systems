package com.dailysystems.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import com.dailysystems.app.AppContainer
import com.dailysystems.app.domain.DailySystem
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.domain.HabitStats
import com.dailysystems.app.domain.Reminder
import com.dailysystems.app.domain.ReminderPlanner
import com.dailysystems.app.domain.Task
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Keeps exactly one alarm set: the next reminder. When it fires, everything due since the last
 * run is posted (so nothing is lost if the alarm is late) and the next alarm is set.
 */
class ReminderScheduler(private val context: Context, private val c: AppContainer) {
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    private val alarms = context.getSystemService(AlarmManager::class.java)

    private fun toMillis(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun fromMillis(m: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(m), ZoneId.systemDefault())

    /** Reminders up to this moment were already posted. */
    private fun handledUntil(now: LocalDateTime): LocalDateTime {
        val m = prefs.getLong(KEY_HANDLED, 0L)
        return if (m == 0L) now else maxOf(fromMillis(m), now.minusHours(MAX_CATCH_UP_HOURS))
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun schedule(systems: List<DailySystem>, tasks: List<Task>, labels: List<PriorityLabel>) {
        val now = LocalDateTime.now()
        val next = ReminderPlanner.next(maxOf(now, handledUntil(now)), systems, tasks, labels)
        val pi = alarmIntent()
        if (next == null) {
            alarms.cancel(pi)
            return
        }
        val at = toMillis(next.at)
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            // Exact alarms not allowed (default on Android 14+): deliver within a few minutes.
            alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MILLIS, pi)
        }
    }

    suspend fun scheduleNow() {
        schedule(c.systems.systems.first(), c.tasks.tasks.first(), c.labels.labels.first())
    }

    suspend fun onAlarm() {
        val now = LocalDateTime.now()
        val systems = c.systems.systems.first()
        val tasks = c.tasks.tasks.first()
        val labels = c.labels.labels.first()
        val until = now.plusSeconds(30)
        val start = prefs.getLong(KEY_HANDLED, 0L).let { if (it == 0L) now.minusMinutes(10) else handledUntil(now) }
        val due = ReminderPlanner.between(start, until, systems, tasks, labels)
        val withNudges = c.settings.settings.value.nudgesInReminders && due.any { it.kind == Reminder.Kind.SYSTEM_START }
        val completions = if (withNudges) c.habitCompletions(now.toLocalDate()).first() else emptyMap()
        due.forEach { r ->
            val nudge = if (withNudges && r.kind == Reminder.Kind.SYSTEM_START) {
                systems.firstOrNull { it.id == r.refId }?.let { HabitStats.nudge(HabitStats.history(it, tasks, completions, r.date), r.date) }
            } else null
            Notifications.post(context, r, systems, tasks, labels, nudge)
        }
        prefs.edit { putLong(KEY_HANDLED, toMillis(until)) }
        schedule(systems, tasks, labels)
    }

    companion object {
        private const val KEY_HANDLED = "handledUntil"
        private const val MAX_CATCH_UP_HOURS = 2L
        private const val WINDOW_MILLIS = 5 * 60_000L
    }
}
