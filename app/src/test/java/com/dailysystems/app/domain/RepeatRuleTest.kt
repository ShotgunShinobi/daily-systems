package com.dailysystems.app.domain

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatRuleTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private val tue = d(2026, 11, 3)

    @Test
    fun `daily and every n days`() {
        val daily = RepeatRule(RepeatUnit.DAY, 1, anchor = tue)
        assertEquals(tue.plusDays(1), daily.nextAfter(tue))
        val every3 = RepeatRule(RepeatUnit.DAY, 3, anchor = tue)
        assertEquals(tue.plusDays(3), every3.nextAfter(tue))
        assertEquals(tue.plusDays(6), every3.nextAfter(tue.plusDays(4)))
        assertTrue(every3.occursOn(tue.plusDays(9)))
        assertFalse(every3.occursOn(tue.plusDays(10)))
    }

    @Test
    fun `before the anchor the next occurrence is the anchor`() {
        assertEquals(tue, RepeatRule(RepeatUnit.DAY, 2, anchor = tue).nextAfter(tue.minusDays(5)))
        assertEquals(tue, RepeatRule(RepeatUnit.MONTH, 1, anchor = tue).nextAfter(tue.minusDays(40)))
    }

    @Test
    fun `weekly on chosen weekdays with an interval`() {
        val r = RepeatRule(RepeatUnit.WEEK, 1, setOf(MONDAY, THURSDAY), anchor = tue)
        assertEquals(d(2026, 11, 5), r.nextAfter(tue)) // Thu
        assertEquals(d(2026, 11, 9), r.nextAfter(d(2026, 11, 5))) // next Mon
        val fortnight = RepeatRule(RepeatUnit.WEEK, 2, setOf(FRIDAY), anchor = tue)
        assertEquals(d(2026, 11, 6), fortnight.nextAfter(tue))
        assertEquals(d(2026, 11, 20), fortnight.nextAfter(d(2026, 11, 6)))
        assertFalse(fortnight.occursOn(d(2026, 11, 13)))
    }

    @Test
    fun `weekly with no days uses the anchor weekday`() {
        val r = RepeatRule(RepeatUnit.WEEK, 1, emptySet(), anchor = tue)
        assertEquals(tue.plusWeeks(1), r.nextAfter(tue))
        assertEquals(DayOfWeek.TUESDAY, r.nextAfter(tue.plusDays(3)).dayOfWeek)
    }

    @Test
    fun `monthly on the 31st clamps in short months without drifting`() {
        val r = RepeatRule(RepeatUnit.MONTH, 1, anchor = d(2026, 1, 31))
        assertEquals(d(2026, 2, 28), r.nextAfter(d(2026, 1, 31)))
        assertEquals(d(2026, 3, 31), r.nextAfter(d(2026, 2, 28)))
        assertEquals(d(2026, 4, 30), r.nextAfter(d(2026, 3, 31)))
        assertTrue(r.occursOn(d(2026, 2, 28)))
        assertFalse(r.occursOn(d(2026, 3, 28)))
    }

    @Test
    fun `every 3 months and yearly including leap day`() {
        val q = RepeatRule(RepeatUnit.MONTH, 3, anchor = d(2026, 1, 15))
        assertEquals(d(2026, 4, 15), q.nextAfter(d(2026, 1, 15)))
        assertEquals(d(2026, 7, 15), q.nextAfter(d(2026, 5, 1)))
        val leap = RepeatRule(RepeatUnit.YEAR, 1, anchor = d(2028, 2, 29))
        assertEquals(d(2029, 2, 28), leap.nextAfter(d(2028, 2, 29)))
        assertEquals(d(2032, 2, 29), leap.nextAfter(d(2031, 2, 28)))
    }

    @Test
    fun `completing overdue or early schedules from the later of due and done`() {
        val daily = RepeatRule(RepeatUnit.DAY, 1, anchor = tue)
        val overdue = Task("t", "t", dueDate = tue.minusDays(3))
        assertEquals(tue.plusDays(1), Recurrence.nextDue(overdue, daily, tue))
        val weekly = RepeatRule(RepeatUnit.WEEK, 1, setOf(FRIDAY), anchor = d(2026, 11, 6))
        val early = Task("t", "t", dueDate = d(2026, 11, 6))
        assertEquals(d(2026, 11, 13), Recurrence.nextDue(early, weekly, tue)) // done Tue, due Fri → next Fri
    }
}
