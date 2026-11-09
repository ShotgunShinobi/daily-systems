package com.dailysystems.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.reminders.ReminderScheduler
import com.dailysystems.app.widget.WidgetLine
import com.dailysystems.app.widget.loadWidgetData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RemindersAndWidgetTest {
    private val app = ApplicationProvider.getApplicationContext<DailySystemsApp>()
    private val today = LocalDate.now()
    private val c = testContainer(today)

    @Before
    fun setUp() {
        app.replaceContainer(c)
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        runBlocking { c.labels.seedDefaultsIfEmpty() }
    }

    private fun millis(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun schedulesTheNextReminderAndPostsDueOnes() = runBlocking {
        val now = LocalDateTime.now()
        val soon = now.plusMinutes(5).withSecond(0).withNano(0)
        val later = now.plusHours(3).withSecond(0).withNano(0)
        // Skip the edge case of running right before midnight (the reminders would land tomorrow).
        org.junit.Assume.assumeTrue(later.toLocalDate() == today)
        val t = c.tasks.create("Call bank", labelId = "label-must", dueDate = today)
        c.tasks.setReminderTime(t.id, soon.toLocalTime())
        val s = c.systems.createBlank("label-must")
        c.systems.setName(s.id, "Evening")
        c.systems.addItem(s.id, ItemKind.STEP, null)
        c.systems.setReminderTime(s.id, later.toLocalTime())

        val scheduler = ReminderScheduler(app, c)
        scheduler.scheduleNow()
        val alarms = shadowOf(app.getSystemService(AlarmManager::class.java))
        assertEquals(millis(soon), alarms.peekNextScheduledAlarm()!!.triggerAtTime)

        // Pretend the alarm fired late, after the to-do time: it is still posted.
        app.getSharedPreferences("reminders", 0).edit().putLong("handledUntil", millis(now.minusMinutes(1))).apply()
        c.tasks.setReminderTime(t.id, now.minusSeconds(10).toLocalTime())
        scheduler.onAlarm()
        val posted = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications
        assertEquals(1, posted.size)
        assertEquals("Call bank", posted.single().extras.getString("android.title"))
        assertEquals("Mark done", posted.single().actions.single().title)
        // Next alarm is the system start.
        assertEquals(millis(later), alarms.peekNextScheduledAlarm()!!.triggerAtTime)
    }

    @Test
    fun widgetShowsTodaysPlan() = runBlocking {
        c.starterExamples.load(today)
        val d = loadWidgetData(app, today)
        val headers = d.lines.filterIsInstance<WidgetLine.Header>().map { it.title }
        assertTrue(headers.first() == "Morning launch")
        val checks = d.lines.filterIsInstance<WidgetLine.Check>().map { it.title }
        assertTrue("Pay electricity bill" in checks)
        assertTrue("Review today's plan" in checks)
        assertEquals(0, d.done)
        assertTrue(d.total >= 4)
    }

    @Test
    fun startReminderCarriesTheNudgeWhenEnabled() = runBlocking {
        val now = LocalDateTime.now()
        org.junit.Assume.assumeTrue(now.toLocalTime().isAfter(LocalTime.of(0, 15)))
        val zone = ZoneId.systemDefault()
        c.systems.create(
            com.dailysystems.app.domain.DailySystem(
                id = "habit", name = "Habit", createdAt = millis(today.minusDays(5).atStartOfDay()),
                reminderTime = now.minusMinutes(1).toLocalTime().withSecond(0).withNano(0),
                items = listOf(com.dailysystems.app.domain.SystemItem("walk", ItemKind.STEP, "Walk")),
            ),
        )
        c.systems.toggleStep("habit", "walk", today.minusDays(2))
        c.settings.update { it.copy(nudgesInReminders = true) }
        app.getSharedPreferences("reminders", 0).edit().putLong("handledUntil", millis(now.minusMinutes(5))).apply()
        ReminderScheduler(app, c).onAlarm()
        val n = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
        assertEquals("Habit starts now", n.extras.getString("android.title"))
        assertTrue(n.extras.getCharSequence("android.text").toString().startsWith("Missed last time"))
        check(zone == ZoneId.systemDefault())
    }

    @Test
    fun widgetDataIsLiveSoAWidgetTapShowsUpRightAway() = runBlocking {
        c.starterExamples.load(today)
        val emissions = mutableListOf<com.dailysystems.app.widget.WidgetData>()
        val job = launch(kotlinx.coroutines.Dispatchers.Default) {
            com.dailysystems.app.widget.widgetDataFlow(app, kotlinx.coroutines.flow.flowOf(today)).collect { emissions += it }
        }
        kotlinx.coroutines.withTimeout(5_000) { while (emissions.isEmpty()) kotlinx.coroutines.delay(20) }
        val bill = emissions.last().lines.filterIsInstance<WidgetLine.Check>().single { it.title == "Pay electricity bill" }
        assertEquals(false, bill.done)

        // Same write the widget's tap action makes; no explicit widget update needed.
        c.writeAwait { c.tasks.toggleDone((bill.action as com.dailysystems.app.widget.Toggle.Todo).taskId, today) }
        kotlinx.coroutines.withTimeout(5_000) {
            while (emissions.last().lines.filterIsInstance<WidgetLine.Check>().none { it.title == "Pay electricity bill" && it.done }) kotlinx.coroutines.delay(20)
        }
        assertEquals(1, emissions.last().done)
        // The row keeps its key when checked, so the list item keeps its identity.
        assertEquals(bill.key, emissions.last().lines.filterIsInstance<WidgetLine.Check>().single { it.title == "Pay electricity bill" }.key)
        job.cancel()
    }
}
