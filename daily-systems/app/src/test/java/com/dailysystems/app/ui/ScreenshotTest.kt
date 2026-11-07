package com.dailysystems.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dailysystems.app.TEST_TODAY
import com.dailysystems.app.testContainer
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the main screens to PNGs for a visual check without an emulator.
 * Opt-in: ./gradlew testDebugUnitTest --tests '*ScreenshotTest*' -PscreenshotDir=/some/dir
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private val dir = System.getProperty("screenshotDir").orEmpty()

    private fun shoot(name: String) {
        rule.waitForIdle()
        val roots = rule.onAllNodes(isRoot()).fetchSemanticsNodes()
        File(dir).mkdirs()
        // Only the main window: Robolectric doesn't draw bottom-sheet windows.
        val img = rule.onAllNodes(isRoot())[0].captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
        check(roots.isNotEmpty())
    }

    private fun run(dark: Boolean) {
        assumeTrue("set -PscreenshotDir to render screenshots", dir.isNotEmpty())
        val suffix = if (dark) "-dark" else ""
        val c = testContainer()
        runBlocking {
            c.starterExamples.load(TEST_TODAY)
            c.tasks.create("Renew passport", labelId = "label-could", dueDate = TEST_TODAY.minusDays(2))
            c.tasks.create("Plan weekend trip", labelId = "label-someday")
            val water = c.tasks.create("Water plants", labelId = "label-should", dueDate = TEST_TODAY)
            c.tasks.setRepeat(water.id, com.dailysystems.app.domain.RepeatRule(com.dailysystems.app.domain.RepeatUnit.WEEK, 1, setOf(java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.FRIDAY), TEST_TODAY))
            c.tasks.setReminderTime(water.id, java.time.LocalTime.of(18, 30))
            // A system with a few weeks of history; yesterday was missed, so Today shows the nudge.
            val zone = java.time.ZoneId.systemDefault()
            c.systems.create(
                com.dailysystems.app.domain.DailySystem(
                    id = "workout", name = "Workout", labelId = "label-should",
                    createdAt = TEST_TODAY.minusDays(40).atStartOfDay(zone).toInstant().toEpochMilli(),
                    scheduleType = com.dailysystems.app.domain.ScheduleType.WEEKLY,
                    weekdays = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.SATURDAY),
                    items = listOf(
                        com.dailysystems.app.domain.SystemItem("w1", com.dailysystems.app.domain.ItemKind.STEP, "Warm up", "label-should", 0),
                        com.dailysystems.app.domain.SystemItem("w2", com.dailysystems.app.domain.ItemKind.STEP, "20 minutes strength", "label-should", 1),
                    ),
                ),
            )
            (2L..40L).forEach { ago ->
                val d = TEST_TODAY.minusDays(ago)
                if (ago % 9L != 4L) c.systems.toggleStep("workout", "w1", d)
                if (ago % 3L != 0L) c.systems.toggleStep("workout", "w2", d)
            }
            val morning = c.systems.systems.first().first { it.name == "Morning launch" }
            c.systems.setReminderTime(morning.id, java.time.LocalTime.of(7, 30))
        }
        rule.setContent { DailySystemsRoot(c) }
        rule.waitUntilAtLeastOneExists(hasText("Pay electricity bill"), 5_000)
        shoot("01-today$suffix")

        rule.onNodeWithTag("today-list").performScrollToIndex(6)
        shoot("02-today-scrolled$suffix")

        rule.onNodeWithTag("tab-tasks").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Reply to pending emails"), 5_000)
        shoot("03-todos$suffix")

        rule.onNodeWithTag("tab-systems").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Big-day prep"), 5_000)
        shoot("04-systems$suffix")

        rule.onNodeWithText("Morning launch").performClick()
        rule.waitUntilAtLeastOneExists(hasText("System priority", ignoreCase = true), 5_000)
        rule.mainClock.advanceTimeBy(1_000)
        shoot("05-system-editor$suffix")
        rule.onNodeWithTag("sheet-done").performClick()
        rule.mainClock.advanceTimeBy(1_000)

        rule.onNodeWithTag("tab-notes").performClick()
        rule.waitUntilAtLeastOneExists(hasText("How systems work"), 5_000)
        shoot("06-notes$suffix")

        rule.onNodeWithTag("tab-tasks").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Important appointment"), 5_000)
        rule.onNodeWithText("Water plants").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Run a system on that date", ignoreCase = true), 5_000)
        rule.mainClock.advanceTimeBy(1_000)
        shoot("07-todo-editor$suffix")
        rule.onNodeWithTag("sheet-done").performClick()
        rule.mainClock.advanceTimeBy(1_000)

        rule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Edit priority labels"))[0].performClick()
        rule.waitUntilAtLeastOneExists(hasText("Priority labels", substring = true, ignoreCase = true), 5_000)
        rule.mainClock.advanceTimeBy(1_000)
        shoot("08-labels$suffix")
    }

    @Test
    fun light() = run(dark = false)

    @Test
    @Config(qualifiers = "+night")
    fun dark() = run(dark = true)
}
