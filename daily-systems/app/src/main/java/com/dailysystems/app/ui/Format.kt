package com.dailysystems.app.ui

import com.dailysystems.app.domain.DailySystem
import com.dailysystems.app.domain.RepeatRule
import com.dailysystems.app.domain.RepeatUnit
import com.dailysystems.app.domain.ScheduleType
import com.dailysystems.app.domain.Task
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Presentation formatting. Pure functions so they're easy to test and preview. */
object Format {
    private val locale: Locale get() = Locale.getDefault()

    fun shortDate(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMM", locale))
    fun shortDateYear(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
    fun longDate(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("EEE, d MMMM yyyy", locale))
    fun weekdayLong(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    fun weekdayShort(d: DayOfWeek): String = d.getDisplayName(TextStyle.SHORT, locale)
    fun weekdayNarrow(d: DayOfWeek): String = d.getDisplayName(TextStyle.SHORT, locale).take(2)

    fun relativeDay(d: LocalDate, today: LocalDate): String = when (d) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> weekdayLong(d)
    }

    fun ordinal(n: Int): String {
        val suffix = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
        return "$n$suffix"
    }

    fun timestamp(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM, HH:mm", locale))

    fun dayMonth(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().let(::shortDate)

    fun time(t: LocalTime): String = t.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))

    /** "Every day", "Every 2 weeks on Mon, Thu", "Monthly on the 31st", "Every year on 3 Nov". */
    fun repeatSummary(r: RepeatRule): String {
        val n = r.interval.coerceAtLeast(1)
        return when (r.unit) {
            RepeatUnit.DAY -> if (n == 1) "Every day" else "Every $n days"
            RepeatUnit.WEEK -> {
                val days = r.weekdays.ifEmpty { setOf(r.anchor.dayOfWeek) }
                val on = DayOfWeek.entries.filter { it in days }.joinToString(", ") { weekdayShort(it) }
                (if (n == 1) "Weekly" else "Every $n weeks") + " on $on"
            }
            RepeatUnit.MONTH -> (if (n == 1) "Monthly" else "Every $n months") + " on the ${ordinal(r.anchor.dayOfMonth)}"
            RepeatUnit.YEAR -> (if (n == 1) "Every year" else "Every $n years") + " on ${shortDate(r.anchor)}"
        }
    }

    fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"

    /** "Every day", "Weekly · Mon Tue", "Monthly · 1st, last day", "2 dates · next 4 Mar" (+ target dates, paused). */
    fun scheduleSummary(system: DailySystem, tasks: Collection<Task>, today: LocalDate): String {
        var t = when (system.scheduleType) {
            ScheduleType.DAILY -> "Every day"
            ScheduleType.WEEKLY ->
                if (system.weekdays.isEmpty()) "Weekly · no days picked"
                else "Weekly · " + DayOfWeek.entries.filter { it in system.weekdays }.joinToString(" ") { weekdayShort(it) }
            ScheduleType.MONTHLY ->
                if (system.monthDays.isEmpty()) "Monthly · no days picked"
                else "Monthly · " + system.monthDays.sortedBy { if (it == DailySystem.LAST_DAY) 99 else it }
                    .joinToString(", ") { if (it == DailySystem.LAST_DAY) "last day" else ordinal(it) }
            ScheduleType.DATES -> {
                val ds = system.dates.sorted()
                if (ds.isEmpty()) "Specific dates · none yet"
                else plural(ds.size, "date") + (ds.firstOrNull { !it.isBefore(today) }?.let { " · next ${shortDate(it)}" } ?: "")
            }
        }
        val targets = tasks.count { it.runSystemId == system.id && it.dueDate != null && !it.done }
        if (targets > 0) t += " · +${plural(targets, "target date")}"
        system.reminderTime?.let { t += " · at ${time(it)}" }
        if (!system.active) t = "Paused · $t"
        return t
    }
}
