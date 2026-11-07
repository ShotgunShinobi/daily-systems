package com.dailysystems.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class RepeatUnit { DAY, WEEK, MONTH, YEAR }

/**
 * How a recurring to-do repeats. Occurrences are counted from [anchor] (the first due date),
 * so monthly/yearly series never drift: "every month on the 31st" lands on the 28th/29th/30th in
 * short months and back on the 31st afterwards.
 */
data class RepeatRule(
    val unit: RepeatUnit,
    val interval: Int = 1,
    /** WEEK only; empty = the anchor's weekday. */
    val weekdays: Set<DayOfWeek> = emptySet(),
    val anchor: LocalDate,
) {
    private val n: Int get() = interval.coerceAtLeast(1)
    private val days: Set<DayOfWeek> get() = weekdays.ifEmpty { setOf(anchor.dayOfWeek) }

    fun occursOn(d: LocalDate): Boolean {
        if (d.isBefore(anchor)) return false
        return when (unit) {
            RepeatUnit.DAY -> ChronoUnit.DAYS.between(anchor, d) % n == 0L
            RepeatUnit.WEEK -> d.dayOfWeek in days && ChronoUnit.WEEKS.between(monday(anchor), monday(d)) % n == 0L
            RepeatUnit.MONTH -> monthOccurrence(d, 1)
            RepeatUnit.YEAR -> monthOccurrence(d, 12)
        }
    }

    /** The first occurrence strictly after [after]. */
    fun nextAfter(after: LocalDate): LocalDate {
        val from = if (after.isBefore(anchor)) anchor.minusDays(1) else after
        return when (unit) {
            RepeatUnit.DAY -> {
                val elapsed = ChronoUnit.DAYS.between(anchor, from)
                anchor.plusDays((Math.floorDiv(elapsed, n.toLong()) + 1) * n)
            }
            RepeatUnit.WEEK -> generateSequence(from.plusDays(1)) { it.plusDays(1) }.first { occursOn(it) }
            RepeatUnit.MONTH, RepeatUnit.YEAR -> {
                val step = if (unit == RepeatUnit.MONTH) n else 12 * n
                var k = (ChronoUnit.MONTHS.between(anchor, from) / step - 1).coerceAtLeast(0)
                var d = anchor.plusMonths(k * step)
                while (!d.isAfter(from)) {
                    k++
                    d = anchor.plusMonths(k * step)
                }
                d
            }
        }
    }

    private fun monthOccurrence(d: LocalDate, monthsPerUnit: Int): Boolean {
        val months = ChronoUnit.MONTHS.between(anchor.withDayOfMonth(1), d.withDayOfMonth(1))
        if (months % (monthsPerUnit * n) != 0L) return false
        return anchor.plusMonths(months) == d
    }

    private fun monday(d: LocalDate) = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
