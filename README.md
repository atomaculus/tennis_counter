# PLAYCE - Tennis & Padel Score Tracker

> **play** + **ace** - A native Android app for tracking live tennis and padel scores, built for the court.

PLAYCE lets you keep score directly from your wrist during a match. The Wear OS app runs the live scoring engine while the Android phone displays the score in real time and stores your match history.

---

## Why I built this

Every time I played tennis, keeping score meant stopping the game, arguing about sets, or relying on memory. Existing apps were either too complex or required your phone on court. I wanted something simple: tap your watch, score updates, done.

---

## Features

### Core
- **Wear OS scorer** — tap-based point tracking directly on your smartwatch, no phone needed during play
- **Hardware button scoring** — optional STEM_1/STEM_2 buttons on the watch to add points without touching the screen
- **Serve guidance on wrist** — current server highlight plus dynamic left/right serve-side halo during each game
- **Live sync** — phone receives live score updates from the watch via Wearable Data Layer with ACK confirmation
- **Match history** — all finished matches stored locally on your phone with Room database
- **Share card** — generate and share a match summary card after each game
- **Premium gate** — freemium model with local persistence; free mode keeps a manual counter on phone

### Scoring engine (shared module)
- **Full tennis scoring** — points, games, sets with proper deuce/advantage logic
- **Tiebreak support** — standard tiebreak at 6-6, super tiebreak in final set (configurable)
- **Configurable formats** — Best of 3, Best of 5 (Grand Slam), Fast4 (no-ad + super tiebreak)
- **Server rotation** — automatic tracking including tiebreak alternation every 2 points
- **Undo/replay** — full point history with undo support

### Phone companion
- **Match setup from phone** — configure player names and match format, then sync to watch via DataClient
- **Stats dashboard** — matches played, total time, average/longest/shortest match duration
- **Export to CSV** — share your full match history as a CSV file
- **Onboarding** — 3-page intro shown on first launch
- **Light & dark theme** — follows system preference automatically
- **In-app review** — prompts for Google Play rating after 3 completed matches
- **Home screen widget** — shows last match result, tap to open the app
- **Localized UI** — English and Spanish (strings.xml + values-es)

---

## Tech stack

| Layer | Stack |
|---|---|
| Wear OS app | Kotlin, Jetpack Compose for Wear |
| Android phone | Kotlin, Jetpack Compose, Material 3, Room, ViewModel |
| Shared module | Pure Kotlin scoring engine with 22 unit tests |
| Sync | Wearable Data Layer API (DataClient + MessageClient) |
| Billing | Google Play Billing Library v7 |
| Review | Google Play In-App Review API |
| Crash reporting | Firebase Crashlytics (optional, flag-controlled) |
| Build | Gradle KTS, R8/ProGuard minification, release keystore pipeline |

---

## Architecture

```text
app/          -> Wear OS module (scorer, timer, watch-side sync, spectator mode)
mobile/       -> Phone module (counter, history, stats, detail, share card, widget)
shared/       -> Pure Kotlin scoring engine (ScoringEngine, MatchFormat) + unit tests
```

The watch is the primary input device during the match. The phone acts as the companion display, configuration hub, and local archive.

---

## Main flows

1. **Setup** — configure player names and match format on the phone, send to watch
2. **Play** — start the match on the watch; track points, games, sets, and tiebreaks in real time
3. **Sync** — live score streams to phone (and optional spectator watch) via DataClient
4. **Save** — completed matches persist to local Room database on phone
5. **Review** — browse stats dashboard, export CSV, share a polished result card

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

Crashlytics is optional and currently disabled by default in `gradle.properties`. If a release enables it, place `google-services.json` only in the modules that will report crashes and keep Play Console Data safety aligned with that build.

---

## Status

Production-ready with full scoring engine, phone-to-watch config sync, stats, export, widget, onboarding, and localized UI. Remaining steps before Play Store submission: Firebase setup (`google-services.json`), signed release build, screenshots, and real-device QA validation.
