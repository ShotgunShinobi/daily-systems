package com.dailysystems.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailysystems.app.domain.DayResult
import com.dailysystems.app.domain.HabitStats
import com.dailysystems.app.domain.SystemHistory
import com.dailysystems.app.ui.Format
import com.dailysystems.app.ui.theme.AppTheme
import java.time.DayOfWeek
import java.time.LocalDate

/** "5 in a row": runs in a row, not days, since many systems don't run daily. */
@Composable
fun StreakBadge(streak: Int, modifier: Modifier = Modifier) {
    if (streak < 2) return
    val c = AppTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(99.dp))
            .background(c.accentSoft)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Streak: $streak runs in a row" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Repeat, null, Modifier.size(11.dp), tint = c.accent)
        Spacer(Modifier.width(4.dp))
        Text("$streak in a row", style = AppTheme.type.monoSmall.copy(fontSize = 10.5.sp), color = c.ink)
    }
}

/** Gentle "never miss twice" banner. */
@Composable
fun NudgeBanner(text: String, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.accentSoft)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(c.accent))
        Spacer(Modifier.width(10.dp))
        Text(text, style = TextStyles.rowSub.copy(fontSize = 13.sp), color = c.ink)
    }
}

/** One cell's look: filled = complete, half = partial, ring = missed, dashed-ish = today pending. */
@Composable
private fun ResultDot(result: DayResult?, size: Dp) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(size / 3)
    val base = Modifier.size(size).clip(shape)
    when (result) {
        DayResult.COMPLETE -> Box(base.background(c.accent))
        DayResult.PARTIAL -> Box(base.background(c.accent.copy(alpha = 0.45f)))
        DayResult.MISSED -> Box(base.border(1.5.dp, c.danger.copy(alpha = 0.7f), shape))
        DayResult.PENDING -> Box(base.border(1.5.dp, c.accent, shape))
        null -> Box(base.background(c.line.copy(alpha = 0.5f)))
    }
}

/** The last [count] run days, oldest left. */
@Composable
fun RecentRunsStrip(history: SystemHistory, count: Int = 14, modifier: Modifier = Modifier) {
    val recent = history.days.entries.toList().takeLast(count)
    if (recent.isEmpty()) return
    val kept = recent.count { it.value == DayResult.COMPLETE || it.value == DayResult.PARTIAL }
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = "Kept $kept of the last ${recent.size} runs" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        recent.forEach { ResultDot(it.value, 9.dp) }
    }
}

/** Calendar grid: one column per week (Mon at top), last [weeks] weeks; grey = not a run day. */
@Composable
fun HistoryGrid(history: SystemHistory, today: LocalDate, weeks: Int = 15, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val firstMonday = HabitStats.weekStart(today).minusWeeks((weeks - 1).toLong())
    Column(modifier) {
        Row {
            Column(Modifier.padding(end = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                DayOfWeek.entries.forEach { d ->
                    Box(Modifier.size(width = 14.dp, height = 13.dp), contentAlignment = Alignment.CenterStart) {
                        if (d.value % 2 == 1) Text(Format.weekdayNarrow(d).take(1), style = AppTheme.type.monoSmall.copy(fontSize = 9.sp), color = c.muted)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                (0 until weeks).forEach { w ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        (0L until 7L).forEach { k ->
                            val d = firstMonday.plusWeeks(w.toLong()).plusDays(k)
                            if (d.isAfter(today)) Box(Modifier.size(13.dp)) else ResultDot(history.days[d], 13.dp)
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(DayResult.COMPLETE, "all steps")
            Legend(DayResult.PARTIAL, "some")
            Legend(DayResult.MISSED, "missed")
        }
    }
}

@Composable
private fun Legend(r: DayResult, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ResultDot(r, 10.dp)
        Spacer(Modifier.width(4.dp))
        Text(text, style = TextStyles.rowSub, color = AppTheme.colors.muted)
    }
}

/** Big number + caption, used in history and the review. */
@Composable
fun StatTile(value: String, caption: String, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, style = AppTheme.type.cardTitle.copy(fontSize = 22.sp), color = c.ink)
        Text(caption, style = TextStyles.rowSub, color = c.muted)
    }
}

fun percent(f: Float?): String = f?.let { "${Math.round(it * 100)}%" } ?: "–"

