# Daily Systems (Android)

Native Kotlin + Jetpack Compose build of the Daily Systems spec (`docs/SPEC.md`). Work rules for agents are in `CLAUDE.md`.

## Build & run
Requires JDK 21 (Robolectric's Android 16 sandbox, used by the tests, needs it). Android Studio's bundled JDK works.

```
./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/
./gradlew installDebug           # onto a connected device/emulator
./gradlew testDebugUnitTest      # planner, repository and Compose UI tests (Robolectric, no device needed)
./gradlew lint
```
Optional screenshots: `./gradlew testDebugUnitTest --tests '*ScreenshotTest*' -PscreenshotDir=/tmp/shots`

Toolchain: Glance 1.2, Reorderable 3.1, AGP 9.5, Gradle 9.8.2, Kotlin 2.4.21, KSP 2.3.13, Compose BOM 2026.09.00, Room 2.8.5, compileSdk/targetSdk 37, minSdk 26.

## v2 features
Reminders (systems and to-dos), a Today home-screen widget, recurring to-dos and drag-to-reorder. See `docs/SPEC.md` §8.

Habit tracking: streaks, run history, a weekly review and a "never miss twice" nudge. See `docs/SPEC.md` §9.

## Layout
- `domain/` — models + `DayPlanner` (pure Kotlin, SPEC §3)
- `data/` — Room entities/DAOs/database, repositories, starter examples
- `ui/` — theme, components, today/, tasks/, systems/, notes/, labels/, editors/ (bottom sheets)
- `reminders/` — notification channel, alarm scheduler, receivers; `widget/` — Glance widget
- `BackgroundSync` — re-arms the reminder alarm and refreshes the widget whenever data changes
- `AppContainer` — manual DI; all writes go through one ordered queue so per-keystroke saves never land out of order.
