package com.dailysystems.app

import android.content.Context
import com.dailysystems.app.reminders.ReminderScheduler
import com.dailysystems.app.widget.TodayWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Whenever data (or the date) changes: re-plan the next reminder alarm and refresh the widget.
 * Runs for the life of the app process; every edit happens in-process, so nothing is missed.
 */
class BackgroundSync(private val context: Context, private val c: AppContainer, private val reminders: ReminderScheduler) {
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun start() {
        c.appScope.launch {
            val steps = c.today.flatMapLatest { c.systems.checkedSteps(it) }
            combine(c.labels.labels, c.systems.systems, c.tasks.tasks, c.today, steps) { l, s, t, _, _ -> Triple(l, s, t) }
                .debounce(400)
                .collect { (labels, systems, tasks) ->
                    runCatching { reminders.schedule(systems, tasks, labels) }
                    TodayWidget.refresh(context)
                }
        }
    }
}
