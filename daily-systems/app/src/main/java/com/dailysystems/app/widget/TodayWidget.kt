package com.dailysystems.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.dailysystems.app.DailySystemsApp
import com.dailysystems.app.MainActivity
import com.dailysystems.app.R
import com.dailysystems.app.domain.DayPlanner
import com.dailysystems.app.domain.PlannedRow
import com.dailysystems.app.domain.StepKey
import com.dailysystems.app.ui.Format
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/** One line in the widget list. */
sealed interface WidgetLine {
    data class Header(val title: String, val meta: String) : WidgetLine
    data class Check(
        val key: String,
        val title: String,
        val done: Boolean,
        val colorArgb: Int?,
        val indent: Boolean,
        val action: Toggle,
    ) : WidgetLine
    data class Open(val key: String, val text: String) : WidgetLine
}

sealed interface Toggle {
    data class Step(val systemId: String, val itemId: String) : Toggle
    data class Todo(val taskId: String) : Toggle
}

data class WidgetData(val date: LocalDate, val done: Int, val total: Int, val lines: List<WidgetLine>)

/** Builds the widget's view of Today from the same planner the app uses. */
suspend fun loadWidgetData(context: Context, date: LocalDate = LocalDate.now()): WidgetData {
    val c = (context.applicationContext as DailySystemsApp).container
    val labels = c.labels.labels.first()
    val systems = c.systems.systems.first()
    val tasks = c.tasks.tasks.first()
    val checked = c.systems.checkedSteps(date).first()
    val plan = DayPlanner.planDay(date, systems, tasks, labels)
    val labelMap = labels.associateBy { it.id }
    val color = { id: String? -> id?.let { labelMap[it]?.colorArgb } }
    val lines = buildList {
        plan.systems.forEach { ps ->
            val s = ps.system
            add(WidgetLine.Header(s.name.ifBlank { "Untitled system" }, if (ps.runningForTarget) "target date" else s.reminderTime?.let(Format::time).orEmpty()))
            ps.rows.forEach { row ->
                when (row) {
                    is PlannedRow.Step -> add(
                        WidgetLine.Check(
                            "s:${s.id}:${row.item.id}", row.item.title.ifBlank { "Untitled step" },
                            StepKey(s.id, row.item.id) in checked, color(row.item.labelId), false,
                            Toggle.Step(s.id, row.item.id),
                        ),
                    )
                    is PlannedRow.Slot -> {
                        row.tasks.forEach { t ->
                            add(WidgetLine.Check("t:${s.id}:${t.id}", t.title.ifBlank { "Untitled to-do" }, t.done, color(t.labelId), true, Toggle.Todo(t.id)))
                        }
                        repeat(row.openPositions) { k -> add(WidgetLine.Open("o:${row.item.id}:$k", "Open slot · ${row.item.title.ifBlank { "Slot" }}")) }
                    }
                }
            }
        }
        if (plan.alsoDue.isNotEmpty()) {
            add(WidgetLine.Header("Also due", ""))
            plan.alsoDue.forEach { t -> add(WidgetLine.Check("d:${t.id}", t.title.ifBlank { "Untitled to-do" }, t.done, color(t.labelId), false, Toggle.Todo(t.id))) }
        }
    }
    val p = plan.progress(checked)
    return WidgetData(date, p.done, p.total, lines)
}

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent { WidgetBody(data) }
    }

    companion object {
        suspend fun refresh(context: Context) = runCatching { TodayWidget().updateAll(context) }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

// Widget colors mirror the app theme (light / dark).
private val Bg = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF161D26))
private val Ink = ColorProvider(day = Color(0xFF141C26), night = Color(0xFFE6EBF1))
private val Muted = ColorProvider(day = Color(0xFF5B6878), night = Color(0xFF97A5B6))
private val Accent = ColorProvider(day = Color(0xFF2B4ACB), night = Color(0xFF93A8FF))
private val Line = ColorProvider(day = Color(0xFFD6DDE6), night = Color(0xFF283341))
private val Socket = Color(0xFFA3AEBB)

private val KeyKind = ActionParameters.Key<String>("kind")
private val KeySystem = ActionParameters.Key<String>("system")
private val KeyItem = ActionParameters.Key<String>("item")
private val KeyTask = ActionParameters.Key<String>("task")

@Composable
private fun WidgetBody(d: WidgetData) {
    val open = actionStartActivity<MainActivity>()
    Column(GlanceModifier.fillMaxSize().background(Bg).cornerRadius(20.dp).padding(14.dp)) {
        Row(GlanceModifier.fillMaxWidth().clickable(open), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text(Format.longDate(d.date).uppercase(), style = TextStyle(color = Muted, fontSize = 11.sp))
                Text("Today", style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold))
            }
            Text("${d.done}/${d.total}", style = TextStyle(color = Muted, fontSize = 13.sp))
        }
        Spacer(GlanceModifier.height(8.dp))
        LinearProgressIndicator(
            progress = if (d.total == 0) 0f else d.done.toFloat() / d.total,
            modifier = GlanceModifier.fillMaxWidth().height(5.dp),
            color = Accent, backgroundColor = Line,
        )
        Spacer(GlanceModifier.height(6.dp))
        if (d.lines.isEmpty()) {
            Box(GlanceModifier.fillMaxSize().clickable(open), contentAlignment = Alignment.Center) {
                Text("Nothing planned today. Tap to open Daily Systems.", style = TextStyle(color = Muted, fontSize = 13.sp))
            }
        } else {
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(d.lines, itemId = { it.hashCode().toLong() }) { line -> LineView(line) }
            }
        }
    }
}

@Composable
private fun LineView(line: WidgetLine) {
    when (line) {
        is WidgetLine.Header -> Row(GlanceModifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(line.title, style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
            if (line.meta.isNotEmpty()) Text(line.meta, style = TextStyle(color = Muted, fontSize = 11.sp))
        }
        is WidgetLine.Open -> Text(
            line.text,
            style = TextStyle(color = Muted, fontSize = 12.sp),
            modifier = GlanceModifier.padding(start = 30.dp, top = 4.dp, bottom = 4.dp).clickable(actionStartActivity<MainActivity>()),
        )
        is WidgetLine.Check -> {
            val params = when (val a = line.action) {
                is Toggle.Step -> actionParametersOf(KeyKind to "step", KeySystem to a.systemId, KeyItem to a.itemId)
                is Toggle.Todo -> actionParametersOf(KeyKind to "task", KeyTask to a.taskId)
            }
            val tint = ColorProvider(day = Color(line.colorArgb ?: Socket.toArgb()), night = Color(line.colorArgb ?: Socket.toArgb()))
            Row(
                GlanceModifier.fillMaxWidth().padding(start = if (line.indent) 14.dp else 0.dp, top = 5.dp, bottom = 5.dp)
                    .clickable(actionRunCallback<ToggleFromWidget>(params)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(GlanceModifier.size(22.dp), contentAlignment = Alignment.Center) {
                    Image(ImageProvider(if (line.done) R.drawable.widget_dot else R.drawable.widget_ring), null, colorFilter = ColorFilter.tint(tint), modifier = GlanceModifier.size(22.dp))
                    if (line.done) Image(ImageProvider(R.drawable.widget_check), null, modifier = GlanceModifier.size(22.dp))
                }
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    line.title, maxLines = 1,
                    style = TextStyle(
                        color = if (line.done) Muted else Ink, fontSize = 14.sp,
                        textDecoration = if (line.done) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )
            }
        }
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

/** Tap on a widget row: check it off (or back on) for today. */
class ToggleFromWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val c = (context.applicationContext as DailySystemsApp).container
        val today = LocalDate.now()
        c.writeAwait {
            when (parameters[KeyKind]) {
                "step" -> c.systems.toggleStep(parameters[KeySystem] ?: return@writeAwait, parameters[KeyItem] ?: return@writeAwait, today)
                "task" -> c.tasks.toggleDone(parameters[KeyTask] ?: return@writeAwait, today)
            }
        }
        TodayWidget().update(context, glanceId)
    }
}
