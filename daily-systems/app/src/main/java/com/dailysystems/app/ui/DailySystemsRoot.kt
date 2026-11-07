package com.dailysystems.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dailysystems.app.AppContainer
import com.dailysystems.app.ui.components.AppIcons
import com.dailysystems.app.ui.components.TextStyles
import com.dailysystems.app.ui.editors.SheetHost
import com.dailysystems.app.ui.notes.NotesRoute
import com.dailysystems.app.ui.systems.SystemsRoute
import com.dailysystems.app.ui.tasks.TasksRoute
import com.dailysystems.app.ui.theme.AppTheme
import com.dailysystems.app.ui.theme.DailySystemsTheme
import com.dailysystems.app.ui.today.TodayRoute

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", AppIcons.Today),
    TASKS("tasks", "To-dos", AppIcons.Tasks),
    SYSTEMS("systems", "Systems", AppIcons.Systems),
    NOTES("notes", "Notes", AppIcons.Notes),
}

@Composable
fun DailySystemsRoot(container: AppContainer) {
    DailySystemsTheme {
        val factory = remember(container) { appViewModelFactory(container) }
        CompositionLocalProvider(LocalViewModelFactory provides factory) {
            val sheets: SheetsViewModel = viewModel(factory = factory)
            val nav = rememberNavController()
            val entry by nav.currentBackStackEntryAsState()
            val current = entry?.destination?.route
            val go: (Tab) -> Unit = { tab ->
                nav.navigate(tab.route) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            val c = AppTheme.colors
            Scaffold(
                containerColor = c.background,
                contentColor = c.ink,
                bottomBar = {
                    Column {
                        HorizontalDivider(thickness = 1.dp, color = c.line)
                        NavigationBar(containerColor = c.surface, tonalElevation = 0.dp) {
                            Tab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = current == tab.route,
                                    onClick = { go(tab) },
                                    icon = { Icon(tab.icon, null, Modifier.size(22.dp)) },
                                    label = { Text(tab.label, style = TextStyles.chip) },
                                    modifier = Modifier.testTag("tab-${tab.route}"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = c.accent,
                                        selectedTextColor = c.ink,
                                        indicatorColor = c.accentSoft,
                                        unselectedIconColor = c.muted,
                                        unselectedTextColor = c.muted,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                NavHost(
                    navController = nav,
                    startDestination = Tab.TODAY.route,
                    modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
                ) {
                    composable(Tab.TODAY.route) { TodayRoute(sheets, openSystemsTab = { go(Tab.SYSTEMS) }) }
                    composable(Tab.TASKS.route) { TasksRoute(sheets) }
                    composable(Tab.SYSTEMS.route) { SystemsRoute(sheets) }
                    composable(Tab.NOTES.route) { NotesRoute(sheets) }
                }
            }
            SheetHost(sheets)
        }
    }
}
