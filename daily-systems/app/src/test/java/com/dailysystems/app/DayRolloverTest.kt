package com.dailysystems.app

import android.app.AlarmManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dailysystems.app.data.AppDatabase
import com.dailysystems.app.reminders.DayRollover
import com.dailysystems.app.reminders.MidnightReceiver
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DayRolloverTest {
    private val app = ApplicationProvider.getApplicationContext<DailySystemsApp>()
    private val alarms = shadowOf(app.getSystemService(AlarmManager::class.java))

    private fun midnightAlarms() = alarms.scheduledAlarms.filter {
        it.operation?.let { op -> shadowOf(op).savedIntent.component?.className } == MidnightReceiver::class.java.name
    }

    @Test
    fun midnightAlarmIsASingleNonWakeupAlarmJustAfterMidnight() {
        DayRollover(app).schedule()
        DayRollover(app).schedule() // idempotent
        val a = midnightAlarms().single()
        assertEquals(AlarmManager.RTC, a.type) // not RTC_WAKEUP: never wakes the phone itself
        val expected = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 1_000
        assertEquals(expected, a.triggerAtTime)
    }

    @Test
    fun appStartSchedulesNoRepeatingTimers() {
        // Application.onCreate already ran: only the midnight alarm (plus reminders, none here).
        assertTrue(alarms.scheduledAlarms.none { it.interval > 0 })
        assertEquals(1, midnightAlarms().size)
    }

    @Test
    fun refreshTodayRollsTheDateOverOnlyWhenItChanges() {
        var clock = LocalDate.of(2026, 11, 9)
        val c = AppContainer(AppDatabase.inMemory(app), dateClock = { clock })
        assertEquals(LocalDate.of(2026, 11, 9), c.today.value)
        c.refreshToday()
        assertEquals(LocalDate.of(2026, 11, 9), c.today.value)
        clock = clock.plusDays(1)
        c.refreshToday()
        // today is a StateFlow fed through stateIn on a background scope; give it a moment.
        val deadline = System.currentTimeMillis() + 5_000
        while (c.today.value != clock && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertEquals(LocalDate.of(2026, 11, 10), c.today.value)
    }

    @Test
    fun midnightReceiverReschedulesTheNextRollover() {
        alarms.scheduledAlarms.toList().forEach { a -> a.operation?.let { app.getSystemService(AlarmManager::class.java).cancel(it) } }
        MidnightReceiver().onReceive(app, Intent())
        val deadline = System.currentTimeMillis() + 5_000
        while (midnightAlarms().isEmpty() && System.currentTimeMillis() < deadline) {
            shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertEquals(1, midnightAlarms().size)
    }
}
