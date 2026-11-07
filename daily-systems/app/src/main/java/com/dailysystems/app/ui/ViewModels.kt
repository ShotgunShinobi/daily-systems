package com.dailysystems.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dailysystems.app.AppContainer
import com.dailysystems.app.ui.notes.NotesViewModel
import com.dailysystems.app.ui.systems.SystemsViewModel
import com.dailysystems.app.ui.tasks.TasksViewModel
import com.dailysystems.app.ui.today.TodayViewModel
import kotlinx.coroutines.flow.SharingStarted

/** Builds every ViewModel from the [AppContainer] (manual DI). */
fun appViewModelFactory(c: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { SheetsViewModel(c) }
    initializer { TodayViewModel(c) }
    initializer { TasksViewModel(c) }
    initializer { SystemsViewModel(c) }
    initializer { NotesViewModel(c) }
}

val LocalViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> { error("No ViewModel factory") }

internal val WhileUiSubscribed = SharingStarted.WhileSubscribed(5_000)
