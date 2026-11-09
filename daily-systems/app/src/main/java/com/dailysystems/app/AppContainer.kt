package com.dailysystems.app

import android.content.Context
import com.dailysystems.app.data.AppDatabase
import com.dailysystems.app.data.InMemorySettingsStore
import com.dailysystems.app.data.LabelRepository
import com.dailysystems.app.data.PrefsSettingsStore
import com.dailysystems.app.data.SettingsStore
import com.dailysystems.app.data.NoteRepository
import com.dailysystems.app.data.StarterExamples
import com.dailysystems.app.data.SystemRepository
import com.dailysystems.app.data.TaskRepository
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Manual DI: one instance per process, owned by [DailySystemsApp]. */
class AppContainer(
    val db: AppDatabase,
    /** Fixed source of "today" for tests; null = the real clock, refreshed by [refreshToday]. */
    todaySource: Flow<LocalDate>? = null,
    val settings: SettingsStore = InMemorySettingsStore(),
    /** Reads the current date; replaced in tests. */
    private val dateClock: () -> LocalDate = { LocalDate.now() },
) {
    /** Outlives screens, so writes fired as a sheet closes still complete. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val labels = LabelRepository(db)
    val tasks = TaskRepository(db)
    val systems = SystemRepository(db)
    val notes = NoteRepository(db)
    val starterExamples = StarterExamples(db, labels, tasks, systems, notes)

    private val clockDate = MutableStateFlow(dateClock())

    /**
     * The current date (SPEC §6 day rollover). Nothing polls the clock: [refreshToday] is called by
     * the midnight alarm, when the app comes to the foreground, and on clock/time-zone changes.
     */
    val today: StateFlow<LocalDate> = (todaySource ?: clockDate).distinctUntilChanged()
        .stateIn(appScope, SharingStarted.Eagerly, todaySource?.let { LocalDate.now() } ?: clockDate.value)

    /** Re-read the clock; a no-op unless the date actually changed. */
    fun refreshToday() {
        clockDate.value = dateClock()
    }

    // Edits save immediately, one keystroke at a time. A single consumer keeps them in order.
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        appScope.launch { for (w in writes) runCatching { w() } }
    }

    /** Queue a write; returns immediately. */
    fun write(block: suspend () -> Unit) {
        writes.trySend(block)
    }

    /** Queue a write and wait for its result (e.g. the id of something just created). */
    suspend fun <T> writeAwait(block: suspend () -> T): T {
        val result = CompletableDeferred<T>()
        writes.send { result.completeWith(runCatching { block() }) }
        return result.await()
    }

    fun seed() = write { labels.seedDefaultsIfEmpty() }

    /** Check-offs over the habit-history window ending [today]. */
    fun habitCompletions(today: LocalDate) =
        systems.completionsBetween(today.minusDays(com.dailysystems.app.domain.HabitStats.LOOKBACK_DAYS), today)

    companion object {
        fun create(context: Context) =
            AppContainer(AppDatabase.build(context.applicationContext), settings = PrefsSettingsStore(context.applicationContext))

    }
}
