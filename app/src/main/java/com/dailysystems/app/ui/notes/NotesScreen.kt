package com.dailysystems.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailysystems.app.ui.Format
import com.dailysystems.app.ui.LocalViewModelFactory
import com.dailysystems.app.ui.Sheet
import com.dailysystems.app.ui.SheetsViewModel
import com.dailysystems.app.ui.components.AppButton
import com.dailysystems.app.ui.components.AppCard
import com.dailysystems.app.ui.components.AppIcons
import com.dailysystems.app.ui.components.AppTextField
import com.dailysystems.app.ui.components.ButtonKind
import com.dailysystems.app.ui.components.EmptyCard
import com.dailysystems.app.ui.components.ExampleTag
import com.dailysystems.app.ui.components.ScreenHeader
import com.dailysystems.app.ui.components.Segmented
import com.dailysystems.app.ui.components.SubBit
import com.dailysystems.app.ui.components.TextStyles
import com.dailysystems.app.ui.theme.AppTheme
import com.dailysystems.app.ui.theme.DailySystemsTheme
import com.dailysystems.app.ui.today.TodayPreviewData

data class NotesActions(
    val setQuery: (String) -> Unit = {},
    val setFilter: (NoteFilter) -> Unit = {},
    val open: (NoteCard) -> Unit = {},
    val newNote: () -> Unit = {},
    val openLabels: () -> Unit = {},
)

@Composable
fun NotesRoute(sheets: SheetsViewModel) {
    val vm: NotesViewModel = viewModel(factory = LocalViewModelFactory.current)
    val state by vm.state.collectAsStateWithLifecycle()
    NotesScreen(
        state,
        NotesActions(
            setQuery = vm::setQuery,
            setFilter = vm::setFilter,
            // To-do note cards open the to-do editor.
            open = { sheets.open(if (it.isTodo) Sheet.TaskEditor(it.id) else Sheet.NoteEditor(it.id)) },
            newNote = sheets::newNote,
            openLabels = { sheets.open(Sheet.Labels) },
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(state: NotesUiState, actions: NotesActions) {
    val c = AppTheme.colors
    val full = StaggeredGridItemSpan.FullLine
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp),
        modifier = Modifier.fillMaxSize().testTag("notes-grid"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
    ) {
        item(key = "header", span = full) {
            ScreenHeader("Notes", actions.openLabels) {
                AppButton("New", actions.newNote, kind = ButtonKind.Primary, icon = AppIcons.Plus, small = true)
            }
        }
        item(key = "search", span = full) {
            AppTextField(
                value = state.query,
                onValueChange = actions.setQuery,
                placeholder = "Search notes",
                background = c.surface,
                modifier = Modifier.testTag("notes-search"),
            )
        }
        item(key = "filter", span = full) {
            Segmented(
                listOf(NoteFilter.ALL to "All", NoteFilter.NOTES to "Notes", NoteFilter.TODO_NOTES to "To-do notes"),
                state.filter, actions.setFilter, Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }
        if (state.loaded && state.cards.isEmpty()) {
            item(key = "empty", span = full) {
                EmptyCard(
                    null,
                    if (state.query.isNotBlank()) "No notes match that search."
                    else "Write freely here, or open any to-do and use its notes section. To-do notes show up on this page too.",
                )
            }
        }
        items(state.cards, key = { (if (it.isTodo) "t-" else "n-") + it.id }) { n ->
            AppCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { actions.open(n) }
                        .padding(14.dp),
                ) {
                    Text(n.title.ifBlank { "Untitled" }, style = AppTheme.type.cardTitle.copy(fontSize = AppTheme.type.cardTitle.fontSize * 0.92f), color = c.ink)
                    if (n.body.isNotBlank()) {
                        Text(
                            n.body, style = TextStyles.body, color = c.muted, maxLines = 4, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    FlowRow(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (n.at > 0) Text(Format.dayMonth(n.at), style = AppTheme.type.monoSmall, color = c.muted)
                        if (n.isTodo) SubBit(AppIcons.Check, "to-do")
                        state.labels[n.labelId]?.let { l ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(l.colorArgb)))
                                Spacer(Modifier.width(4.dp))
                                Text(l.name, style = TextStyles.rowSub, color = Color(l.colorArgb))
                            }
                        }
                        if (n.isExample) ExampleTag()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDF0F4)
@Composable
private fun NotesPreview() = DailySystemsTheme(darkTheme = false) {
    val d = TodayPreviewData
    val state = NotesUiState(
        loaded = true, labels = d.labels.associateBy { it.id },
        cards = listOf(
            NoteCard("1", false, "How systems work", "Steps are things you always do.\nSlots are open sockets.", 1_790_000_000_000, null, true),
            NoteCard("2", true, "Reply to pending emails", "Mostly the invoices", 1_790_000_000_000, "should", false),
        ),
    )
    Box(Modifier.background(Color(0xFFEDF0F4))) { NotesScreen(state, NotesActions()) }
}
