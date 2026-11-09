package com.dailysystems.app

import android.app.Application
import com.dailysystems.app.reminders.DayRollover
import com.dailysystems.app.reminders.Notifications
import com.dailysystems.app.reminders.ReminderScheduler

class DailySystemsApp : Application() {
    lateinit var container: AppContainer
        private set
    lateinit var reminders: ReminderScheduler
        private set
    lateinit var dayRollover: DayRollover
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.create(this)
        container.seed()
        reminders = ReminderScheduler(this, container)
        Notifications.ensureChannel(this)
        dayRollover = DayRollover(this).also { it.schedule() }
        BackgroundSync(this, container, reminders).start()
    }

    /** Lets tests swap in an in-memory container before the activity starts. */
    fun replaceContainer(c: AppContainer) {
        container = c
    }
}
