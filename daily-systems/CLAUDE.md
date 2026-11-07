# Daily Systems (Android)

Native Android rebuild of a working web prototype: a to-do app where reusable **systems** (routines) run on chosen days, and their **slots** auto-fill with to-dos by user-defined priority labels.

- Full spec: `docs/SPEC.md` — read it before starting any phase. It is the source of truth.
- Behavior reference: `docs/prototype.html` — open in a browser to see how screens and the planner should behave.

## Stack
Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room (Flows) · MVVM · manual DI (`AppContainer`) · java.time · minSdk 26. Use current stable versions of AGP, Kotlin, and the Compose BOM.

## Layout
```
app/src/main/java/com/dailysystems/app/
  data/     Room entities, DAOs, database, repositories
  domain/   models, DayPlanner (pure Kotlin, no Android imports), use cases
  ui/       theme/, components/, today/, tasks/, systems/, notes/, labels/
app/src/test/  unit tests (planner first)
```

## Commands
- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Install on a connected device/emulator: `./gradlew installDebug`
- Lint: `./gradlew lint`

## Working rules
- Build in the phase order from SPEC §7. Finish a phase, run the build and tests, then move on.
- `DayPlanner` stays a pure function with unit tests for every property listed in SPEC §3. Never put planning logic in ViewModels or composables.
- Edits save immediately through repositories; no Save buttons.
- Every list row's label chip opens the label picker; every editor shows labels as selectable chips.
- Two-tap confirm for deletes (button turns red: "Tap again to delete").
- Colors and fonts come from the theme (SPEC §5); no hard-coded colors in screens except label colors from data.
- Keep composables previewable (`@Preview` with fake data) where practical.
