# Daily Systems (Android) — GitHub Push Tracking & Audit Guide

> **Document Status**: Production Ready  
> **Last Updated**: November 2026  
> **Repository Target**: `https://github.com/<your-username>/daily-systems`  
> **Project Scope**: Native Kotlin + Jetpack Compose Android Application  

---

## Table of Contents
1. [Executive Summary & Health Check](#1-executive-summary--health-check)
2. [Security & Privacy Audit](#2-security--privacy-audit)
3. [Master File Decision Matrix](#3-master-file-decision-matrix)
   - [3.1 Root Configuration & Build Engine](#31-root-configuration--build-engine)
   - [3.2 Gradle Wrapper Toolchain](#32-gradle-wrapper-toolchain)
   - [3.3 Room Database Schema Migrations](#33-room-database-schema-migrations)
   - [3.4 App Module Configuration](#34-app-module-configuration)
   - [3.5 Android Manifest & App Resources](#35-android-manifest--app-resources)
   - [3.6 Typography & Font Assets](#36-typography--font-assets)
   - [3.7 Kotlin Production Source Code](#37-kotlin-production-source-code)
   - [3.8 Automated Test Suite (Robolectric & Unit)](#38-automated-test-suite-robolectric--unit)
   - [3.9 Documentation & Prototype Assets](#39-documentation--prototype-assets)
4. [Files & Folders That MUST NOT Be Pushed](#4-files--folders-that-must-not-be-pushed)
5. [Repository Enhancements & Infrastructure Gaps](#5-repository-enhancements--infrastructure-gaps)
   - [5.1 Enhanced `.gitignore`](#51-enhanced-gitignore)
   - [5.2 Line Ending Protection (`.gitattributes`)](#52-line-ending-protection-gitattributes)
   - [5.3 Recommended GitHub Actions CI Workflow](#53-recommended-github-actions-ci-workflow)
   - [5.4 License Selection (`LICENSE`)](#54-license-selection-license)
6. [Step-by-Step GitHub Push Playbook](#6-step-by-step-github-push-playbook)
7. [Interactive Tracking Checklist](#7-interactive-tracking-checklist)

---

## 1. Executive Summary & Health Check

A comprehensive audit of `d:\Projetcs\daily-systems\daily-systems` was conducted to establish a baseline for publishing to GitHub.

| Metric | Status / Value | Notes |
|---|---|---|
| **Git Repository Status** | **Uninitialized** | No `.git` directory exists yet. Clean slate. |
| **Total Trackable Files** | **115 files** | Clean project structure, zero extraneous clutter. |
| **Total Trackable Footprint** | **~3.44 MB** | Highly lightweight; well below GitHub's 100 MB file limit. |
| **Largest File in Repo** | `01-today.png` (271.7 KB) | No Git LFS required. |
| **Android Toolchain** | AGP 9.5.0, Gradle 9.8.2, Kotlin 2.4.21, KSP 2.3.13 | Modern stable stack. |
| **Android SDK Targets** | `minSdk 26`, `compileSdk 37`, `targetSdk 37` | Android 8.0 to Android 15+. |
| **Secrets / Credential Scan** | **0 Secrets Found** | No API keys, tokens, or hardcoded passwords detected. |

---

## 2. Security & Privacy Audit

Before pushing any repository to GitHub (especially public repositories), all files must be scanned for sensitive data.

### 2.1 Scan Results
- **API Keys / Auth Tokens**: `0 found`.
- **Private Keys / Keystores**: `0 found`. (No `.jks`, `.keystore`, `.pem`, or `.key` files in repo).
- **Local Machine Paths**: `0 found`. (`local.properties` is absent).
- **Database Files**: `0 found`. (No local SQLite `.db` or WAL files present; only Room schema JSON definitions).

### 2.2 Critical Guardrails
1. **Never commit `local.properties`**: This file contains the local developer's Android SDK path (e.g. `sdk.dir=C:\Users\<user>\AppData\Local\Android\Sdk`) and must stay local.
2. **Never commit Release Keystores or `signing.properties`**: Keep production signing keys (`*.jks`, `*.keystore`) strictly out of version control. For GitHub Actions builds, use GitHub Actions Secrets (`ENCODED_KEYSTORE`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, `SIGNING_STORE_PASSWORD`).
3. **Room Schemas are Safe & Mandatory**: The exported schema files (`app/schemas/.../1.json`, `2.json`) contain table schemas and column metadata, but **no user data**. They are required for migration testing and should always be committed.

---

## 3. Master File Decision Matrix

Every file currently in the project has been reviewed and classified into one of three statuses:
- `PUSH`: Crucial file that must be committed to GitHub.
- `IGNORE`: Build artifact, cache, or machine-specific file that must be ignored.
- `OPTIONAL / RECOMMENDED`: Files that improve repository health (e.g. CI workflow, license, gitattributes).

### 3.1 Root Configuration & Build Engine

| Path | Size | Status | Rationale |
|---|---|---|---|
| `build.gradle.kts` | 304 B | `PUSH` | Root Gradle build configuration; defines plugin aliases (AGP, Kotlin Compose, KSP). |
| `settings.gradle.kts` | 331 B | `PUSH` | Defines Gradle repository management (Google, MavenCentral) and includes the `:app` module. |
| `gradle.properties` | 193 B | `PUSH` | Global JVM args (`-Xmx4g`), build caching, AndroidX enablement, and configuration cache flags. |
| `gradlew` | 8.53 KB | `PUSH` | POSIX shell script wrapper for executing Gradle on Linux/macOS and GitHub Actions CI. |
| `gradlew.bat` | 2.87 KB | `PUSH` | Windows batch wrapper script for executing Gradle on Windows workstations. |
| `README.md` | 1.37 KB | `PUSH` | Primary entry documentation for developers; build commands and architecture breakdown. |
| `CLAUDE.md` | 1.79 KB | `PUSH` | AI agent development guidelines, architecture layout, and core workflow constraints. |

---

### 3.2 Gradle Wrapper Toolchain

| Path | Size | Status | Rationale |
|---|---|---|---|
| `gradle/libs.versions.toml` | 2.88 KB | `PUSH` | Version Catalog centralizing all dependency versions, libraries, and plugins across modules. |
| `gradle/wrapper/gradle-wrapper.jar` | 42.74 KB | `PUSH` | The standalone bootstrap binary that enables building without a pre-installed Gradle. *Standard across Android/Gradle repositories.* |
| `gradle/wrapper/gradle-wrapper.properties` | 252 B | `PUSH` | Pins the exact Gradle distribution URL (`gradle-9.8.2-bin.zip`). |

---

### 3.3 Room Database Schema Migrations

| Path | Size | Status | Rationale |
|---|---|---|---|
| `app/schemas/com.dailysystems.app.data.AppDatabase/1.json` | 17.92 KB | `PUSH` | Room Schema v1 export. Essential for `MigrationTest.kt` to verify database schema version 1. |
| `app/schemas/com.dailysystems.app.data.AppDatabase/2.json` | 19.27 KB | `PUSH` | Room Schema v2 export. Tracks migration additions (recurrence rules, reminder flags). |

> **Why this must be pushed**: `app/build.gradle.kts` explicitly exposes `$projectDir/schemas` to the `debug` assets source set so Robolectric unit tests can run real SQLite schema migrations between v1 and v2.

---

### 3.4 App Module Configuration

| Path | Size | Status | Rationale |
|---|---|---|---|
| `app/build.gradle.kts` | 3.10 KB | `PUSH` | Application module build configuration: SDK versions (compile/target 37, min 26), Compose BOM, Room KSP, and dependencies. |
| `app/proguard-rules.pro` | 95 B | `PUSH` | R8 / ProGuard optimization rules for release build minification. |

---

### 3.5 Android Manifest & App Resources

| Path | Size | Status | Rationale |
|---|---|---|---|
| `app/src/main/AndroidManifest.xml` | 2.37 KB | `PUSH` | App manifest: permissions (`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`), `MainActivity`, receivers, and widget metadata. |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | 1.06 KB | `PUSH` | Vector asset for launcher icon foreground. |
| `app/src/main/res/drawable/ic_stat_notify.xml` | 640 B | `PUSH` | Vector asset for status bar reminder notifications. |
| `app/src/main/res/drawable/widget_check.xml` | 420 B | `PUSH` | Vector asset for widget completed state icon. |
| `app/src/main/res/drawable/widget_dot.xml` | 320 B | `PUSH` | Vector asset for widget bullet points. |
| `app/src/main/res/drawable/widget_ring.xml` | 370 B | `PUSH` | Vector asset for widget unchecked slot circle. |
| `app/src/main/res/mipmap-anydpi/ic_launcher.xml` | 330 B | `PUSH` | Adaptive icon XML wrapper combining background and foreground. |
| `app/src/main/res/values/colors.xml` | 160 B | `PUSH` | Base fallback color definitions for light theme. |
| `app/src/main/res/values/strings.xml` | 220 B | `PUSH` | Application name and basic localization strings. |
| `app/src/main/res/values/themes.xml` | 470 B | `PUSH` | Base Material 3 theme XML declaration. |
| `app/src/main/res/values-night/colors.xml` | 110 B | `PUSH` | Dark theme color overrides. |
| `app/src/main/res/values-night/themes.xml` | 380 B | `PUSH` | Dark theme XML declaration. |
| `app/src/main/res/xml/backup_rules.xml` | 120 B | `PUSH` | Android Auto Backup inclusion/exclusion rules. |
| `app/src/main/res/xml/data_extraction_rules.xml` | 260 B | `PUSH` | Android 12+ cloud and device-to-device transfer rules. |
| `app/src/main/res/xml/today_widget_info.xml` | 550 B | `PUSH` | AppWidgetProvider metadata configuring the Today home-screen widget. |

---

### 3.6 Typography & Font Assets

Custom TrueType fonts supporting the application's Material 3 typography design:

| Path | Size | Status | Rationale |
|---|---|---|---|
| `app/src/main/res/font/bricolage_bold.ttf` | 80.18 KB | `PUSH` | Bricolage Grotesque Bold font for prominent headlines. |
| `app/src/main/res/font/bricolage_medium.ttf` | 80.32 KB | `PUSH` | Bricolage Grotesque Medium font for screen titles. |
| `app/src/main/res/font/instrument_bold.ttf` | 47.41 KB | `PUSH` | Instrument Sans Bold font for chips and emphasis. |
| `app/src/main/res/font/instrument_medium.ttf` | 47.59 KB | `PUSH` | Instrument Sans Medium font for labels and buttons. |
| `app/src/main/res/font/instrument_regular.ttf` | 47.48 KB | `PUSH` | Instrument Sans Regular font for body copy. |
| `app/src/main/res/font/instrument_semibold.ttf` | 47.59 KB | `PUSH` | Instrument Sans SemiBold font for list headers. |
| `app/src/main/res/font/plexmono_medium.ttf` | 126.87 KB | `PUSH` | IBM Plex Mono Medium font for times, schedules, and badges. |
| `app/src/main/res/font/plexmono_regular.ttf` | 125.79 KB | `PUSH` | IBM Plex Mono Regular font for slot tags and metadata. |

---

### 3.7 Kotlin Production Source Code

All 49 production Kotlin files across the application architecture:

#### Root Application Layer (`app/src/main/java/com/dailysystems/app/`)
| File | Size | Status | Purpose |
|---|---|---|---|
| `AppContainer.kt` | 3.00 KB | `PUSH` | Manual DI container; initializes database, repositories, ordered mutation queue, and background sync. |
| `BackgroundSync.kt` | 1.27 KB | `PUSH` | Observes database updates to trigger widget refreshes and alarm re-scheduling. |
| `DailySystemsApp.kt` | 0.76 KB | `PUSH` | `Application` class entry; sets up notification channels and instantiates `AppContainer`. |
| `MainActivity.kt` | 0.52 KB | `PUSH` | Single activity hosting Compose content with edge-to-edge system insets. |

#### Data & Storage Layer (`.../data/`)
| File | Size | Status | Purpose |
|---|---|---|---|
| `AppDatabase.kt` | 1.31 KB | `PUSH` | Room database definition with version 2 and migration logic from v1. |
| `Converters.kt` | 1.03 KB | `PUSH` | Room TypeConverters for `LocalDate`, `ScheduleType`, and enum storage. |
| `Daos.kt` | 7.41 KB | `PUSH` | Room DAOs (`LabelDao`, `TaskDao`, `SystemDao`, `NoteDao`) with reactive Flow queries. |
| `Entities.kt` | 4.26 KB | `PUSH` | SQLite table definitions for labels, tasks, system routines, items, and notes. |
| `Mappers.kt` | 3.26 KB | `PUSH` | Converters between Room entity models and pure Kotlin domain entities. |
| `Repositories.kt` | 14.38 KB | `PUSH` | Concrete repository implementations featuring serialized background write pipelines. |
| `StarterExamples.kt` | 4.04 KB | `PUSH` | Starter default priority labels, sample routine systems, and onboarding tasks. |

#### Pure Domain Layer (`.../domain/`)
| File | Size | Status | Purpose |
|---|---|---|---|
| `DayPlanner.kt` | 6.20 KB | `PUSH` | Pure Kotlin planning algorithm; fills system routine slots with highest-priority open tasks. |
| `Models.kt` | 2.60 KB | `PUSH` | Immutable domain models (`PriorityLabel`, `Task`, `SystemRoutine`, `DayPlan`). |
| `Recurrence.kt` | 0.60 KB | `PUSH` | Recurrence date calculations for recurring tasks. |
| `ReminderPlanner.kt` | 2.08 KB | `PUSH` | Computes scheduled reminder trigger timestamps. |
| `RepeatRule.kt` | 2.55 KB | `PUSH` | Parsing, serialization, and evaluation of recurrence rules. |

#### Reminders & Alarms (`.../reminders/`)
| File | Size | Status | Purpose |
|---|---|---|---|
| `Notifications.kt` | 5.26 KB | `PUSH` | System notification builders, action intents (mark done, snooze), and channels. |
| `Receivers.kt` | 2.26 KB | `PUSH` | BroadcastReceivers for alarm fires and device boot / time-zone change reschedule events. |
| `ReminderScheduler.kt` | 3.33 KB | `PUSH` | Manages exact alarms with Android `AlarmManager`. |

#### UI Architecture & Base (`.../ui/`)
| File | Size | Status | Purpose |
|---|---|---|---|
| `DailySystemsRoot.kt` | 4.86 KB | `PUSH` | Root navigation scaffold, bottom bar destinations, and navigation graph. |
| `Format.kt` | 4.42 KB | `PUSH` | Formatting utilities for dates, times, recurrence summaries, and label badges. |
| `SheetsViewModel.kt` | 9.31 KB | `PUSH` | Coordinates modal bottom sheets (task editor, system editor, note editor, label picker). |
| `ViewModels.kt` | 1.05 KB | `PUSH` | Shared ViewModel factory helpers. |

#### UI Reusable Components (`.../ui/components/`)
| File | Size | Status | Purpose |
|---|---|---|---|
| `Components.kt` | 16.16 KB | `PUSH` | Reusable UI atoms: buttons, cards, status chips, delete confirmations, empty states. |
| `DatePickers.kt` | 3.93 KB | `PUSH` | Date selection bottom sheets and quick date chips (Today, Tomorrow, Pick Date). |
| `DragHandle.kt` | 1.41 KB | `PUSH` | Drag-to-reorder handle indicator for reorderable lists. |
| `Icons.kt` | 3.38 KB | `PUSH` | Icon definitions and vector painting utilities. |
| `Pickers.kt` | 3.03 KB | `PUSH` | Dropdowns, priority rank selection, and color palettes. |
| `ScreenHeader.kt` | 1.49 KB | `PUSH` | Top app bar header with title, subtitle, and action buttons. |
| `Styles.kt` | 1.95 KB | `PUSH` | Typography styling, padding constants, and layout tokens. |
| `TaskRow.kt` | 4.17 KB | `PUSH` | Interactive task row with completion checkbox, priority chip, and swipe actions. |
| `TimeField.kt` | 5.28 KB | `PUSH` | Formatted 12h/24h time input component for reminder configuration. |

#### UI Modal Editors (`.../ui/editors/`)
| File | Size | Status | Purpose |
|---|---|---|---|
| `NoteEditorSheet.kt` | 3.10 KB | `PUSH` | Editor sheet for standalone notes and task-linked notes. |
| `SheetChrome.kt` | 1.74 KB | `PUSH` | Common header chrome and drag surface for modal bottom sheets. |
| `SheetHost.kt` | 2.54 KB | `PUSH` | Host composable rendering active bottom sheets based on `SheetsViewModel`. |
| `SystemEditorSheet.kt` | 15.18 KB | `PUSH` | Multi-section editor for routines, schedule types, step list, and slot definitions. |
| `TaskEditorSheet.kt` | 10.51 KB | `PUSH` | Editor for task title, priority label, target date, recurrence, and notes. |

#### Priority Labels UI (`.../ui/labels/`)
| File | Size | Status | Purpose |
|---|---|---|---|
| `LabelPickerSheet.kt` | 3.89 KB | `PUSH` | Quick label selection sheet for assigning priority labels to tasks. |
| `LabelsManagerSheet.kt` | 8.87 KB | `PUSH` | Full management modal for creating, recoloring, reordering, and deleting labels. |

#### Feature Screens & ViewModels (`.../ui/notes/`, `.../systems/`, `.../tasks/`, `.../today/`)
| File | Size | Status | Purpose |
|---|---|---|---|
| `notes/NotesScreen.kt` | 6.58 KB | `PUSH` | Notes screen showing standalone notes and task notes. |
| `notes/NotesViewModel.kt` | 2.92 KB | `PUSH` | ViewModel managing note search, filtering, and CRUD. |
| `systems/SystemsScreen.kt` | 8.88 KB | `PUSH` | Systems list screen with active/paused sections and reordering. |
| `systems/SystemsViewModel.kt` | 4.26 KB | `PUSH` | ViewModel managing routines and drag-to-reorder state. |
| `tasks/TasksScreen.kt` | 10.25 KB | `PUSH` | Master to-do list screen with filter tabs (open, completed, priority). |
| `tasks/TasksViewModel.kt` | 4.09 KB | `PUSH` | ViewModel managing task backlog, priority sorting, and quick additions. |
| `today/TodayScreen.kt` | 19.91 KB | `PUSH` | Today dashboard: planned systems, slot assignments, and ad-hoc daily tasks. |
| `today/TodayViewModel.kt` | 3.88 KB | `PUSH` | ViewModel computing the daily plan via `DayPlanner` from reactive flows. |

#### Theme & Styling (`.../ui/theme/`)
| File | Size | Status | Purpose |
|---|---|---|---|
| `Color.kt` | 3.99 KB | `PUSH` | Material 3 color definitions for light and dark color schemes. |
| `Theme.kt` | 2.50 KB | `PUSH` | `DailySystemsTheme` provider with dynamic color support and edge-to-edge chrome. |
| `Type.kt` | 3.89 KB | `PUSH` | Jetpack Compose `Typography` configuration using custom font families. |

#### Home Screen Widget (`.../widget/`)
| File | Size | Status | Purpose |
|---|---|---|---|
| `TodayWidget.kt` | 10.60 KB | `PUSH` | Jetpack Glance app widget displaying today's plan with interactive check-offs. |

---

### 3.8 Automated Test Suite (Robolectric & Unit)

All 11 unit, integration, and UI test files in `app/src/test/java/com/dailysystems/app/`:

| File | Size | Status | Purpose |
|---|---|---|---|
| `RemindersAndWidgetTest.kt` | 3.55 KB | `PUSH` | Unit tests for reminder alarms and widget state builders. |
| `TestSupport.kt` | 0.47 KB | `PUSH` | Test fixtures, sample model builders, and coroutine test dispatchers. |
| `data/MigrationTest.kt` | 2.08 KB | `PUSH` | Room migration test verifying SQLite schema upgrade from v1 to v2. |
| `data/RecurrenceRepositoryTest.kt` | 5.20 KB | `PUSH` | Tests for recurring task generation and repeat rule persistence. |
| `data/RepositoryTest.kt` | 6.75 KB | `PUSH` | Integration tests verifying database transactions and ordered mutation queues. |
| `domain/DayPlannerTest.kt` | 14.79 KB | `PUSH` | Comprehensive test suite for the `DayPlanner` slot-filling algorithm. |
| `domain/ReminderPlannerTest.kt` | 2.53 KB | `PUSH` | Tests for reminder timing calculations and notification windowing. |
| `domain/RepeatRuleTest.kt` | 3.53 KB | `PUSH` | Unit tests for weekday masks, monthly rules, and recurrence parsing. |
| `ui/AppFlowsTest.kt` | 9.55 KB | `PUSH` | Robolectric Compose UI test validating navigation, creation, and check-offs. |
| `ui/ScreenshotTest.kt` | 4.84 KB | `PUSH` | Automated screenshot test capturing light/dark screen states. |
| `widget/WidgetRenderTest.kt` | 2.64 KB | `PUSH` | Unit tests for Glance widget layout composition and item limits. |

---

### 3.9 Documentation & Prototype Assets

| File | Size | Status | Purpose |
|---|---|---|---|
| `docs/SPEC.md` | 15.02 KB | `PUSH` | Product & technical specification (the single source of truth for features). |
| `docs/prototype.html` | 70.98 KB | `PUSH` | Standalone HTML/JS web prototype used as visual and behavioral benchmark. |
| `docs/screenshots/01-today.png` | 271.66 KB | `PUSH` | Light mode screenshot: Today screen with active system and filled slots. |
| `docs/screenshots/01-today-dark.png` | 269.23 KB | `PUSH` | Dark mode screenshot: Today screen. |
| `docs/screenshots/02-today-scrolled.png` | 256.77 KB | `PUSH` | Light mode screenshot: Today screen scrolled view. |
| `docs/screenshots/02-today-scrolled-dark.png` | 252.97 KB | `PUSH` | Dark mode screenshot: Today screen scrolled view. |
| `docs/screenshots/03-todos.png` | 243.49 KB | `PUSH` | Light mode screenshot: Tasks backlog screen. |
| `docs/screenshots/03-todos-dark.png` | 237.79 KB | `PUSH` | Dark mode screenshot: Tasks backlog screen. |
| `docs/screenshots/04-systems.png` | 161.73 KB | `PUSH` | Light mode screenshot: Systems list screen. |
| `docs/screenshots/04-systems-dark.png` | 157.97 KB | `PUSH` | Dark mode screenshot: Systems list screen. |
| `docs/screenshots/06-notes.png` | 142.76 KB | `PUSH` | Light mode screenshot: Notes screen with standalone & task notes. |
| `docs/screenshots/06-notes-dark.png` | 141.15 KB | `PUSH` | Dark mode screenshot: Notes screen. |
| `docs/screenshots/07-todos-recurring-row.png` | 250.88 KB | `PUSH` | Recurring task row highlight screenshot. |
| `docs/screenshots/09-widget.png` | 18.95 KB | `PUSH` | Light mode screenshot: Glance home screen widget preview. |
| `docs/screenshots/09-widget-dark.png` | 19.05 KB | `PUSH` | Dark mode screenshot: Glance home screen widget preview. |

---

## 4. Files & Folders That MUST NOT Be Pushed

The following files and patterns are strictly blacklisted from being committed to Git:

| Path / Pattern | Reason for Exclusion | Danger Level |
|---|---|---|
| `.gradle/` | Local Gradle cache, daemon lockfiles, and execution history. Machine-specific. | High (Repository bloat) |
| `build/` and `app/build/` | Compiled APKs, AARs, generated code, `.class` files, intermediate DEX, lint reports. | High (Bloat & merge conflicts) |
| `.kotlin/` | Kotlin compiler daemon incremental session cache. | Medium |
| `local.properties` | Developer's local Android SDK directory path. Will break other users' builds. | High |
| `.idea/` | Android Studio / IntelliJ user settings, local window states, task history. | Medium (Pollution) |
| `*.iml` | Legacy IntelliJ module files generated dynamically by Gradle sync. | Low |
| `*.jks`, `*.keystore` | Private Android app signing keys and certificates. | **CRITICAL SECURITY RISK** |
| `signing.properties`, `key.properties` | Plaintext passwords for keystores. | **CRITICAL SECURITY RISK** |
| `.env*` | Environment variable files. | **CRITICAL SECURITY RISK** |
| `captures/` | Android Studio memory, CPU, and GPU trace files. | Medium |
| `.cxx/`, `.externalNativeBuild/` | C++ / NDK native compilation intermediate files. | High |
| `*.log` | Build and runtime log files. | Low |
| `.DS_Store`, `Thumbs.db`, `Desktop.ini` | Operating system folder view metadata. | Low |
| `/tmp/shots/` or local test outputs | Screenshot test output directories when running `-PscreenshotDir`. | Medium |

---

## 5. Repository Enhancements & Infrastructure Gaps

During our audit, 4 essential infrastructure files were identified that should be added or upgraded to achieve production readiness:

### 5.1 Enhanced `.gitignore`
The initial `.gitignore` contained only 8 lines. It has been replaced with the complete, industry-standard Android `.gitignore`:

```gitignore
# Built application files
*.apk
*.aar
*.ap_
*.aab

# Files for the ART/Dalvik VM
*.dex

# Java class files
*.class

# Generated files
bin/
gen/
out/
build/
**/build/

# Gradle files
.gradle/
buildOutputCleanup/

# Local configuration file (sdk path, etc)
local.properties

# Proguard folder generated by Eclipse / Android Studio
proguard/

# Log Files
*.log

# Android Studio Navigation editor temp files
.navigation/

# Android Studio captures folder
captures/

# IntelliJ IDEA
*.iml
.idea/
.idea/modules.xml
.idea/workspace.xml
.idea/tasks.xml
.idea/caches
.idea/libraries

# Keystore files (NEVER commit signing keys)
*.jks
*.keystore
signing.properties
key.properties

# External native build folder
.externalNativeBuild/
.cxx/

# OS / Editor files
.DS_Store
Thumbs.db
ehthumbs.db
Desktop.ini
*.swp
*.swo
*~
.vscode/

# Kotlin compiler caches
.kotlin/

# Test output directories
/tmp/shots/
screenshots/out/
```

### 5.2 Line Ending Protection (`.gitattributes`)
When pushing from Windows to GitHub, two critical issues can occur:
1. `gradlew` can have its line endings converted to CRLF (`\r\n`), causing it to fail on Linux/macOS and GitHub Actions with `/bin/sh^M: bad interpreter`.
2. Binary files (`.png`, `.ttf`, `.jar`) might be corrupted if Git treats them as text.

Create `.gitattributes` at the project root:
```gitattributes
# Auto-detect text files and normalize line endings
* text=auto

# Linux scripts MUST keep LF line endings
gradlew text eol=lf
*.sh text eol=lf

# Windows batch files MUST keep CRLF line endings
gradlew.bat text eol=crlf
*.bat text eol=crlf

# Android code and resources
*.kt text
*.kts text
*.xml text
*.json text
*.properties text
*.md text
*.html text

# Binary assets (prevent corruption)
*.jar binary
*.png binary
*.ttf binary
*.ico binary
*.webp binary
```

### 5.3 Recommended GitHub Actions CI Workflow
To ensure that all PRs and commits compile and pass tests automatically, create `.github/workflows/android-ci.yml`:

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest --stacktrace

      - name: Run Lint
        run: ./gradlew lintDebug

      - name: Assemble Debug APK
        run: ./gradlew assembleDebug
```

### 5.4 License Selection (`LICENSE`)
The repository currently lacks a license file. Recommended options:
- **MIT License**: Maximum permissiveness; allows anyone to use, modify, and distribute with attribution.
- **Apache 2.0**: Permissive with explicit patent protection and trademark reservation (standard for Android open-source).
- **All Rights Reserved**: If this is a proprietary private repository.

---

## 6. Step-by-Step GitHub Push Playbook

Follow these exact shell commands to initialize and push the repository to GitHub:

### Step 1: Open PowerShell in the project directory
```powershell
cd d:\Projetcs\daily-systems\daily-systems
```

### Step 2: Initialize Git with `main` as default branch
```powershell
git init -b main
```

### Step 3: Configure Git User (if not set globally)
```powershell
git config user.name "Your Name"
git config user.email "your.email@example.com"
```

### Step 4: Ensure `gradlew` has executable permissions in Git
On Windows, file permissions aren't fully recorded by default. Force Git to store `gradlew` as an executable (`chmod +x`):
```powershell
git add gradlew
git update-index --chmod=+x gradlew
```

### Step 5: Stage all files and verify
```powershell
git add .
git status
```
> **Check**: Verify that only the 115 expected files (plus any new `.gitattributes`, `LICENSE`, or workflow files) are staged, and that **no** `.gradle/`, `build/`, or `local.properties` files appear.

### Step 6: Create the initial commit
```powershell
git commit -m "feat: initial commit - Daily Systems Android app (Kotlin, Jetpack Compose, Room, Glance)"
```

### Step 7: Create a new repository on GitHub
1. Go to [github.com/new](https://github.com/new).
2. Name the repository: `daily-systems` (or your chosen name).
3. Choose **Public** or **Private**.
4. **Do NOT check** "Add a README file", "Add .gitignore", or "Choose a license" (we already have all of them).
5. Click **Create repository**.

### Step 8: Link Remote and Push
```powershell
# For HTTPS:
git remote add origin https://github.com/<your-username>/daily-systems.git

# Or for SSH:
# git remote add origin git@github.com:<your-username>/daily-systems.git

git push -u origin main
```

---

## 7. Interactive Tracking Checklist

Use this checklist to track the progress of onboarding and pushing to GitHub:

- [ ] **Repository Configuration**
  - [x] Project files audited (115 files identified)
  - [x] Security audit completed (0 credentials or keys found)
  - [x] `.gitignore` updated with full Android rules
  - [x] `.gitattributes` created with LF protection for `gradlew`
  - [ ] `LICENSE` added (MIT / Apache-2.0 / Proprietary)
  - [x] GitHub Actions CI workflow added (`.github/workflows/android-ci.yml`)

- [ ] **Git Initialization & Staging**
  - [ ] `git init -b main` executed
  - [ ] `git update-index --chmod=+x gradlew` executed
  - [ ] All 115 project files staged (`git add .`)
  - [ ] `git status` inspected — zero unwanted files staged
  - [ ] Initial commit created

- [ ] **GitHub Remote & Verification**
  - [ ] GitHub repository created on github.com
  - [ ] Remote `origin` added
  - [ ] Code pushed to `origin main`
  - [ ] GitHub Actions CI workflow passes on `main`
  - [ ] Repository description, topics, and README preview verified
