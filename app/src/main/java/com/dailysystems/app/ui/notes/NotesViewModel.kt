package com.dailysystems.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailysystems.app.AppContainer
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class NoteFilter { ALL, NOTES, TODO_NOTES }

/** A card on the Notes grid: either a standalone note or a to-do's notes. */
data class NoteCard(
    val id: String,
    val isTodo: Boolean,
    val title: String,
    val body: String,
    val at: Long,
    val labelId: String?,
    val isExample: Boolean,
)

data class NotesUiState(
    val loaded: Boolean = false,
    val query: String = "",
    val filter: NoteFilter = NoteFilter.ALL,
    val cards: List<NoteCard> = emptyList(),
    val labels: Map<String, PriorityLabel> = emptyMap(),
)

class NotesViewModel(c: AppContainer) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(NoteFilter.ALL)

    val state: StateFlow<NotesUiState> = combine(c.notes.notes, c.tasks.tasks, c.labels.labels, query, filter) { notes, tasks, labels, q, f ->
        var cards = buildList {
            if (f != NoteFilter.TODO_NOTES) {
                notes.forEach { add(NoteCard(it.id, false, it.title, it.body, it.updatedAt, it.labelId, it.isExample)) }
            }
            if (f != NoteFilter.NOTES) {
                tasks.filter { it.notes.isNotBlank() }.forEach {
                    add(NoteCard(it.id, true, it.title, it.notes, it.notesUpdatedAt ?: it.createdAt, it.labelId, it.isExample))
                }
            }
        }
        val needle = q.trim()
        if (needle.isNotEmpty()) cards = cards.filter { (it.title + " " + it.body).contains(needle, ignoreCase = true) }
        NotesUiState(true, q, f, cards.sortedByDescending { it.at }, labels.associateBy { it.id })
    }.stateIn(viewModelScope, WhileUiSubscribed, NotesUiState())

    fun setQuery(q: String) {
        query.value = q
    }

    fun setFilter(f: NoteFilter) {
        filter.value = f
    }
}
