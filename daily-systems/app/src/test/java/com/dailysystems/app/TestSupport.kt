package com.dailysystems.app

import androidx.test.core.app.ApplicationProvider
import com.dailysystems.app.data.AppDatabase
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf

/** A Tuesday; tests pin "today" so schedules are deterministic. */
val TEST_TODAY: LocalDate = LocalDate.of(2026, 11, 3)

fun testContainer(today: LocalDate = TEST_TODAY): AppContainer =
    AppContainer(AppDatabase.inMemory(ApplicationProvider.getApplicationContext()), flowOf(today))
