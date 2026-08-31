# ITT Android App

[中文](README.md)

> ITT (Individual Time Trial) is an Android app for recording, tracking, and analyzing personal activity durations.

## Overview

ITT is a personal time-tracking app that organizes activities into groups and events, records every start and stop, and provides five main pages: Home, Timeline, Notes, Statistics, and Settings. All data is stored locally, and it supports import/export via ZIP (CSV + image folder) or plain CSV for backup and migration.

## Features

### Recording

- Tap an event to start timing; tap again to stop
- Long-press (0.5 s) a running card to stop it, with haptic feedback
- Manual backfill (start time / end time)
- "Previous record end + 1 minute" shortcut in the record editor
- Overnight records are split automatically

### Groups & Events

- Organize events into groups, each with its own color
- "Ungrouped" is pinned at the bottom and cannot be sorted
- Events can be starred (the star is shown in timeline blocks only)

### Home

- Current time display (date can be toggled)
- Running records section
- Favorites and groups sections

### Timeline

- Daily record blocks
- Pinch-to-zoom anchored at the time at the screen center
- Previous day / today / next day / jump to a specific date
- Note text is shown with a dynamic line count based on block height, truncated when it overflows

### Notes

- Add a text note and up to 10 images to each record
- Image sources: gallery multi-select / camera
- Fullscreen editing mode
- Drafts are auto-saved when exiting without saving
- Draft cleanup rules: drafts of running records are kept forever; drafts of finished records older than 1 day are removed; drafts of deleted records are removed

### Statistics

- Total duration statistics (sum / unique optional)
- Configurable semester, week start day, etc.

### Settings

- Theme: system / light / dark
- Font scale: small / medium / large / extra-large / system
- Wallpaper: default / image / solid color
- Vibration toggle
- Import/export: ZIP (CSV + image folder) or plain CSV

## Tech Stack

- Language: Kotlin
- UI: Jetpack Compose (Material 3)
- Database: Room 2.6.1 (version 2, with migration)
- Preferences: DataStore Preferences
- Architecture: ViewModel + Repository
- Build: Gradle 8.7 / Android Gradle Plugin 8.5.2 / Kotlin 1.9.24
- Min SDK: Android 12 (API 31)
- Target / Compile SDK: 34

## App Info

- Package: `com.bigbrother.mobile`
- Current version: 2.8 (versionCode 13)

## Requirements

- JDK 17
- Android SDK Platform 34, Build Tools 34.0.0
- Internet access to resolve Gradle dependencies

## Build

Build a debug APK from Windows PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug --no-daemon
```

APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Delivery naming example: `ITT-v2.8-build-yyyyMMdd-HHmm-debug.apk`

## Project Structure

```text
android-mobile/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/bigbrother/mobile/
│       │   ├── data/        # Room database, repository, DataStore, CSV codec
│       │   ├── domain/      # Statistics calculation, time utilities
│       │   └── ui/          # Compose UI, ViewModel, theme
│       └── res/             # Resources
├── gradle/wrapper/          # Gradle Wrapper
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## Data & Storage

- Data is stored in the app's private Room database (tables: records / groups / events / note_images)
- Note images are stored in `filesDir/notes/<recordId>/`; draft images in `filesDir/notes_draft/<recordId>/`
- Only image file names are stored in the database

## Import / Export

- ZIP: contains a CSV data file plus an image folder, for full backup/restore of notes and images
- Plain CSV: compatible with files exported by older versions; text data only
- CSV parsing supports newlines, commas, and escaped quotes inside quoted fields

## Version History

### v2.8 (2026-08-15)

- Added the notes feature: text notes and images (gallery / camera), fullscreen editing, auto-saved drafts and cleanup
- Timeline blocks show notes with a dynamic line count based on block height
- Import/export upgraded to ZIP (CSV + images)

Earlier changes are recorded in Git tags and commit history.

## Notes

- This is a personal-use app; data is stored locally by default
- For backups, use the in-app Export feature and keep the exported ZIP files safe
