package com.dailysystems.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId

/**
 * One alarm just after midnight that rolls Today and the widget over to the new day.
 *
 * It is a non-wakeup (RTC) alarm: if the phone is asleep at midnight, Android holds it until the
 * phone next wakes for any reason (usually the screen turning on) and delivers it then. So the
 * rollover never wakes the phone by itself, and the widget is right by the time anyone looks.
 */
class DayRollover(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun nextMidnightMillis(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Long =
        today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 1_000

    /** Idempotent: re-setting replaces the pending alarm. */
    fun schedule() {
        alarms.set(AlarmManager.RTC, nextMidnightMillis(), intent())
    }

    private fun intent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, MidnightReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_CODE = 1
    }
}
