# PLAYCE - Tennis & Padel Score Tracker

> **play** + **ace** - A native Android app for tracking live tennis and padel scores, built for the court.

PLAYCE lets you keep score directly from your wrist during a match. The Wear OS app runs the live scoring engine while the Android phone displays the score in real time and stores your match history.

---

## Why I built this

Every time I played tennis, keeping score meant stopping the game, arguing about sets, or relying on memory. Existing apps were either too complex or required your phone on court. I wanted something simple: tap your watch, score updates, done.

---

## Features

- **Wear OS scorer** - tap-based point tracking directly on your smartwatch, no phone needed during play
- **Serve guidance on wrist** - current server highlight plus dynamic left/right serve-side halo during each game
- **Live sync** - phone receives live score updates from the watch via WatchConnectivity with ACK confirmation
- **Match history** - all finished matches stored locally on your phone with Room database
- **Share card** - generate and share a match summary card after each game
- **Premium gate** - freemium model with local persistence; free mode keeps a manual counter on phone

---

## Tech stack

| Layer | Stack |
|---|---|
| Wear OS app | Kotlin, Jetpack Compose for Wear |
| Android phone | Kotlin, Jetpack Compose, Room, ViewModel |
| Sync | Wearable Data Layer API (WatchConnectivity) |
| Crash reporting | Firebase Crashlytics (optional, flag-controlled) |
| Build | Gradle KTS, release keystore pipeline |

---

## Architecture

```text
app/          -> Wear OS module (scorer, timer, watch-side sync)
mobile/       -> Phone module (history, match detail, share card)
shared logic  -> Scoring rules, payload contracts, premium state
```

The watch is the primary input device during the match. The phone acts as the companion display and local archive.

---

## Main flows

1. Start a match from the watch
2. Track points, games, and sets in real time
3. Sync score state to the phone with delivery acknowledgement
4. Save completed matches into local history
5. Share a polished result card after the match

---

## Build

### Release configuration

Set these values in `gradle.properties`:

- `PLAYCE_VERSION_CODE`
- `PLAYCE_VERSION_NAME`

Create `keystore.properties` from the template and provide the real signing values before building release artifacts.

### Commands

```bash
./gradlew :mobile:bundleRelease
./gradlew :app:assembleRelease
```

---

## Crash reporting

Crashlytics can be enabled with the corresponding Gradle flag. If used, place `google-services.json` in the modules that report crashes.

---

## Status

This project is production-oriented but still evolving. Current work is focused on release hardening, match UX on Wear, and improving confidence around sync/timer behavior before a broader public release.
