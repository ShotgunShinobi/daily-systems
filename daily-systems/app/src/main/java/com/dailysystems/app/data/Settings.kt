package com.dailysystems.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    /** "Never miss twice" banner on Today. */
    val nudgesInApp: Boolean = true,
    /** Add the nudge to a system's start reminder (no extra notifications). */
    val nudgesInReminders: Boolean = false,
)

interface SettingsStore {
    val settings: StateFlow<AppSettings>
    fun update(change: (AppSettings) -> AppSettings)
}

class InMemorySettingsStore(initial: AppSettings = AppSettings()) : SettingsStore {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<AppSettings> = state.asStateFlow()
    override fun update(change: (AppSettings) -> AppSettings) {
        state.value = change(state.value)
    }
}

class PrefsSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(
        AppSettings(
            nudgesInApp = prefs.getBoolean(KEY_IN_APP, true),
            nudgesInReminders = prefs.getBoolean(KEY_IN_REMINDERS, false),
        ),
    )
    override val settings: StateFlow<AppSettings> = state.asStateFlow()

    override fun update(change: (AppSettings) -> AppSettings) {
        val next = change(state.value)
        state.value = next
        prefs.edit {
            putBoolean(KEY_IN_APP, next.nudgesInApp)
            putBoolean(KEY_IN_REMINDERS, next.nudgesInReminders)
        }
    }

    private companion object {
        const val KEY_IN_APP = "nudgesInApp"
        const val KEY_IN_REMINDERS = "nudgesInReminders"
    }
}
