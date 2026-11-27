# Furi Budget

An offline-first Android budget tracker where the look and feel is the point: five theme packs, a full accent-colour system, animated and photo backgrounds, and motion that explains what your money is doing. Everything lives on your phone, with no account and no tracking.

![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Offline first](https://img.shields.io/badge/Offline-first-informational)
![Vibecoded](https://img.shields.io/badge/Built%20with-AI%20(vibecoded)-orange)

> **This project is almost entirely vibecoded.**
> Nearly all of the code, tests, documentation and build tooling were written by an AI coding assistant ([Claude Code](https://www.anthropic.com/claude-code)) working from prompts and feedback. A human set the direction, made the product decisions, and tested the app on a phone with friends. It has not had a professional security or financial audit. See [How this was built](#how-this-was-built).

## Contents

- [Features](#features)
- [Privacy and data](#privacy-and-data)
- [Getting started](#getting-started)
- [Project structure](#project-structure)
- [How this was built](#how-this-was-built)
- [Credits and third-party material](#credits-and-third-party-material)
- [License](#license)

## Features

**Money**
- Multiple accounts with live balances. Hide, reorder, delete (moving everything to another account keeps every total), and a balance check that records an adjustment instead of silently editing history.
- Income, expense and transfer transactions with a fast keypad, notes, dates, tags and split transactions across categories.
- Categories with a searchable library of about 210 icons plus any emoji, and reorderable lists.
- Monthly and weekly budgets, overall or per category, with optional rollover and alerts as a limit nears.
- Entries in foreign currencies, stored with the original amount and rate. Exchange rates are optional, cached and off by default.
- **Free to spend**: balance minus money set aside minus bills still due, shown for each account and in total.

**Planning**
- **Goals**: a target, an optional deadline and monthly set-aside. Each goal either keeps its money in an account as "set aside" or moves it out of the balance.
- **Bills calendar**: bills, subscriptions, instalments, loans and expected income, with auto-pay, reminders and a *Paid* button right in the notification.
- **People ledger**: who owes you and whom you owe, linked to real transactions, with repayments and settling.
- Templates, a home-screen widget and launcher shortcuts for quick logging.

**Insight**
- Donut, paired bars and trend charts, a spending-by-tag view, and a pace line against the previous month.

**Safety nets**
- *Recently deleted* keeps removed items for 30 days, and swiping to delete a transaction or bill comes with Undo.
- JSON backup and restore, CSV export, and CSV import with a preview that skips duplicates and creates missing accounts, categories and tags.
- Optional app lock using the phone's screen lock.

**Look and feel**
- Five theme packs, 16 accent colours plus wallpaper, photo-derived and custom accents, light and dark.
- Backgrounds: drifting colour, aurora, particles, still gradient, solid, or your own photo with blur, dim and tilt parallax.
- A *minimal colours* switch (one accent, plain ink), colour-blind-safe income and expense colours, adjustable card opacity and an animation-intensity slider that also respects the system's "remove animations".
- Optional sounds (five built-in styles, or your own short audio file) and haptics with adjustable strength.

## Privacy and data

- All data is stored locally in an on-device database. There are no accounts, no analytics and no ads.
- The **only** network use is the optional exchange-rate update, which is off until you switch it on. It sends a single request to a public rates service carrying just your main currency code, and the result is cached so the app keeps working offline.
- There is no bank linking, now or planned.
- Back up from *Settings → Your data* before updating, especially across versions that change the database. Migrations are tested, but your money data deserves a copy.

## Getting started

### Requirements

- JDK 17 or newer
- Android SDK with the platform used by `compileSdk` (Android SDK 37)
- An Android 8.0 (API 26) or newer device or emulator

### Build and run

```bash
git clone <this repository>
cd Furi-Budget

# point Gradle at your Android SDK if it is not already set
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Tests and checks

```bash
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug   # unit tests and lint
./gradlew :app:connectedDebugAndroidTest                     # database migration tests (needs a device or emulator)
```

### Release build

Release builds are minified and shrunk. To sign one, create `~/.android-keystores/furibudget.properties` with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`; without it the release APK is left unsigned. Keep your keystore and that file out of version control.

```bash
./gradlew :app:assembleRelease
```

## Project structure

| Path | What it holds |
| --- | --- |
| `core/` | Pure Kotlin/JVM logic with no Android dependencies: money, currencies, exchange maths, bill schedules, periods, colour science. |
| `app/` | The Android app: Room database, repositories, view models and the Compose UI. |
| `app/src/main/kotlin/app/furibudget/data` | Entities, DAOs, migrations, backup, CSV import, and the money logic for budgets, funds, goals and loans. |
| `app/src/main/kotlin/app/furibudget/ui` | Screens and components, plus the theme system under `ui/theme`. |
| `app/schemas/` | Exported Room schemas, one per database version. |
| `tools/make_sounds.py` | Script that synthesises the built-in sound effects. |
| `PRODUCT.md`, `DESIGN.md` | Product intent and the design system, written down for people and for AI assistants. |

**Stack:** Kotlin, Jetpack Compose (Material 3), Room, DataStore, kotlinx.serialization, Navigation Compose, Glance (widget), Coil, AGSL shaders where the OS supports them.

## How this was built

This is a vibecoding project. In practice that means:

- **The AI wrote almost everything**: application code, unit and migration tests, database migrations, build configuration, documentation, the sound-synthesis script, and the app icon artwork (generated with an AI image tool, see [`ICON_SOURCE.md`](ICON_SOURCE.md)).
- **The human directed it**: deciding what to build, reviewing screenshots, running the app on a phone, collecting feedback from friends, and asking for changes. Features, bugs and design choices came out of that back-and-forth rather than from a written specification.
- **Verification** is a mix of automated tests (including tests that upgrade real older databases), lint, and hands-on testing on an emulator and on phones. That is good for catching regressions, but it is not an independent audit.

What that means for you:

- Treat it as a personal project, not a vetted financial product. Read the code before trusting it with anything important, and keep backups.
- Expect rough edges and code that is more verbose or more uniform than a hand-written project would be.
- Contributions and bug reports are welcome. Say so in your pull request if your change was AI-assisted too.

## Credits and third-party material

- **Fonts** (SIL Open Font License 1.1, licence texts in `app/licenses/`): Manrope, Sora, Fredoka, Bricolage Grotesque and Hanken Grotesk.
- **Icons** in the app come from Material Icons (Apache License 2.0).
- **Libraries**: AndroidX, Jetpack Compose, Room, Coil, Kotlin and kotlinx libraries, each under their own open-source licences.
- **Exchange rates** (when enabled) come from the public [open.er-api.com](https://www.exchangerate-api.com/docs/free) endpoint.
- **Sound effects** are generated by `tools/make_sounds.py` and contain no third-party recordings.
- **App icon** was generated with an AI image tool from a text prompt. Details and provenance are in [`ICON_SOURCE.md`](ICON_SOURCE.md).

## License

No license has been chosen yet. Until one is added, all rights are reserved by the author and the code may not be reused or redistributed.
