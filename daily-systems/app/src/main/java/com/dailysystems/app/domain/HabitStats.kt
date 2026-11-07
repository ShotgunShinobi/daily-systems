package com.dailysystems.app.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** How a system went on one of its run days, judged by its steps. */
enum class DayResult {
    /** Every step checked. */
    COMPLETE,
    /** At least one step checked: showing up counts. */
    PARTIAL,
    /** A past run day with nothing checked. */
    MISSED,
    /** Today, nothing checked yet. Never counts as a miss until the day is over. */
    PENDING,
}

data class StepStats(val item: SystemItem, val runs: Int, val done: Int, val currentStreak: Int) {
    val rate: Float get() = if (runs == 0) 0f else done.toFloat() / runs
}

data class SystemHistory(
    val system: DailySystem,
    /** Run days from the system's start to today, oldest first. */
    val days: Map<LocalDate, DayResult>,
    val currentStreak: Int,
    val bestStreak: Int,
    /** Kept run days / past run days over the last 30 days; null when it hasn't run yet. */
    val rate30: Float?,
    /** Missed run days in a row just before today (0 = last run was kept). */
    val missesBeforeToday: Int,
    val steps: List<StepStats>,
) {
    fun isKept(d: LocalDate) = days[d] == DayResult.COMPLETE || days[d] == DayResult.PARTIAL
}

data class SystemWeek(
    val system: DailySystem,
    val runDays: Int,
    val kept: Int,
    val complete: Int,
    val currentStreak: Int,
    val bestStreak: Int,
)

data class MissedStep(val systemName: String, val stepTitle: String, val missed: Int, val runs: Int)

data class WeekReview(
    val weekStart: LocalDate,
    val systems: List<SystemWeek>,
    /** Kept / counted run days this week (today counts only once something is checked). */
    val keptRate: Float?,
    val previousKeptRate: Float?,
    val stepsChecked: Int,
    val todosDone: Int,
    val mostMissed: List<MissedStep>,
)

/**
 * Habit history computed from step check-offs. Pure Kotlin.
 *
 * Run days come from the system's *current* schedule (plus target-date runs) back to the day it
 * was created; the app keeps no history of earlier schedules or pauses.
 */
object HabitStats {
    /** How far back streaks and history look. */
    const val LOOKBACK_DAYS = 730L

    fun startDate(system: DailySystem, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        val created = Instant.ofEpochMilli(system.createdAt).atZone(zone).toLocalDate()
        val earliest = today.minusDays(LOOKBACK_DAYS)
        return if (created.isBefore(earliest)) earliest else created
    }

    fun runsOn(system: DailySystem, tasks: Collection<Task>, d: LocalDate): Boolean =
        DayPlanner.scheduledOn(system, d) || DayPlanner.targetRun(system, tasks, d)

    /** `null` for systems without steps: there is nothing to keep a streak on. */
    fun history(
        system: DailySystem,
        tasks: Collection<Task>,
        completions: Map<LocalDate, Set<StepKey>>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): SystemHistory? {
        val steps = system.items.filter { it.kind == ItemKind.STEP }
        if (steps.isEmpty()) return null
        val stepIds = steps.map { it.id }.toSet()

        val days = LinkedHashMap<LocalDate, DayResult>()
        var d = startDate(system, today, zone)
        while (!d.isAfter(today)) {
            if (runsOn(system, tasks, d)) {
                val checked = completions[d].orEmpty().count { it.systemId == system.id && it.itemId in stepIds }
                days[d] = when {
                    checked >= steps.size -> DayResult.COMPLETE
                    checked > 0 -> DayResult.PARTIAL
                    d == today -> DayResult.PENDING
                    else -> DayResult.MISSED
                }
            }
            d = d.plusDays(1)
        }

        val results = days.values.filter { it != DayResult.PENDING }
        val kept = { r: DayResult -> r == DayResult.COMPLETE || r == DayResult.PARTIAL }
        val current = results.asReversed().takeWhile(kept).size
        var best = 0
        var run = 0
        results.forEach { if (kept(it)) { run++; if (run > best) best = run } else run = 0 }

        val past = days.entries.filter { it.key != today }
        val misses = past.asReversed().takeWhile { it.value == DayResult.MISSED }.size
        val last30 = days.filterKeys { !it.isBefore(today.minusDays(29)) }.values.filter { it != DayResult.PENDING }
        val rate30 = if (last30.isEmpty()) null else last30.count(kept).toFloat() / last30.size

        val runDays = days.filterValues { it != DayResult.PENDING }.keys.toList()
        val stepStats = steps.map { step ->
            // A step can't have been missed before it existed; it counts from its first check-off.
            val first = runDays.firstOrNull { StepKey(system.id, step.id) in completions[it].orEmpty() }
            val window = if (first == null) emptyList() else runDays.filter { !it.isBefore(first) }
            val doneDays = window.map { StepKey(system.id, step.id) in completions[it].orEmpty() }
            StepStats(step, window.size, doneDays.count { it }, doneDays.asReversed().takeWhile { it }.size)
        }

        return SystemHistory(system, days, current, best, rate30, misses, stepStats)
    }

    fun weekStart(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekReview(
        weekStart: LocalDate,
        systems: Collection<DailySystem>,
        tasks: Collection<Task>,
        completions: Map<LocalDate, Set<StepKey>>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): WeekReview {
        val week = (0L until 7L).map { weekStart.plusDays(it) }.filter { !it.isAfter(today) }
        val histories = systems.filter { it.active }.mapNotNull { history(it, tasks, completions, today, zone) }

        fun rateFor(days: List<LocalDate>): Float? {
            var counted = 0
            var keptCount = 0
            histories.forEach { h ->
                days.forEach { d ->
                    when (h.days[d]) {
                        DayResult.COMPLETE, DayResult.PARTIAL -> { counted++; keptCount++ }
                        DayResult.MISSED -> counted++
                        else -> Unit
                    }
                }
            }
            return if (counted == 0) null else keptCount.toFloat() / counted
        }

        val perSystem = histories.map { h ->
            val inWeek = week.mapNotNull { h.days[it] }.filter { it != DayResult.PENDING }
            SystemWeek(
                h.system, inWeek.size,
                inWeek.count { it == DayResult.COMPLETE || it == DayResult.PARTIAL },
                inWeek.count { it == DayResult.COMPLETE },
                h.currentStreak, h.bestStreak,
            )
        }.filter { it.runDays > 0 }

        val mostMissed = histories.flatMap { h ->
            val runDays = week.filter { h.days[it] != null && h.days[it] != DayResult.PENDING }
            h.system.items.filter { it.kind == ItemKind.STEP }.map { step ->
                val missed = runDays.count { StepKey(h.system.id, step.id) !in completions[it].orEmpty() }
                MissedStep(h.system.name.ifBlank { "Untitled system" }, step.title.ifBlank { "Untitled step" }, missed, runDays.size)
            }
        }.filter { it.missed > 0 }.sortedWith(compareByDescending<MissedStep> { it.missed }.thenBy { it.stepTitle }).take(3)

        val weekSet = week.toSet()
        return WeekReview(
            weekStart = weekStart,
            systems = perSystem,
            keptRate = rateFor(week),
            previousKeptRate = rateFor((0L until 7L).map { weekStart.minusWeeks(1).plusDays(it) }),
            stepsChecked = week.sumOf { d -> completions[d].orEmpty().size },
            todosDone = tasks.count { it.done && it.doneOn in weekSet },
            mostMissed = mostMissed,
        )
    }

    /**
     * "Never miss twice": the message to show for [system] today, or null. Only when it runs today,
     * isn't paused, nothing is checked yet, and its last run was missed.
     */
    fun nudge(history: SystemHistory?, today: LocalDate): String? {
        if (history == null || !history.system.active) return null
        if (history.days[today] != DayResult.PENDING) return null
        return when (history.missesBeforeToday) {
            0 -> null
            1 -> "Missed last time. Never miss twice: even one step today keeps it going."
            else -> "Missed the last ${history.missesBeforeToday} runs. Start again small: one step today counts."
        }
    }
}
