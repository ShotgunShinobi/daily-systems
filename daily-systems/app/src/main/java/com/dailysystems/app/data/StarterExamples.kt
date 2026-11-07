package com.dailysystems.app.data

import androidx.room.withTransaction
import com.dailysystems.app.domain.DailySystem
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.domain.ItemOrder
import com.dailysystems.app.domain.ScheduleType
import com.dailysystems.app.domain.SystemItem
import java.time.DayOfWeek
import java.time.LocalDate

/** SPEC §4.6: one action that creates example systems, to-dos and a note (all flagged isExample). */
class StarterExamples(
    private val db: AppDatabase,
    private val labels: LabelRepository,
    private val tasks: TaskRepository,
    private val systems: SystemRepository,
    private val notes: NoteRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun load(today: LocalDate) = db.withTransaction {
        labels.seedDefaultsIfEmpty()
        val ls = labels.all()
        // Label at rank i (clamped to what exists), or none if the user deleted every label.
        fun l(i: Int): String? = ls.getOrNull(i.coerceAtMost(ls.lastIndex))?.id
        fun accepts(vararg ranks: Int): Set<String> = ranks.toList().mapNotNull { l(it) }.toSet()
        val now = clock()

        fun step(title: String, rank: Int) = SystemItem(newId(), ItemKind.STEP, title, l(rank))
        fun slot(title: String, rank: Int, count: Int, accepted: Set<String>) =
            SystemItem(newId(), ItemKind.SLOT, title, l(rank), slotCount = count, acceptedLabelIds = accepted)

        systems.create(
            DailySystem(
                id = newId(), name = "Morning launch", labelId = l(0), itemOrder = ItemOrder.PRIORITY,
                scheduleType = ScheduleType.DAILY, createdAt = now, isExample = true,
                items = listOf(
                    step("Water and a 10-minute walk", 1),
                    step("Review today's plan", 0),
                    slot("Top priorities", 0, 2, accepts(0, 1)),
                ),
            ),
        )
        systems.create(
            DailySystem(
                id = newId(), name = "Evening wind-down", labelId = l(1), itemOrder = ItemOrder.LISTED,
                scheduleType = ScheduleType.WEEKLY,
                weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                createdAt = now + 1, isExample = true,
                items = listOf(
                    slot("Small stuff", 2, 2, accepts(2, 3)),
                    step("Write tomorrow's top 3", 1),
                    step("Phone away by 11", 2),
                ),
            ),
        )
        val bigDay = systems.create(
            DailySystem(
                id = newId(), name = "Big-day prep", labelId = l(0), itemOrder = ItemOrder.LISTED,
                scheduleType = ScheduleType.DATES, createdAt = now + 2, isExample = true,
                items = listOf(
                    step("Lay out everything the night before", 0),
                    step("Re-read notes on the event", 0),
                    step("Leave 30 min early", 1),
                ),
            ),
        )

        tasks.create("Pay electricity bill", l(0), dueDate = today, isExample = true, createdAt = now)
        tasks.create("Reply to pending emails", l(1), isExample = true, createdAt = now + 1)
        tasks.create("Book dentist appointment", l(2), isExample = true, createdAt = now + 2)
        tasks.create("Sort photos from last trip", l(3), isExample = true, createdAt = now + 3)
        tasks.create(
            "Important appointment", l(0), dueDate = today.plusDays(4), runSystemId = bigDay.id,
            notes = "This to-do runs the Big-day prep system on its target date.",
            isExample = true, createdAt = now + 4,
        )

        notes.create(
            title = "How systems work",
            body = "Steps are things you always do.\nSlots are open sockets that pull in your highest-priority to-dos.\n" +
                "Finish a to-do and the next one plugs in automatically.\n\n" +
                "These starter items are examples. Edit or delete them freely.",
            isExample = true,
        )
    }
}
