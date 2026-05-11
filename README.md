# PLAYCE - Tennis & Padel Score Tracker

> **play** + **ace** - A native Android app for tracking live tennis and padel scores, built for the court.

PLAYCE lets you keep score directly from your wrist during a match. The Wear OS app runs the live scoring engine while the Android phone displays the score in real time and stores your match history. A parallel Garmin Connect IQ companion lets you score from a paired Garmin watch with the exact same flow.

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
- **Match setup from phone** — configure player names and match format, then sync to watch via DataClient (Wear OS) and Garmin Connect IQ in parallel
- **Garmin Connect IQ bridge** — same score-tracking experience from a Garmin watch (`venu3`, `venu3s`, `epix2`, `fenix7`) routed through Garmin Connect Mobile; coexists with Wear OS without rewriting any existing flow
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
| Wear OS sync | Wearable Data Layer API (DataClient + MessageClient) |
| Garmin sync | Connect IQ Mobile SDK (companion `.aar`, transports through Garmin Connect Mobile) — sibling repo [`playce_garmin`](https://github.com/atomaculus/playce_garmin) supplies the watch-side app |
| Billing | Google Play Billing Library v7 |
| Review | Google Play In-App Review API |
| Crash reporting | Firebase Crashlytics |
| Build | Gradle KTS, R8/ProGuard minification, release keystore pipeline |

---

## Architecture

```text
app/          -> Wear OS module (scorer, timer, watch-side sync, spectator mode)
mobile/       -> Phone module (counter, history, stats, detail, share card, widget,
                 Wear OS listeners, Garmin Connect IQ companion bridge)
shared/       -> Pure Kotlin scoring engine (ScoringEngine, MatchFormat) + unit tests
```

Sibling repository (Garmin watch app):

```text
playce_garmin/  -> Connect IQ (Monkey C) port that replaces :app for Garmin devices
                   and uses the same /playce/* sync paths.
```

The watch is the primary input device during the match — Wear OS or Garmin, indistinctly. The phone acts as the companion display, configuration hub, and local archive for both ecosystems at once.

### Garmin Connect IQ bridge (`:mobile.garmin`)

When the user has *Garmin Connect Mobile* installed and a Garmin watch paired, the phone speaks to the watch via the Connect IQ Mobile SDK. The bridge mirrors the Wear OS contract semantically:

| Path | Direction | Wear transport | Garmin transport |
|---|---|---|---|
| `/playce/match-config` | phone → watch | DataClient | Connect IQ envelope |
| `/playce/live` | watch → phone | DataClient | Connect IQ envelope |
| `/match_finished` | watch → phone | MessageClient | Connect IQ envelope |
| `/match_finished_ack` | phone → watch | MessageClient | Connect IQ envelope |

The Garmin envelope is a `Map<String, Any?>` with `path`, `kind`, `payload`, optional `idempotencyKey`, and `timestamp`. Payload field names match Wear OS exactly, so `LiveScoreRepository` and `MatchRepository` stay unchanged.

The Connect IQ Mobile SDK is **not** in Maven Central. Drop the official `.aar` in `mobile/libs/` (see `mobile/libs/README.md`) before building.

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

Firebase Crashlytics and Analytics are part of the intended release track. Add the correct `google-services.json` files before building the final release artifacts and keep Play Console Data safety aligned with the shipped build.

---

## Status

Production-ready with full scoring engine, phone-to-watch config sync, stats, export, widget, onboarding, and localized UI. Remaining steps before Play Store submission: Firebase configuration (`google-services.json`), signed release build, screenshots, public legal URLs, and real-device QA validation.
