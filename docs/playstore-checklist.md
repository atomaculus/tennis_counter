# PLAYCE - Play Store release checklist

Last updated: March 30, 2026

This file is the release gate for the current codebase. Use it before publishing any new version to Google Play.

## 1. Release configuration

- [x] `PLAYCE_VERSION_CODE` is defined in `gradle.properties`.
- [x] `PLAYCE_VERSION_NAME` is defined in `gradle.properties`.
- [x] `applicationId` is identical in `:app` and `:mobile` (`com.playce.tenniscounter.app`).
- [ ] Increase `PLAYCE_VERSION_CODE` before every new Play upload.
- [ ] Confirm `PLAYCE_VERSION_NAME` matches the release notes.
- [ ] Copy `keystore.properties.template` to `keystore.properties` and load the real signing values.
- [ ] Verify the signing keystore pointed by `storeFile` exists outside the repo and is backed up.

## 2. Automated release checks

Run these commands from repository root:

```powershell
.\gradlew.bat :shared:test
.\gradlew.bat :app:lintDebug :mobile:lintDebug
.\gradlew.bat :mobile:testDebugUnitTest
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :mobile:assembleDebug
.\gradlew.bat :mobile:bundleRelease :app:assembleRelease
```

Expected result:
- no failing lint tasks
- no failing unit tests (including 22 shared scoring engine tests)
- release artifacts generated successfully

Current repo baseline validated locally on March 30, 2026:
- `:shared:test` passed (22 tests)
- `:app:assembleDebug` passed
- `:mobile:assembleDebug` passed

Pending before a real store submission:
- [ ] Re-run the full release command set on the final candidate commit.
- [ ] Build `:mobile:bundleRelease` with the production keystore in place.
- [ ] Archive generated artifacts and exact `versionCode` / `versionName` used for submission.

## 3. Crash reporting and diagnostics

Current default repository state:
- Firebase plugins are applied in both app modules.
- Firebase Crashlytics and Firebase Analytics dependencies are present in both app modules.
- `google-services.json` is still required before building the final Firebase-enabled release candidate.

Release decision for the current launch track:
- Firebase Crashlytics and Firebase Analytics are intended to ship.

Required before submission:
- [ ] Add the correct `google-services.json` files for the release build.
- [ ] Force a test crash on phone and verify it appears in Firebase Crashlytics.
- [ ] Confirm Firebase Analytics events are configured only as intended for the shipped build.
- [ ] Update Play Console Data safety to reflect diagnostic and analytics collection in the actual release build.

## 4. Manual QA on real devices

Minimum required setup:
- one real Android phone
- one real Wear OS watch used as scorer
- optional second Wear OS watch for spectator mode

Companion document:
- follow [docs/manual-qa-guide.md](/C:/Users/atoma/OneDrive/Desktop/Proyectos/Tenis%20counter/docs/manual-qa-guide.md) for the full step-by-step runbook and bug report template

### 4.1 Wear scoring core

- [ ] Start a fresh match and confirm the timer auto-starts.
- [ ] Validate point progression: `0 -> 15 -> 30 -> 40 -> game`.
- [ ] Validate deuce and advantage both directions.
- [ ] Validate tiebreak triggers at 6-6 with correct point-by-point scoring (first to 7, win by 2).
- [ ] Validate super tiebreak in final set (if configured): first to 10, win by 2.
- [ ] Validate game rollover updates games and keeps set logic correct.
- [ ] Validate set rollover and confirm the serve sequence continues into the next set.
- [ ] Validate long press undo on both players.
- [ ] Validate admin actions: `reset game`, `reset match`, `new match`.
- [ ] Validate current server button highlight.
- [ ] Validate left/right serve-side halo changes with even/odd point count.

### 4.1b Hardware button scoring (watch)

- [ ] Enable hardware buttons via the toggle chip on the watch.
- [ ] Validate STEM_1 (top button) adds point to Player A.
- [ ] Validate STEM_2 (bottom button) adds point to Player B.
- [ ] Validate buttons are ignored when screen is off (ambient mode).
- [ ] Validate buttons are ignored when match is finished.
- [ ] Validate HOME button still exits the app normally.
- [ ] Disable hardware buttons and confirm taps are the only input.

### 4.1c Phone → Watch config sync

- [ ] Set player names on phone and tap "Send to Watch".
- [ ] Confirm watch receives names and displays them on the scoring buttons.
- [ ] Set match format (Standard/Grand Slam/Fast4) and send to watch.
- [ ] Test with long player names (15+ chars) — verify watch UI truncates gracefully.
- [ ] Test sending config while watch app is closed — confirm config loads on next open.

### 4.2 Wear timer persistence

- [ ] Leave the match running, background the app, then return and confirm elapsed time is preserved.
- [ ] Remove the app from recent tasks and confirm timer consolidation works after reopening.
- [ ] Reboot the watch during an unfinished match and confirm timer state is reasonable and not silently reset.

### 4.3 Wear -> mobile sync

- [ ] Finish a match on the watch and save with phone connected.
- [ ] Confirm phone receives the match once and Wear shows synced state.
- [ ] Save with phone disconnected, then reconnect and confirm retry succeeds.
- [ ] Repeat the same save flow until an ACK duplicate path is observed and confirm no duplicate record is stored.
- [ ] Confirm Premium-locked flow returns `premium_locked` and does not insert into history.

### 4.4 Mobile UX

- [ ] Confirm live score replaces the local mobile counter while a watch match is active.
- [ ] Confirm history loads with correct final score and set breakdown.
- [ ] Confirm match detail renders correctly.
- [ ] Confirm share card renders and can be shared to another app.
- [ ] Confirm the mobile free experience still works without Premium.
- [ ] Confirm premium unlock purchase and restore flows on a test account.
- [ ] Confirm onboarding shows on first launch and does not show again.
- [ ] Confirm Stats tab displays correct data after playing matches.
- [ ] Confirm CSV export generates a valid file and share intent works.
- [ ] Confirm home screen widget shows last match result and opens the app on tap.
- [ ] Confirm light theme renders correctly when system is in light mode.
- [ ] Confirm in-app review prompt appears after 3 completed matches.
- [ ] Confirm Match Setup card sends config to watch successfully.
- [ ] Confirm all UI strings display in Spanish when device language is Spanish.

### 4.5 Spectator mode

- [ ] With a second watch, confirm spectator mode detects an active scorer watch.
- [ ] Confirm spectator score updates without interaction.
- [ ] Confirm spectator mode exits cleanly when the active match ends.

## 5. Play Console readiness

- [ ] Publish a final privacy policy URL accessible without login.
- [ ] Publish a final Terms of Service URL accessible without login.
- [ ] Review `docs/privacy-policy.md` and `docs/politica-privacidad-playce-es.md` against the exact release build.
- [ ] Fill Play Console Data safety using `docs/play-data-safety.md`.
- [ ] Prepare short description and full description in Spanish and English.
- [ ] Produce final screenshots for phone and watch from the release candidate build.
- [ ] Export icon and feature graphic assets in the required Play formats.
- [ ] Confirm support email matches the one shown in policy and store listing.
- [ ] Confirm in-app product `premium_unlock` is active and correctly linked in Play Console.

## 6. Final go / no-go gate

Do not submit if any of these are still unresolved:
- release build fails
- sync retry or ACK flow is flaky on real devices
- privacy policy URL is not public
- Terms URL is not public
- Data safety form does not match the shipped build
- versioning is not updated for the new upload
