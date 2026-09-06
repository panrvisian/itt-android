# ITT Android App

[中文](README.md)

> ITT (Individual Time Trial) is a local-first Android time-tracking app for recording, tracking, and analyzing personal activity durations.

## Overview

ITT organizes activities by groups and events. It supports normal timing, manual backfill, record editing, event cloning, timeline browsing, statistics, notes, and home-screen widgets. Data is stored locally by default and can be backed up or migrated through ZIP (CSV + images) or plain CSV export.

## Features

### Recording

- Tap an event to start timing; tap it again to stop
- Long-press a running record to stop it, with haptic feedback
- Manually enter start and end times
- Edit records with time shortcuts or clone a record
- Cloning first selects a group and then an event in that group; running records can also be cloned
- Overnight records are split automatically

### Groups & Events

- Organize events into groups, each with its own color
- "Ungrouped" is pinned at the bottom
- Events can be starred
- Home sorts events by record frequency; normal, backfilled, and cloned records are counted, while an overnight record counts once

### Home

- Current time display with an optional date
- Running records section
- Favorites and groups sections

### Timeline

- View record blocks by day
- Switch between proportional and compact views
- Proportional view uses actual time positions and durations; compact view arranges records in sequence instead of matching their start times
- Overlapping records use the existing column-width rules
- Block text puts the name and start/end time on one line when possible, keeps notes below them, and wraps or truncates with an ellipsis when needed
- Pinch-to-zoom uses the time positions under the two fingers as anchors and supports two-finger vertical movement
- One-finger scrolling uses normal drag and fling behavior; rotation is not recognized
- Navigate to the previous day, today, the next day, or a specific date
- Record details support editing, notes, cloning, and deletion

### Notes

- Add text notes and up to 10 images to each record
- Choose images from the gallery or camera
- Fullscreen editing is supported
- Drafts are saved automatically when leaving without saving
- Drafts are cleaned up according to record state and age

### Statistics

- View day, week, and month statistics
- Navigate to other days, weeks, and months before or after the current range
- Day, week, and month views share one base date
- Choose between total and unique duration statistics
- A group-duration pie chart shows accumulated time by group
- Select a group to view an event-duration pie chart; the event ranking is filtered to that group, and Back restores the complete view

### Home-screen Widgets

- 1×1 quick-timing widget
- 4×2 widget with 7 event cells and 1 edit cell
- Tap an event cell to start or stop timing
- Running events show a solid status dot
- Unassigned cells show a gray plus sign and open group-then-event selection when tapped
- Event names are displayed on one line and truncated with an ellipsis when necessary
- Each widget instance has independent event configuration, and the same event may be assigned more than once

### Onboarding & System UI

- A first-launch overlay guide can be skipped
- The guide can be opened again from Settings
- The grouped Settings screen uses slide transitions for subpages and includes Material and MiuiX presentation styles
- Themes can follow the system or stay light/dark; Monet is enabled by default, supports preset accent colors, and falls back to MiuiX defaults when disabled
- Bottom navigation uses MiuiX components and can switch between solid and fully rounded floating layouts; live blur and liquid glass are enabled by default for the floating layout
- Main pages switch through the bottom bar; horizontal pager gestures are disabled to avoid conflicts with sliders
- Edge-to-edge display accounts for the bottom gesture navigation bar
- System-bar icon colors follow the current theme

## Tech Stack

- Language: Kotlin
- UI: Jetpack Compose (Material 3, MiuiX-inspired components, Monet dynamic color)
- Database: Room 2.6.1 (database version 2, with migration)
- Preferences: DataStore Preferences
- Architecture: ViewModel + Repository
- Build: Gradle 9.7.1 / Android Gradle Plugin 9.3.2 / Kotlin 2.4.10 / KSP 2.3.10
- Min SDK: Android 12 (API 31)
- Target / Compile SDK: 34

## App Info

- Package: `com.bigbrother.mobile`
- Current version: `2.11`
- versionCode: `16`

## Requirements

- Windows, Android Studio, or PowerShell
- JDK 21
- Android SDK Platform 37.0
- Android SDK Build Tools 37.0.0
- Android SDK Platform-Tools (including `adb`)
- Internet access to resolve Gradle dependencies

## Build and automatic deployment

For a manual build, see [BUILD_APK.md](BUILD_APK.md).

For automatic build, installation, app launch, or wireless ADB, see [AUTO_INSTALL.md](AUTO_INSTALL.md). The scripts read `JAVA_HOME`, `ANDROID_SDK_ROOT`, or `ANDROID_HOME` from the current host instead of relying on the original developer machine's absolute paths.

Minimal build example (replace the paths with the actual paths on your host):

```powershell
$env:JAVA_HOME = 'C:\Path\To\jdk-21'
$env:ANDROID_SDK_ROOT = 'C:\Path\To\Android\Sdk'
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:Path"
.\gradlew.bat :app:assembleDebug --no-daemon
```

APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Project Structure

```text
itt-android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/bigbrother/mobile/
│       │   ├── data/        # Room, repository, DataStore, CSV codec
│       │   ├── domain/      # Statistics and time utilities
│       │   ├── ui/          # Compose screens, ViewModel, theme
│       │   └── widget/      # 1×1 and 4×2 home-screen widgets
│       └── res/             # Layouts and other resources
├── auto_install/            # Windows automatic build, install, and wireless ADB scripts
├── gradle/wrapper/           # Gradle Wrapper
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## Data & Storage

- Data is stored in the app's private Room database
- Note images are stored in `filesDir/notes/<recordId>/`
- Draft images are stored in `filesDir/notes_draft/<recordId>/`
- Only image file names are stored in the database

## Import / Export

- ZIP: contains CSV data and an image folder for full backup and restore of notes
- Plain CSV: compatible with older exports and contains text data only
- CSV parsing supports newlines, commas, and escaped quotes inside quoted fields

## Version Notes

The current source version is `2.11` (versionCode `16`). When later feature updates keep the same version number, they are still recorded in the `main` branch history; formal releases are identified by Git tags and GitHub Releases.

## Notes

- Project data is stored locally by default and is not uploaded to a server automatically
- Use the in-app Export feature for backups and keep the exported ZIP files safe
