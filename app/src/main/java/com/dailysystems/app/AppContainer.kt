package com.dailysystems.app

import android.content.Context
import com.dailysystems.app.data.AppDatabase
import com.dailysystems.app.data.LabelRepository
import com.dailysystems.app.data.NoteRepository
import com.dailysystems.app.data.StarterExamples
import com.dailysystems.app.data.SystemRepository
import com.dailysystems.app.data.TaskRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Manual DI: one instance per process, owned by [DailySystemsApp]. */
class AppContainer(
    val db: AppDatabase,
    /** Source of "today"; replaced in tests. */
    todaySource: Flow<LocalDate> = midnightTicker(),
) {
    /** Outlives screens, so writes fired as a sheet closes still complete. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val labels = LabelRepository(db)
    val tasks = TaskRepository(db)
    val systems = SystemRepository(db)
    val notes = NoteRepository(db)
    val starterExamples = StarterExamples(db, labels, tasks, systems, notes)

    /** The current date; ticks over after midnight so Today refreshes (SPEC §6). */
    val today: StateFlow<LocalDate> = todaySource.distinctUntilChanged()
        .stateIn(appScope, SharingStarted.Eagerly, LocalDate.now())

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

    companion object {
        fun create(context: Context) = AppContainer(AppDatabase.build(context.applicationContext))

        fun midnightTicker(): Flow<LocalDate> = flow {
            while (true) {
                emit(LocalDate.now())
                val now = LocalDateTime.now()
                val untilMidnight = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()
                delay((untilMidnight + 50).coerceIn(1_000, 60_000))
            }
        }
    }
}
