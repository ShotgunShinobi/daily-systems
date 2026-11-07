package com.dailysystems.app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dailysystems.app.TEST_TODAY
import com.dailysystems.app.domain.ItemKind
import com.dailysystems.app.testContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Compose UI tests for the main flows, run on the JVM with Robolectric. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xhdpi")
class AppFlowsTest {
    @get:Rule val rule = createComposeRule()
    private val c = testContainer()

    @Before
    fun setUp() {
        runBlocking { c.labels.seedDefaultsIfEmpty() }
    }

    private fun launch() = rule.setContent { DailySystemsRoot(c) }

    private fun waitForText(text: String, substring: Boolean = false) =
        rule.waitUntilAtLeastOneExists(hasText(text, substring = substring, ignoreCase = true), 5_000)

    @Test
    fun emptyTodayLoadsStarterExamples() {
        launch()
        waitForText("Build your first system")
        rule.onNodeWithText("Load starter examples").performClick()
        waitForText("Morning launch")
        waitForText("Pay electricity bill")
        rule.onNodeWithText("Evening wind-down").assertExists()
        rule.onNodeWithText("0/8 done").assertExists()
    }

    @Test
    fun checkingASlotTaskKeepsItInPlaceAndUpdatesProgress() {
        runBlocking { c.starterExamples.load(TEST_TODAY) }
        launch()
        waitForText("Pay electricity bill")
        rule.onAllNodesWithContentDescription("Mark done")[0].performClick()
        waitForText("1/8 done")
        rule.onNodeWithText("Pay electricity bill").assertExists()
        rule.onNodeWithText("Top priorities").assertExists()
        rule.onAllNodesWithText("2/2").assertCountEquals(2) // both starter slots still full
        assertTrue(runBlocking { c.tasks.tasks.first() }.single { it.title == "Pay electricity bill" }.done)
    }

    @Test
    fun quickAddKeepsFocusAndGroupsByLabel() {
        launch()
        rule.onNodeWithTag("tab-tasks").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("quick-add"))
        rule.onNodeWithTag("quick-add").performTextInput("Buy milk")
        rule.onNodeWithTag("quick-add").performImeAction()
        waitForText("Buy milk")
        // Default quick-add label is the second label: chip in quick add + group header + row chip.
        assertEquals(3, rule.onAllNodesWithText("Should do").fetchSemanticsNodes().size)
        rule.onNodeWithTag("quick-add").performTextInput("Water plants")
        rule.onNodeWithTag("quick-add").performImeAction()
        waitForText("Water plants")
        assertEquals(setOf("Buy milk", "Water plants"), runBlocking { c.tasks.tasks.first() }.map { it.title }.toSet())
    }

    @Test
    fun tappingAnOpenSlotCreatesADatedToDo() {
        runBlocking {
            val s = c.systems.createBlank("label-must")
            c.systems.setName(s.id, "Focus")
            val slot = c.systems.addItem(s.id, ItemKind.SLOT, "label-must")
            c.systems.toggleAccepted(slot, "label-could")
            c.systems.setItemTitle(slot, "Deep work")
        }
        launch()
        waitForText("Open slot", substring = true)
        rule.onNodeWithText("Open slot · add a to-do (Could do)").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("task-title"))
        rule.onNodeWithTag("task-title").performTextInput("Draft report")
        rule.onNodeWithTag("sheet-done").performClick()
        rule.waitUntil(5_000) { runBlocking { c.tasks.tasks.first() }.any { it.title == "Draft report" } }
        val t = runBlocking { c.tasks.tasks.first() }.single()
        assertEquals(TEST_TODAY, t.dueDate)
        assertEquals("label-could", t.labelId)
        waitForText("Draft report")
        rule.onNodeWithText("1/1").assertExists()
    }

    @Test
    fun blankNewToDoIsDiscardedOnDone() {
        launch()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Add a to-do"))
        rule.onAllNodesWithContentDescription("Add a to-do")[0].performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("task-title"))
        rule.onNodeWithTag("sheet-done").performClick()
        rule.waitUntil(5_000) { runBlocking { c.tasks.tasks.first() }.isEmpty() && rule.onAllNodes(hasTestTag("task-title")).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun deleteNeedsTwoTaps() {
        runBlocking { c.tasks.create("Old thing", labelId = "label-could") }
        launch()
        rule.onNodeWithTag("tab-tasks").performClick()
        waitForText("Old thing")
        rule.onNodeWithText("Old thing").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Delete"))
        rule.onNodeWithText("Delete").performClick()
        waitForText("Tap again to delete")
        assertEquals(1, runBlocking { c.tasks.tasks.first() }.size)
        rule.onNodeWithText("Tap again to delete").performClick()
        rule.waitUntil(5_000) { runBlocking { c.tasks.tasks.first() }.isEmpty() }
    }

    @Test
    fun labelChipOpensPickerAndChangesPriority() {
        runBlocking { c.tasks.create("Pick me", labelId = "label-could") }
        launch()
        rule.onNodeWithTag("tab-tasks").performClick()
        waitForText("Pick me")
        rule.onAllNodesWithContentDescription("Priority: Could do. Tap to change")[0].performClick()
        waitForText("To-do priority")
        rule.onNodeWithText("Must do").performClick()
        rule.waitUntil(5_000) { runBlocking { c.tasks.tasks.first() }.single().labelId == "label-must" }
    }

    @Test
    fun systemEditorBuildsAWeeklySystem() {
        launch()
        rule.onNodeWithTag("tab-systems").performClick()
        waitForText("No systems yet")
        rule.onNodeWithText("New system").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("system-name"))
        rule.onNodeWithTag("system-name").performTextInput("Gym")
        rule.onNodeWithText("Weekly").performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Mon"))
        rule.onNodeWithContentDescriptionCompat("Mon").performClick()
        rule.onNodeWithText("Step").performClick()
        rule.waitUntil(5_000) {
            runBlocking { c.systems.systems.first() }.singleOrNull()?.let {
                it.name == "Gym" && it.items.size == 1 && it.weekdays.size == 1
            } == true
        }
        rule.onNodeWithTag("sheet-done").performClick()
        waitForText("Weekly · Mon", substring = true)
        rule.onNodeWithText("1 step · 0 slots").assertExists()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithContentDescriptionCompat(d: String) =
        onAllNodesWithContentDescription(d)[0]

    @Test
    fun labelsCanBeReorderedFromTheManager() {
        launch()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Edit priority labels"))
        rule.onAllNodesWithContentDescription("Edit priority labels")[0].performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Drag to reorder Someday"))
        // Drag "Someday" (last) up past the other three.
        rule.onNode(hasContentDescription("Drag to reorder Someday")).performTouchInput {
            down(center)
            repeat(30) { moveBy(androidx.compose.ui.geometry.Offset(0f, -12f)) }
            up()
        }
        rule.waitUntil(5_000) { runBlocking { c.labels.labels.first() }.first().name == "Someday" }
        // Screen-reader path: the "Move down" action on the grip.
        val actions = rule.onNode(hasContentDescription("Drag to reorder Someday")).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
        rule.runOnIdle { actions.first { it.label == "Move down" }.action() }
        rule.waitUntil(5_000) { runBlocking { c.labels.labels.first() }.map { it.name }.indexOf("Someday") == 1 }
    }

    @Test
    fun recurringToDoFromTheEditor() {
        runBlocking { c.tasks.create("Stretch", labelId = "label-must", dueDate = TEST_TODAY) }
        launch()
        rule.onNodeWithTag("tab-tasks").performClick()
        waitForText("Stretch")
        rule.onNodeWithText("Stretch").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Daily"))
        rule.onNodeWithText("Daily").performClick()
        rule.waitUntil(5_000) { runBlocking { c.tasks.tasks.first() }.single().repeat != null }
        rule.onNodeWithTag("sheet-done").performClick()
        waitForText("every day")
        rule.onAllNodesWithContentDescription("Mark done")[0].performClick()
        rule.waitUntil(5_000) { runBlocking { c.tasks.tasks.first() }.size == 2 }
        val open = runBlocking { c.tasks.tasks.first() }.single { !it.done }
        assertEquals(TEST_TODAY.plusDays(1), open.dueDate)
    }

    /** A daily "Habit" system created 5 days ago, with its step checked 2 days ago but not yesterday. */
    private fun systemMissedYesterday(): String = runBlocking {
        val zone = java.time.ZoneId.systemDefault()
        val s = com.dailysystems.app.domain.DailySystem(
            id = "habit", name = "Habit", labelId = "label-must",
            createdAt = TEST_TODAY.minusDays(5).atStartOfDay(zone).toInstant().toEpochMilli(),
            items = listOf(com.dailysystems.app.domain.SystemItem("walk", ItemKind.STEP, "Walk")),
        )
        c.systems.create(s)
        c.systems.toggleStep("habit", "walk", TEST_TODAY.minusDays(2))
        s.id
    }

    @Test
    fun neverMissTwiceNudgeShowsUntilAStepIsChecked() {
        systemMissedYesterday()
        launch()
        rule.waitUntilAtLeastOneExists(hasTestTag("nudge"), 5_000)
        waitForText("Missed last time", substring = true)
        rule.onNodeWithContentDescriptionCompat("Mark step done").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("nudge")).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun reviewOpensFromSystemsAndCanTurnNudgesOff() {
        systemMissedYesterday()
        launch()
        rule.waitUntilAtLeastOneExists(hasTestTag("nudge"), 5_000)
        rule.onNodeWithTag("tab-systems").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("week-card"), 5_000)
        // Mon 2 Nov kept? No: only Sun 1 Nov (2 days before Tue 3 Nov) was checked; Mon was missed.
        waitForText("0 of 1 runs kept", substring = true)
        rule.onNodeWithTag("week-card").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("review-title"), 5_000)
        rule.onNodeWithText("This week").assertExists()
        waitForText("Walk")
        rule.onNodeWithTag("switch-nudge-app").performClick()
        rule.waitUntil(5_000) { !c.settings.settings.value.nudgesInApp }
        rule.onNodeWithTag("sheet-done").performClick()
        rule.onNodeWithTag("tab-today").performClick()
        waitForText("Habit")
        assertEquals(0, rule.onAllNodes(hasTestTag("nudge")).fetchSemanticsNodes().size)
    }
}
