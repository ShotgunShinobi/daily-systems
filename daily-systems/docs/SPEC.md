# Daily Systems — Product & Technical Spec

A personal Android productivity + to-do app. You build reusable **systems** (routines) that run on chosen days. Each system has **steps** (done every time) and **slots** that automatically pull in your open to-dos by priority. Everything — tasks, systems, and every item inside a system — carries an editable, user-defined **priority label**. Every to-do has its own notes, and there is a dedicated Notes screen.

A working web prototype lives at `docs/prototype.html`. Open it in a browser to see the intended behavior (it falls back to browser storage outside claude.ai). When this spec and the prototype disagree, this spec wins.

---

## 1. Core concepts

| Concept | What it is |
|---|---|
| **Priority label** | User-defined label (name, color, rank). Rank 0 = highest priority. Fully editable: rename, recolor, reorder, add, delete. |
| **To-do (Task)** | Title, priority label, optional target date, optional "run this system on the target date", notes, done state. |
| **System** | A reusable routine with a name, its own priority label, a schedule, and an ordered list of items. Can be paused. |
| **Step** | A system item you do every time the system runs (e.g. "Review today's plan"). Checked off per day. |
| **Slot** | A system item that is an open socket. On a given day it fills with the highest-priority open to-dos that match the labels it accepts, up to its count. |
| **Schedule** | Daily · Weekly (pick weekdays) · Monthly (pick days 1–31 and/or "last day") · Specific dates (a list of target dates). |
| **Target-date run** | A to-do can name a system to run on its target date, even if that system's schedule wouldn't otherwise include that day. |
| **Note** | Standalone note with title, body, optional label. The Notes screen also lists to-do notes. |

### Default labels (seeded on first launch, all editable)
| Rank | Name | Color |
|---|---|---|
| 0 | Must do | `#D7263D` |
| 1 | Should do | `#F08A24` |
| 2 | Could do | `#2F7FD8` |
| 3 | Someday | `#7A8794` |

Label color palette for the picker: `#D7263D #F08A24 #E3B505 #3AA35C #1F9E9A #2F7FD8 #5B5BD6 #A64AC9 #D6457F #7A8794`.

---

## 2. Data model (Room)

All ids are UUID strings. Dates are `LocalDate` (stored as ISO `yyyy-MM-dd` text); timestamps are epoch millis.

```
PriorityLabel(id PK, name, colorArgb: Int, rank: Int)

Task(id PK, title, labelId: String? FK→PriorityLabel ON DELETE SET NULL,
     dueDate: LocalDate?, runSystemId: String? FK→SystemEntity ON DELETE SET NULL,
     notes: String = "", notesUpdatedAt: Long?, done: Boolean,
     doneOn: LocalDate?, doneAt: Long?, createdAt: Long, isExample: Boolean = false)

SystemEntity(id PK, name, labelId: String? FK ON DELETE SET NULL, active: Boolean,
             itemOrder: PRIORITY | LISTED, scheduleType: DAILY | WEEKLY | MONTHLY | DATES,
             weekdaysMask: Int,        // bit 0 = Monday … bit 6 = Sunday
             monthDays: String,        // CSV of 1..31, plus 0 meaning "last day of month"
             createdAt: Long, isExample: Boolean = false)

SystemDate(systemId FK CASCADE, date: LocalDate, PK(systemId, date))   // for DATES schedules

SystemItem(id PK, systemId FK CASCADE, kind: STEP | SLOT, title,
           labelId: String? FK ON DELETE SET NULL, position: Int, slotCount: Int = 1)

SlotAcceptedLabel(itemId FK CASCADE, labelId FK CASCADE, PK(itemId, labelId))
           // empty set = slot accepts any label

StepCompletion(systemId FK CASCADE, itemId FK CASCADE, date: LocalDate,
               PK(systemId, itemId, date))

Note(id PK, title, body, labelId: String? FK ON DELETE SET NULL,
     createdAt: Long, updatedAt: Long, isExample: Boolean = false)
```

Deleting a label: items using it become unlabelled (rank treated as +∞), then remaining labels are re-ranked 0..n-1.
Deleting a system: tasks pointing at it via `runSystemId` are cleared.

---

## 3. Planning algorithm (the heart of the app)

Implement as a **pure Kotlin function** in the domain layer, no Android dependencies, fully unit-tested.

```
rank(labelId) = label.rank, or +∞ if null/unknown

scheduledOn(system, D):
  DAILY   → true
  WEEKLY  → weekday(D) ∈ system.weekdays
  MONTHLY → dayOfMonth(D) ∈ monthDays  OR  (0 ∈ monthDays AND D is last day of its month)
  DATES   → D ∈ system.dates

targetRun(system, D) = ∃ task: task.runSystemId == system.id AND task.dueDate == D
                       AND (!task.done OR task.doneOn == D)

activeSystems(D) = systems where active AND (scheduledOn OR targetRun),
                   sorted by rank(system.labelId) ASC, then createdAt ASC

orderedItems(system) =
  LISTED   → items by position
  PRIORITY → items by rank(item.labelId) ASC, ties by position

visibleOn(task, D) =
  task.done ? task.doneOn == D
            : task.dueDate == null OR task.dueDate <= D

taskOrder = rank(labelId) ASC, then dated before undated, then dueDate ASC,
            then createdAt ASC

planDay(D):
  candidates = tasks.filter(visibleOn(_, D)).sortedWith(taskOrder)
  used = {}
  for system in activeSystems(D):
    for item in orderedItems(system):
      if item is SLOT:
        take up to max(1, slotCount) candidates not in `used`
          whose labelId ∈ acceptedLabels (or any label if acceptedLabels is empty)
        add them to `used`
  rest      = candidates not in `used`
  alsoDue   = rest with a dueDate
  nextUp    = rest without a dueDate, not done (show top 5)
  return DayPlan(systems with filled slots, alsoDue, nextUp)
```

Key properties to preserve (write tests for each):
1. A task finished today stays in the slot it filled, shown checked — slots don't reshuffle when you check things off.
2. Tomorrow, done tasks drop out and the next-highest-priority task fills the slot.
3. Higher-priority systems claim matching tasks before lower-priority ones; a task never appears in two slots.
4. Overdue open tasks are still candidates (`dueDate <= D`).
5. Future-dated tasks don't appear before their date.
6. A system on a DATES schedule with no dates still runs on a target date set by a task.
7. "Last day" monthly works for 28/29/30/31-day months.
8. Paused systems never run, including target-date runs.

**Progress for day D** = (checked steps + done slot tasks + done "also due" tasks) / (all steps + all slot tasks + all "also due" tasks) for the active systems that day.

---

## 4. Screens

Bottom navigation with four tabs. A tag icon in every top bar opens the Labels manager. Edits save immediately (no Save buttons); editors close with "Done".

### 4.1 Today
- Header: relative day name ("Today", "Tomorrow", weekday) + full date; "Today" button when viewing another day.
- Week strip (Mon–Sun) with prev/next week arrows; a dot under days that have at least one active system.
- Progress bar with `done/total`.
- **Systems running** — one card per active system: name, schedule summary (or "Running for a target date"), label chip, edit button.
  - Step rows: round check (ring tinted by the item's label color), title, label chip.
  - Slot blocks: slot name, `filled/count`, "takes Must do, Should do" (or "any priority"), filled task rows, and a dashed **Open slot** placeholder for each unfilled position. Tapping an open slot creates a to-do dated D with the slot's first accepted label and opens the editor.
- **Also due** — dated visible tasks not placed in a slot.
- **Next up, no date** — top 5 undated open tasks not placed in a slot.
- FAB: new to-do with target date = selected day.
- Empty state (no systems and no tasks): explain systems; buttons **New system** and **Load starter examples**.

### 4.2 To-dos
- Quick add: title field, tap-to-cycle label chip, optional date, Add. Submitting keeps focus in the field.
- Filter: Open / Done / All.
- Grouped by label in rank order, then "No label". Row: check, title, sub-line (date in red with "overdue" when late, system name if it runs one, note indicator), label chip.
- Done filter sorts by `doneAt` descending.

### 4.3 Systems
- List sorted by system priority. Card: name, schedule summary, "N steps · M slots", a row of pips (filled = step, ring = slot) colored by item label, label chip, paused systems dimmed.
- **New** button; empty state with starter examples.

### 4.4 Notes
- Search (title + body), filter: All / Notes / To-do notes, **New** note.
- Grid of cards (title, 4-line snippet, date, "to-do" marker, label). To-do note cards open the to-do editor.
- Empty notes (no title, no body) are deleted when the editor closes.

### 4.5 Editors (bottom sheets)
- **To-do editor:** title, priority (label chips + "No label"), target date, "Run a system on that date" (dropdown), notes (multi-line), Mark done / Mark not done, Delete (two-tap confirm). A brand-new to-do closed with an empty title and empty notes is discarded.
- **System editor:** name, system priority, schedule type (segmented: Daily / Weekly / Monthly / Specific dates) with the matching picker (weekday toggles Mon→Sun; 1–31 grid + "Last"; date chips + add date), list of to-dos targeting this system, item order (By priority / As listed), items list (title, move up/down, remove; Step/Slot toggle; item label; for slots: accepted labels multi-select + count stepper 1–9), add Step / add Slot, Pause/Resume, Delete (two-tap). A new system closed with no name and no items is discarded.
- **Note editor:** title, priority (optional), body, last-edited time, Delete (two-tap).
- **Labels manager:** ordered list (position = priority), color swatch opens palette, inline rename, move up/down, delete (two-tap, warns items become unlabelled), Add label.
- **Label picker** (tapping any label chip on a list row): list of labels + "No label" + "Edit labels" link.

### 4.6 Starter examples
One action that creates (all flagged `isExample`, shown with a small "example" tag):
- *Morning launch* — daily, label rank 0, order by priority: step "Water and a 10-minute walk" (rank 1), step "Review today's plan" (rank 0), slot "Top priorities" (rank 0, count 2, accepts ranks 0–1).
- *Evening wind-down* — weekly Mon–Fri, rank 1, order as listed: slot "Small stuff" (rank 2, count 2, accepts ranks 2–3), step "Write tomorrow's top 3", step "Phone away by 11".
- *Big-day prep* — specific dates (none), rank 0: three steps.
- To-dos: "Pay electricity bill" (rank 0, due today), "Reply to pending emails" (rank 1), "Book dentist appointment" (rank 2), "Sort photos from last trip" (rank 3), "Important appointment" (rank 0, due today+4, runs Big-day prep, has a note).
- Note: "How systems work".

---

## 5. Visual design

Follow the prototype's look; Material 3 as the base, customised:
- Cool grey-blue neutrals. Light: background `#EDF0F4`, surface `#FFFFFF`, ink `#141C26`, muted `#5B6878`, divider `#D6DDE6`, accent `#2B4ACB`. Dark: background `#0E131A`, surface `#161D26`, ink `#E6EBF1`, muted `#97A5B6`, divider `#283341`, accent `#93A8FF`. Follow the system theme; no dynamic color.
- Fonts (Google Fonts, bundled): Bricolage Grotesque for headings, Instrument Sans for body, IBM Plex Mono for dates and counts.
- Label chips: dot + name on a 14% tint of the label color with a 38% border.
- Slots are drawn as sockets: a "jack" icon, a vertical rule, filled task rows on a light inset, dashed outlines for open positions.
- Edge-to-edge with proper insets; tap targets ≥ 48dp.

---

## 6. Technical decisions

- **Kotlin + Jetpack Compose + Material 3**, single activity, Navigation Compose.
- **Room** for storage, exposed as `Flow`s; **MVVM** with one ViewModel per screen; manual DI via an `AppContainer` (no Hilt in v1).
- `kotlinx-datetime` or `java.time` (minSdk 26) for dates.
- minSdk 26, target/compile the current stable SDK. Use current stable AGP, Kotlin, Compose BOM — don't pin outdated versions.
- Package: `com.dailysystems.app` (rename freely).
- Layers: `data` (Room entities, DAOs, repositories) → `domain` (models, `DayPlanner`, use cases) → `ui` (screens, components, theme).
- Day rollover: Today screen observes the current date and refreshes after midnight. Nothing polls the clock: the date is re-read by a non-wakeup alarm just after midnight (delivered when the phone next wakes), when the app comes to the foreground, and on clock/time-zone changes.
- v1 is offline-only. No accounts, no sync.

### Out of scope for v1 (candidates for v2)
Reminders/notifications for systems and due dates · home-screen widget for Today · cloud sync/backup · recurring to-dos · drag-to-reorder · import/export JSON (would allow migrating from the prototype).

---

## 8. v2 (built)

DB version 2 (Room auto-migration from v1; all new columns nullable or defaulted).

**Reminders.** `SystemEntity.reminderTime` — notify at that time on every day the system is active (scheduled or target-date run; never while paused); the notification lists the day's steps and slot to-dos. `Task.reminderTime` — notify on `dueDate` at that time while not done; the notification has a **Mark done** action. `ReminderPlanner` (pure) lists reminders in a time window; `ReminderScheduler` keeps one AlarmManager alarm for the next reminder (exact when allowed, otherwise a 5-minute window), posts everything due since the last run when it fires (catching up at most 2 hours), and is re-armed on data changes, boot, app update and clock/time-zone changes. Notification permission is requested when a reminder time is first set.

**Recurring to-dos.** `Task.repeat = RepeatRule(unit DAY|WEEK|MONTH|YEAR, interval ≥ 1, weekdays (WEEK), anchor)`; occurrences are counted from the anchor, so monthly/yearly never drift (31st → 28th/29th/30th in short months, back to the 31st after). A repeat needs a date (setting one dates an undated to-do; clearing the date clears the repeat; moving the date re-anchors). Checking off an occurrence on day D keeps it done in place (§3 property 1) and creates the next open occurrence at the first rule date after max(dueDate, D), carrying title, label, notes, reminder, system and rule (`seriesId` links them). Un-checking it again, while that next occurrence is still the open one, folds it back.

**Home-screen widget** (Glance). Today's plan from `DayPlanner`: date, progress, each running system with its steps and slot to-dos, open slots, and "Also due". Tapping a row checks it off for today; tapping the header opens the app. Refreshes live on every data change (it collects the database while Android keeps its session open) and at midnight via the non-wakeup rollover alarm; no timed refresh, so it never wakes the phone by itself.

**Drag-to-reorder.** System items (editor) and labels (manager) reorder by dragging the grip; the move is saved on drop. The grip offers "Move up"/"Move down" accessibility actions in place of the old arrow buttons.

---

## 7. Build phases & acceptance

1. **Scaffold** — Gradle project, theme, fonts, bottom nav with four empty screens, Room DB with all entities and DAOs, default-label seeding. *Done when:* app builds, launches, tabs switch, labels exist on first run.
2. **Planner** — `DayPlanner` pure function + unit tests covering every property in §3. *Done when:* `./gradlew testDebugUnitTest` passes.
3. **To-dos + labels** — quick add, list, filters, to-do editor, label picker, labels manager.
4. **Systems** — list, full system editor including all four schedule types and slot configuration.
5. **Today** — week strip, progress, system cards with steps and slots, also due, next up, FAB, open-slot tap.
6. **Notes** — notes screen, search, filters, note editor, to-do notes integration.
7. **Polish** — starter examples, empty states, dark theme check, edge-to-edge insets, a few Compose UI tests for the main flows.

---

## 9. Habit tracking (inspired by *Atomic Habits*)

Layers on top of systems; `DayPlanner` and planning are unchanged. `HabitStats` (pure) computes everything from `StepCompletion` rows; no schema change.

- **Run days** = days the system's *current* schedule runs, plus its target-date runs, from its creation date (at most 730 days back) to today. Earlier schedules and pause periods are not recorded, so they can't be reconstructed.
- **Day result** (systems with at least one step; slot-only systems have no history because slot contents aren't stored): COMPLETE = every step checked; PARTIAL = at least one ("showing up counts"); MISSED = a past run day with none; PENDING = today with none (never a miss until the day is over).
- **Streak** = consecutive kept (complete or partial) run days, counted in runs rather than calendar days; also best streak, 30-day kept rate, and per-step rates counted from each step's first check-off.
- **Systems tab**: a "This week" card (runs kept and rate) that opens the weekly review; each system card shows its last 14 runs and current streak. **Today** cards show the streak ("5 in a row") from 2 runs up.
- **System editor → History**: current streak, best streak, last-30-days rate, a 15-week calendar grid, and per-step rates.
- **Weekly review** (sheet; previous/next week): kept rate vs the previous week, steps checked, to-dos done, each system's runs kept, complete days, current and best streak, the 3 most-missed steps, and the nudge settings.
- **Never miss twice**: when a system runs today, isn't paused, has nothing checked yet, and its last run was missed, Today shows a gentle banner ("Missed last time…", or "Missed the last N runs…") until any step is checked. Optional: the same line in the system's existing start reminder (no extra notifications). Both are switches in the weekly review; in-app on and reminders off by default.
